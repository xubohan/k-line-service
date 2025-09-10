package com.example.kline.interfaces.rest;

import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.domain.entity.KlineResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Redis 数据验证控制器
 * 用于检查 Redis 中的数据写入情况
 */
@RestController
@RequestMapping("/redis")
public class RedisCheckController {

    private final RedisKlineCache redisKlineCache;

    @Autowired
    public RedisCheckController(RedisKlineCache redisKlineCache) {
        this.redisKlineCache = redisKlineCache;
    }

    /**
     * 检查 Redis 中是否有指定股票的数据
     */
    @GetMapping("/check")
    public Map<String, Object> checkRedisData(
            @RequestParam(defaultValue = "300033") String stockcode,
            @RequestParam(defaultValue = "33") String marketId) {
        
        try {
            KlineResponse response = redisKlineCache.getRange(stockcode, marketId, null, null, 20);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", "0");
            result.put("message", "Redis data check completed");
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("stockcode", stockcode);
            data.put("marketId", marketId);
            data.put("dataCount", response.getData().size());
            data.put("hasData", !response.getData().isEmpty());
            
            if (!response.getData().isEmpty()) {
                data.put("firstRecord", response.getData().get(0));
                data.put("lastRecord", response.getData().get(response.getData().size() - 1));
            }
            
            result.put("data", data);
            return result;
            
        } catch (Exception e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("code", "-1");
            result.put("message", "Failed to check Redis data: " + e.getMessage());
            return result;
        }
    }
}