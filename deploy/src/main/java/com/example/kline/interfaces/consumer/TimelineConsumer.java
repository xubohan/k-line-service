package com.example.kline.interfaces.consumer;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for timeline data.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@Component
@RequiredArgsConstructor
public class TimelineConsumer {
    private final KlineRepository klineRepository;

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
