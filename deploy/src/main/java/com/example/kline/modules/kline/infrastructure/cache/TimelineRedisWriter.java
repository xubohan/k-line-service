package com.example.kline.modules.kline.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 将timeline数据点写入Redis ZSET用于K线缓存
 * Key: kline:1m:{marketId}:{stockCode}
 * Score: 分钟时间戳 (tsSec/60)
 * Member: 价格字符串 (toPlainString)
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-10 13:30:00
 */
@Component
public class TimelineRedisWriter {
    private static final Logger log = LoggerFactory.getLogger(TimelineRedisWriter.class);

    private final boolean externalEnabled;
    private final StringRedisTemplate redisTemplate;

    // 无参构造函数，用于测试
    public TimelineRedisWriter() {
        this.externalEnabled = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        this.redisTemplate = null;
    }

    @Autowired
    public TimelineRedisWriter(Environment env, StringRedisTemplate redisTemplate) {
        boolean fromSpring = env.getProperty("app.redis.external", Boolean.class, false);
        boolean fromSys = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        this.externalEnabled = fromSpring || fromSys;
        this.redisTemplate = externalEnabled ? redisTemplate : null;
    }

    public void write(String stockCode, String marketId, long tsSec, BigDecimal price) {
        if (!externalEnabled || redisTemplate == null) return;
        if (isBlank(stockCode) || isBlank(marketId) || price == null) return;
        
        String key = zkey(marketId, stockCode);
        double score = (double) (tsSec / 60L);
        
        try {
            redisTemplate.opsForZSet().add(key, price.toPlainString(), score);
        } catch (Exception e) {
            log.warn("Failed to write timeline to Redis ZSET: {}", e.getMessage());
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    
    private static String zkey(String marketId, String stockCode) { 
        return "kline:1m:" + marketId + ":" + stockCode; 
    }
    
    private static String getProp(String key, String def) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(key.replace('.', '_').toUpperCase());
        return v != null ? v : def;
    }
}

