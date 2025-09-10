package com.example.kline.modules.kline.infrastructure.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 股票名称缓存
 * 
 * 专门用于缓存从外部名称服务获取的股票名称信息
 * 使用Redis数据库1，与K线数据缓存(数据库0)分离
 * 
 * 支持两种模式：
 * - 内存模式 (默认, 无外部依赖)
 * - Redis外部模式 (当app.redis.external=true时启用)
 * 
 * @author xubohan@myhexin.com
 * @date 2025-09-10 13:30:00
 */
@Component
public class RedisNameCache {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    private static final int NAME_CACHE_DB = 1;  // 使用Redis数据库1存储名称缓存
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private final StringRedisTemplate redisTemplate;
    private final boolean externalEnabled;

    @Autowired
    public RedisNameCache(RedisConnectionFactory connectionFactory) {
        this.externalEnabled = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        
        // 创建专用于名称缓存的 Redis Template，使用数据库1
        if (externalEnabled) {
            this.redisTemplate = createNameCacheRedisTemplate(connectionFactory);
        } else {
            this.redisTemplate = null;
        }
    }

    public String getName(String stockcode, String marketId) {
        // Per MVP principle: return null for invalid input to prevent service interruption
        if (stockcode == null || marketId == null) {
            return null; // return null for invalid input
        }
        
        String k = key(stockcode, marketId);
        if (externalEnabled && redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(k);
                if (val == null) return null;
                String parsed = parseNameFromValue(val);
                if (parsed != null) return parsed;
                // fallback: store raw string
                return val;
            } catch (Exception ignore) {
                // fallback to in-memory if any issue
            }
        }
        return store.get(k);
    }

    public void setName(String stockcode, String marketId, String name, long ttlSec) {
        // Per MVP principle: discard null values to prevent service interruption
        if (stockcode == null || marketId == null) {
            return; // no-op, discard invalid input
        }
        
        String k = key(stockcode, marketId);
        if (externalEnabled && redisTemplate != null) {
            try {
                // Handle null name for JSON formatting
                String nm = name != null ? name : "";
                // store as JSON with the requested format
                String json = String.format("{\"stockCode\":\"%s\", \"marketId\": \"%s\", \"stockname\":\"%s\"}",
                    stockcode, marketId, nm);
                if (ttlSec > 0) {
                    redisTemplate.opsForValue().set(k, json, ttlSec, TimeUnit.SECONDS);
                } else {
                    redisTemplate.opsForValue().set(k, json);
                }
                return;
            } catch (Exception ignore) {
                // fall through to in-memory
            }
        }
        // Handle null name for in-memory storage
        store.put(k, name != null ? name : "");
    }

    /**
     * 创建专用于名称缓存的 RedisTemplate，使用数据库1
     */
    private StringRedisTemplate createNameCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        
        // 配置默认使用数据库1 - 使用 RedisCallback 明确指定类型
        template.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            connection.select(NAME_CACHE_DB);
            return null;
        });
        
        template.afterPropertiesSet();
        return template;
    }

    private String parseNameFromValue(String val) {
        try {
            if (val == null) return null;
            String s = val.trim();
            if (!s.startsWith("{")) return null;
            JsonNode node = MAPPER.readTree(s);
            JsonNode n1 = node.get("stockname");
            if (n1 == null) n1 = node.get("stockName");
            if (n1 == null) n1 = node.get("name");
            return n1 != null && !n1.isNull() ? n1.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String key(String stockcode, String marketId) {
        return stockcode + ":" + marketId;
    }

    private static String getProp(String key, String def) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(key.replace('.', '_').toUpperCase());
        return v != null ? v : def;
    }
}
