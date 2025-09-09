package com.example.kline.modules.kline.infrastructure.cache;

import java.util.Objects;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Write timeline points into Redis ZSET for kline cache.
 * Key: kline:1m:{marketId}:{stockCode}
 * Score: minute timestamp (tsSec/60)
 * Member: price string (toPlainString)
 */
@Component
public class TimelineRedisWriter {
    private static final Logger log = LoggerFactory.getLogger(TimelineRedisWriter.class);

    private final boolean externalEnabled;
    private final String redisHost;
    private final int redisPort;
    private volatile JedisPool pool;

    public TimelineRedisWriter() {
        this.externalEnabled = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        this.redisHost = getProp("spring.redis.host", "127.0.0.1");
        this.redisPort = Integer.parseInt(getProp("spring.redis.port", "6379"));
    }

    @Autowired
    public TimelineRedisWriter(Environment env) {
        boolean fromSpring = env.getProperty("app.redis.external", Boolean.class, false);
        boolean fromSys = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        this.externalEnabled = fromSpring || fromSys;
        this.redisHost = coalesce(env.getProperty("spring.redis.host"), getProp("spring.redis.host", "127.0.0.1"));
        this.redisPort = Integer.parseInt(coalesce(env.getProperty("spring.redis.port"), getProp("spring.redis.port", "6379")));
    }

    public void write(String stockCode, String marketId, long tsSec, BigDecimal price) {
        if (!externalEnabled) return;
        if (isBlank(stockCode) || isBlank(marketId) || price == null) return;
        String key = zkey(marketId, stockCode);
        double score = (double) (tsSec / 60L);
        try (Jedis j = jedis().getResource()) {
            j.zadd(key, score, price.toPlainString());
        } catch (Exception e) {
            log.warn("Failed to write timeline to Redis ZSET: {}", e.getMessage());
        }
    }

    private JedisPool jedis() {
        JedisPool p = pool;
        if (p == null) {
            synchronized (this) {
                if (pool == null) {
                    pool = new JedisPool(new JedisPoolConfig(), redisHost, redisPort);
                }
                p = pool;
            }
        }
        return p;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String zkey(String marketId, String stockCode) { return "kline:1m:" + marketId + ":" + stockCode; }
    private static String getProp(String key, String def) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(key.replace('.', '_').toUpperCase());
        return v != null ? v : def;
    }
    private static String coalesce(String a, String b) { return a != null ? a : b; }
}

