package com.example.kline.modules.kline.infrastructure.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Simulated Redis cache for stock names.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@Component
public class RedisNameCache {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public String getName(String stockcode, String marketId) {
        return store.get(key(stockcode, marketId));
    }

    public void setName(String stockcode, String marketId, String name, long ttlSec) {
        store.put(key(stockcode, marketId), name);
    }

    private String key(String stockcode, String marketId) {
        return stockcode + ":" + marketId;
    }
}
