package com.example.kline.interfaces.consumer;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import com.example.kline.modules.kline.infrastructure.cache.TimelineRedisWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for timeline data with strict schema validation.
 * Expected message JSON schema: {"stockCode","marketId","price","date","time"}.
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class TimelineConsumer {
    private static final Logger log = LoggerFactory.getLogger(TimelineConsumer.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KlineRepository klineRepository;
    private final TimelineRedisWriter timelineRedisWriter;

    @Autowired
    public TimelineConsumer(KlineRepository klineRepository, TimelineRedisWriter timelineRedisWriter) {
        this.klineRepository = klineRepository;
        this.timelineRedisWriter = timelineRedisWriter;
        log.info("TimelineConsumer initialized with manual ACK enabled");
    }

    // Backward-compatible convenience constructor for tests without Spring context
    public TimelineConsumer(KlineRepository klineRepository) {
        this(klineRepository, new TimelineRedisWriter());
    }

    /**
     * 单条消费的代码，注意 listener.type需要设置为single，形参使用ConsumerRecord
     * 按照 listener.txt 最佳实践实现手动ACK
     * Expected format: {"topic":"timeline", "stock_minute_data":{"stockCode":"300000","marketId":"33","price":"86.96","date":"20200101","time":"0000"}}
     * Invalid messages are discarded per strict API contract.
     */
    @KafkaListener(
            id = "timeline-consumer",
            topics = "timeline",
            groupId = "kline-service",
            concurrency = "1",
            autoStartup = "true",
            containerFactory = "manualAckKafkaListenerContainerFactory"
    )
    public void run(ConsumerRecord<String, String> record, Acknowledgment ack) throws Exception {
        String payload = record.value();
        
        if (payload == null || payload.trim().isEmpty()) {
            log.warn("⚠️ Empty or null payload, acknowledging and skipping");
            ack.acknowledge();
            return;
        }
        
        try {
            log.info("📥 Receive Message. Message info: {}", payload);
            
            // Parse the outer wrapper message
            KafkaMessage kafkaMsg = objectMapper.readValue(payload, KafkaMessage.class);
            if (kafkaMsg == null || kafkaMsg.stock_minute_data == null) {
                log.warn("⚠️ Discarding message without stock_minute_data: {}", payload);
                ack.acknowledge(); // ACK even for invalid messages to avoid reprocessing
                return;
            }
            
            TimelineMessage msg = kafkaMsg.stock_minute_data;
            if (!isValid(msg)) {
                log.warn("⚠️ Discarding invalid timeline message: {}", payload);
                ack.acknowledge(); // ACK invalid messages
                return;
            }

            log.info("✅ Valid message parsed - StockCode:{}, MarketId:{}, Price:{}, Date:{}, Time:{}", 
                    msg.stockCode, msg.marketId, msg.price, msg.date, msg.time);

            long ts = toEpochSeconds(msg.date, msg.time);
            PricePoint p = new PricePoint();
            p.setTs(ts);
            p.setOpen(msg.price);
            p.setHigh(msg.price);
            p.setLow(msg.price);
            p.setClose(msg.price);
            p.setVol(0L);

            KlineResponse resp = new KlineResponse();
            resp.setStockcode(msg.stockCode);
            resp.setMarketId(msg.marketId);
            resp.addPricePoint(p);

            // 消费逻辑：Per L2 flow: Write directly to Redis cache (no database)
            log.info("💾 Writing to KlineRepository and Redis...");
            klineRepository.upsertBatch(resp);
            // Also write to Redis ZSET for L2 cache
            timelineRedisWriter.write(msg.stockCode, msg.marketId, ts, msg.price);
            
            // ACK 确认消息处理成功
            ack.acknowledge();
            log.info("✅ Consume success. Topic:{}, Partition:{}, Offset:{}, StockCode:{}, Price:{}",
                   record.topic(), record.partition(), record.offset(), msg.stockCode, msg.price);
        } catch (Exception e) {
            log.error("❌ Failed to process timeline message: {}", payload, e);
            // 异常处理逻辑：可以选择ACK（丢弃错误消息）或不ACK（重试）
            // 这里选择ACK避免无限重试
            ack.acknowledge();
        }
    }

    /**
     * Backward-compatible entry used by unit tests; not a Kafka listener.
     */
    public void run(KlineResponse response) {
        if (response == null) return;
        String sc = response.getStockcode();
        String mk = response.getMarketId();
        if (isBlank(sc) || isBlank(mk)) return;
        klineRepository.upsertBatch(response);
    }

    private boolean isValid(TimelineMessage m) {
        if (m == null) return false;
        if (isBlank(m.stockCode) || isBlank(m.marketId)) return false;
        if (m.price == null) return false;
        if (isBlank(m.date) || isBlank(m.time)) return false;
        if (!m.date.matches("\\d{8}") || !m.time.matches("\\d{4}")) return false;
        return true;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static long toEpochSeconds(String date, String time) {
        LocalDate d = LocalDate.parse(date, DATE_FMT);
        LocalTime t = LocalTime.parse(time, TIME_FMT);
        return d.atTime(t).toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    /** POJO matching the strict timeline message schema. */
    public static class TimelineMessage {
        public String stockCode;
        public String marketId;
        public BigDecimal price;
        public String date; // yyyyMMdd
        public String time; // HHmm

        public TimelineMessage() {}
    }

    /** POJO matching the Kafka message wrapper format. */
    public static class KafkaMessage {
        public String topic;
        public TimelineMessage stock_minute_data;

        public KafkaMessage() {}
    }
}