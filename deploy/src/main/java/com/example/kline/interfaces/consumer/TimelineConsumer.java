package com.example.kline.interfaces.consumer;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Kafka consumer for timeline data.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class TimelineConsumer {
    private final KlineRepository klineRepository;

    @Autowired
    public TimelineConsumer(KlineRepository klineRepository) {
        this.klineRepository = klineRepository;
    }

    /**
     * Consume message (stub implementation).
     *
     * @param response k-line response
     */
    @KafkaListener(topics = "timeline", groupId = "kline-service")
    public void run(KlineResponse response) {
        klineRepository.upsertBatch(response);
    }
}
