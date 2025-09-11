package com.example.kline.common.external.message;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Kafka 管理服务，用于创建 Topic 和验证连接
 */
@Slf4j
@Service
public class KafkaAdminService {

    private final KafkaAdmin kafkaAdmin;

    @Autowired
    public KafkaAdminService(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    /**
     * 检查 Kafka 连接状态
     */
    public boolean isKafkaAvailable() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult result = adminClient.listTopics();
            result.names().get(); // 等待结果，如果连接失败会抛出异常
            log.info("Kafka connection is available");
            return true;
        } catch (Exception e) {
            log.error("Kafka connection failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 列出所有 Topic
     */
    public Set<String> listTopics() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult result = adminClient.listTopics();
            Set<String> topics = result.names().get();
            log.info("Available topics: {}", topics);
            return topics;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to list topics: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 检查 Topic 是否存在
     */
    public boolean topicExists(String topicName) {
        Set<String> topics = listTopics();
        boolean exists = topics.contains(topicName);
        log.info("Topic '{}' exists: {}", topicName, exists);
        return exists;
    }

    /**
     * 创建 Topic
     */
    public boolean createTopic(String topicName, int partitions, short replicationFactor) {
        if (topicExists(topicName)) {
            log.info("Topic '{}' already exists", topicName);
            return true;
        }

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
            result.all().get(); // 等待创建完成
            log.info("Topic '{}' created successfully with {} partitions and replication factor {}", 
                    topicName, partitions, replicationFactor);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to create topic '{}': {}", topicName, e.getMessage());
            return false;
        }
    }

    /**
     * 创建默认的 timeline Topic
     */
    public boolean createTimelineTopic() {
        return createTopic("timeline", 1, (short) 1);
    }

    /**
     * 获取 Kafka 连接信息
     */
    public String getKafkaInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Kafka Configuration:\n");
        info.append("- Bootstrap Servers: ").append(kafkaAdmin.getConfigurationProperties().get("bootstrap.servers")).append("\n");
        info.append("- Connection Available: ").append(isKafkaAvailable()).append("\n");
        info.append("- Available Topics: ").append(listTopics()).append("\n");
        info.append("- Timeline Topic Exists: ").append(topicExists("timeline")).append("\n");
        return info.toString();
    }
}