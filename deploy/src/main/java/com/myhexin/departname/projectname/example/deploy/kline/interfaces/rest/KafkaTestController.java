package com.example.kline.interfaces.rest;

import com.example.kline.common.external.message.TestSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka测试控制器，用于演示消息发送功能
 * 
 * @author xubohan@myhexin.com
 * @date 2025-09-09 20:24:08
 */
@RestController
@RequestMapping("/kafka")
public class KafkaTestController {

    private final TestSender testSender;

    @Autowired
    public KafkaTestController(TestSender testSender) {
        this.testSender = testSender;
    }

    /**
     * 异步发送消息到 Kafka
     */
    @PostMapping("/send/async")
    public Map<String, Object> sendAsync(@RequestBody Map<String, Object> request) {
        String topic = (String) request.get("topic");
        String message = (String) request.get("message");
        
        if (topic == null || message == null) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "-1");
            resp.put("message", "topic and message are required");
            return resp;
        }
        
        testSender.sendAsync(topic, message);
        
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", "0");
        resp.put("message", "Message sent asynchronously");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("topic", topic);
        data.put("messageLength", message.length());
        resp.put("data", data);
        return resp;
    }

    /**
     * 同步发送消息到 Kafka
     */
    @PostMapping("/send/sync")
    public Map<String, Object> sendSync(@RequestBody Map<String, Object> request) {
        String topic = (String) request.get("topic");
        String message = (String) request.get("message");
        
        if (topic == null || message == null) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "-1");
            resp.put("message", "topic and message are required");
            return resp;
        }
        
        try {
            testSender.sendSync(topic, message);
            
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "0");
            resp.put("message", "Message sent successfully");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("topic", topic);
            data.put("messageLength", message.length());
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "-1");
            resp.put("message", "Failed to send message: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 发送所有 kafka_data.json 中的数据，每秒发送一个对象
     * 总共3600个消息，异步执行
     */
    @PostMapping("/send/all-data")
    public Map<String, Object> sendAllKafkaData() {
        try {
            testSender.sendAllKafkaDataAsync();
            
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "0");
            resp.put("message", "All kafka data sending started asynchronously (3600 messages)");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("topic", "timeline");
            data.put("totalMessages", 3600);
            data.put("source", "kafka_data.json");
            data.put("mode", "async");
            data.put("note", "Messages are being sent in background, one per second. Check logs for progress");
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "-1");
            resp.put("message", "Failed to start sending kafka data: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 发送单条测试消息到 timeline topic
     */
    @PostMapping("/send/test")
    public Map<String, Object> sendTestMessage() {
        try {
            testSender.sendTestMessage();
            
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "0");
            resp.put("message", "Test message sent successfully");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("topic", "timeline");
            data.put("message", "Test timeline data");
            resp.put("data", data);
            return resp;
        } catch (Exception e) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("code", "-1");
            resp.put("message", "Failed to send test message: " + e.getMessage());
            return resp;
        }
    }
}