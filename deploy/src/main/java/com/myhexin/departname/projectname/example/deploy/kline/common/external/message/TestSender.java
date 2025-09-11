package com.example.kline.common.external.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka测试消息发送器
 * 实现按照 producer.txt 的最佳实践要求
 * 
 * @author xubohan@myhexin.com
 * @date 2025-09-09 20:24:08
 */
@Slf4j
@Component
public class TestSender {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestSender(KafkaTemplate<String, String> kafkaTemplate, ResourceLoader resourceLoader) {
        this.kafkaTemplate = kafkaTemplate;
        this.resourceLoader = resourceLoader;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 异步推送
     * 如果推送不需要确认推送结果并返回，使用非阻塞式推送
     */
    public void sendAsync(String topic, String message) {
        kafkaTemplate.send(topic, message)
                .addCallback(
                        (SendResult<String, String> sendResult) -> {
                            if (Objects.nonNull(sendResult)) {
                                RecordMetadata metadata = sendResult.getRecordMetadata();
                                log.info("Send over. {}, {}, {}", metadata.topic(), metadata.partition(), metadata.offset());
                            }
                        },
                        (Throwable e) -> log.warn("Send failed.", e)
                );
    }

    /**
     * 同步推送
     * 如果推送需要确认推送结果并返回，使用阻塞式推送
     */
    @SneakyThrows
    public void sendSync(String topic, String message) {
        ListenableFuture<SendResult<String,String >> future = kafkaTemplate.send(topic, message);
        SendResult<String,String > sendResult = future.get();
        RecordMetadata metadata = sendResult.getRecordMetadata();
        log.info("Send over. {}, {}, {}", metadata.topic(), metadata.partition(), metadata.offset());
    }

    /**
     * 异步发送所有 kafka_data.json 中的数据，每秒发送一个对象
     * 总共3600个JSON对象
     */
    @Async
    public void sendAllKafkaDataAsync() {
        try {
            log.info("Starting to send all kafka data (3600 messages) to timeline topic, one per second");
            sendAllKafkaData();
        } catch (Exception e) {
            log.error("Failed to send kafka data", e);
        }
    }

    /**
     * 发送所有 kafka_data.json 中的数据
     * 每秒发送一个对象到 timeline topic
     */
    @SneakyThrows
    public void sendAllKafkaData() {
        // 读取 kafka_data.json 文件
        List<Map<String, Object>> kafkaDataList;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("kafka_data.json")) {
            if (inputStream == null) {
                throw new IllegalArgumentException("kafka_data.json file not found in classpath");
            }
            kafkaDataList = objectMapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>(){});
        }
        
        log.info("Loaded {} messages from kafka_data.json", kafkaDataList.size());
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // 逐个发送消息，每秒一个
        for (int i = 0; i < kafkaDataList.size(); i++) {
            try {
                Map<String, Object> data = kafkaDataList.get(i);
                
                // 构造 timeline 消息格式
                Map<String, Object> timelineMessage = new LinkedHashMap<>();
                timelineMessage.put("topic", "timeline");
                timelineMessage.put("stock_minute_data", data);
                
                String messageJson = objectMapper.writeValueAsString(timelineMessage);
                
                // 同步发送确保按顺序
                sendSync("timeline", messageJson);
                successCount.incrementAndGet();
                
                // 每100条消息打印进度
                if ((i + 1) % 100 == 0) {
                    log.info("Progress: {}/{} messages sent", i + 1, kafkaDataList.size());
                }
                
                // 每秒发送一个消息
                Thread.sleep(1000);
                
            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("Failed to send message {}/{}: {}", i + 1, kafkaDataList.size(), e.getMessage());
            }
        }
        
        log.info("Finished sending kafka data: {} success, {} errors, {} total", 
                successCount.get(), errorCount.get(), kafkaDataList.size());
    }

    /**
     * 发送单条测试消息
     */
    public void sendTestMessage() {
        String testMessage = "{\"topic\":\"timeline\", \"stock_minute_data\":{\"stockCode\":\"300000\",\"marketId\":\"33\",\"price\":86.96,\"date\":\"20200101\",\"time\":\"0000\"}}";
        sendAsync("timeline", testMessage);
    }
}