package com.example.kline.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka监听器配置，支持手动ACK模式
 * 
 * @author xubohan@myhexin.com
 * @date 2025-09-11
 */
@Configuration
public class KafkaListenerConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory(KafkaProperties props) {
        // 用 Boot 的属性生成（确保 value 反序列化器是 String）
        Map<String, Object> cfg = new HashMap<>(props.buildConsumerProperties());
        cfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        cfg.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        // 如果你希望首次无提交位点时从最早开始
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 关闭自动提交（手动 ack 场景更匹配）
        cfg.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(cfg);
    }

    @Bean(name = "manualAckKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> manualAckKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // 手动立即 ack（提交位点）
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 可选：失败重试 + 回滚到当前记录（2.5.x 用 SeekToCurrentErrorHandler）
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new FixedBackOff(1000L, 3))); // 重试3次，每次间隔1s
        return factory;
    }
}