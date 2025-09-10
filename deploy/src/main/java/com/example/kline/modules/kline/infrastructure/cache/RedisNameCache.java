package com.example.kline.modules.kline.infrastructure.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Name cache with two modes:
 * - In-memory map (default, no external deps)
 * - Real Redis via Jedis when enabled by system/env property
 *   app.redis.external=true (host: spring.redis.host, port: spring.redis.port)
 */
@Component
public class RedisNameCache {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final boolean externalEnabled;
    private final String redisHost;
    private final int redisPort;
    private volatile JedisPool pool; // lazy init

    public RedisNameCache() {
        this.externalEnabled = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        this.redisHost = getProp("spring.redis.host", "127.0.0.1");
        this.redisPort = Integer.parseInt(getProp("spring.redis.port", "6379"));
    }

    public String getName(String stockcode, String marketId) {
        // Per MVP principle: return null for invalid input to prevent service interruption
        if (stockcode == null || marketId == null) {
            return null; // return null for invalid input
        }
        
        String k = key(stockcode, marketId);
        if (externalEnabled) {
            try (Jedis j = jedis().getResource()) {
                String val = j.get(k);
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
        if (externalEnabled) {
            try (Jedis j = jedis().getResource()) {
                // Handle null name for JSON formatting
                String nm = name != null ? name : "";
                // store as JSON with the requested format
                String json = String.format("{\"stockCode\":\"%s\", \"marketId\": \"%s\", \"stockname\":\"%s\"}",
                    stockcode, marketId, nm);
                if (ttlSec > 0) {
                    j.setex(k, (int) Math.min(ttlSec, Integer.MAX_VALUE), json);
                } else {
                    j.set(k, json);
                }
                return;
            } catch (Exception ignore) {
                // fall through to in-memory
            }
        }
        // Handle null name for in-memory storage
        store.put(k, name != null ? name : "");
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
