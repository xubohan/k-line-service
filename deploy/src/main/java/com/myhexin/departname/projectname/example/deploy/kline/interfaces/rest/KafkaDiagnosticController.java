package com.example.kline.interfaces.rest;

import com.example.kline.common.external.message.KafkaAdminService;
import com.example.kline.common.external.message.TestSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka 诊断控制器，用于检查 Kafka 连接状态和创建 Topic
 */
@RestController
@RequestMapping("/kafka/admin")
public class KafkaDiagnosticController {

    private final KafkaAdminService kafkaAdminService;
    private final TestSender testSender;

    @Autowired
    public KafkaDiagnosticController(KafkaAdminService kafkaAdminService, TestSender testSender) {
        this.kafkaAdminService = kafkaAdminService;
        this.testSender = testSender;
    }

    /**
     * 检查 Kafka 连接状态和配置信息
     */
    @GetMapping("/status")
    public Map<String, Object> getKafkaStatus() {
        Map<String, Object> resp = new LinkedHashMap<>();
        
        try {
            boolean available = kafkaAdminService.isKafkaAvailable();
            boolean timelineExists = kafkaAdminService.topicExists("timeline");
            
            resp.put("code", "0");
            resp.put("message", "Kafka status check completed");
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("kafkaAvailable", available);
            data.put("timelineTopicExists", timelineExists);
            data.put("availableTopics", kafkaAdminService.listTopics());
            data.put("detailedInfo", kafkaAdminService.getKafkaInfo());
            
            resp.put("data", data);
            
            if (!available) {
                resp.put("code", "-1");
                resp.put("message", "Kafka is not available");
            } else if (!timelineExists) {
                resp.put("code", "-2");
                resp.put("message", "Kafka is available but timeline topic does not exist");
            }
            
        } catch (Exception e) {
            resp.put("code", "-1");
            resp.put("message", "Failed to check Kafka status: " + e.getMessage());
        }
        
        return resp;
    }

    /**
     * 创建 timeline Topic
     */
    @PostMapping("/create-timeline-topic")
    public Map<String, Object> createTimelineTopic() {
        Map<String, Object> resp = new LinkedHashMap<>();
        
        try {
            boolean success = kafkaAdminService.createTimelineTopic();
            
            if (success) {
                resp.put("code", "0");
                resp.put("message", "Timeline topic created successfully or already exists");
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("topicName", "timeline");
                data.put("partitions", 1);
                data.put("replicationFactor", 1);
                data.put("created", true);
                resp.put("data", data);
            } else {
                resp.put("code", "-1");
                resp.put("message", "Failed to create timeline topic");
            }
            
        } catch (Exception e) {
            resp.put("code", "-1");
            resp.put("message", "Exception while creating timeline topic: " + e.getMessage());
        }
        
        return resp;
    }

    /**
     * 发送测试消息并验证消费
     */
    @PostMapping("/test-message-flow")
    public Map<String, Object> testMessageFlow() {
        Map<String, Object> resp = new LinkedHashMap<>();
        
        try {
            // 1. 检查 Kafka 可用性
            if (!kafkaAdminService.isKafkaAvailable()) {
                resp.put("code", "-1");
                resp.put("message", "Kafka is not available");
                return resp;
            }
            
            // 2. 确保 timeline topic 存在
            if (!kafkaAdminService.topicExists("timeline")) {
                kafkaAdminService.createTimelineTopic();
            }
            
            // 3. 发送测试消息
            String testMessage = "{\"topic\":\"timeline\", \"stock_minute_data\":{\"stockCode\":\"300033\",\"marketId\":\"33\",\"price\":88.94,\"date\":\"20200101\",\"time\":\"0940\"}}";
            testSender.sendSync("timeline", testMessage);
            
            resp.put("code", "0");
            resp.put("message", "Test message sent successfully");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("topic", "timeline");
            data.put("testMessage", testMessage);
            data.put("note", "Check application logs for consumer activity");
            resp.put("data", data);
            
        } catch (Exception e) {
            resp.put("code", "-1");
            resp.put("message", "Failed to test message flow: " + e.getMessage());
        }
        
        return resp;
    }
}