package com.example.kline.modules.kline.infrastructure.cache;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Simulated Redis cache for k-line data.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@Component
public class RedisKlineCache {
    private final Map<String, List<PricePoint>> store = new ConcurrentHashMap<>();

    public KlineResponse getRange(String stockcode, String marketId, Long startTs, Long endTs, Integer limit) {
        List<PricePoint> list = store.get(key(stockcode, marketId));
        if (list == null) {
            return new KlineResponse();
        }
        List<PricePoint> range = list.stream()
            .filter(p -> (startTs == null || p.getTs() >= startTs)
                && (endTs == null || p.getTs() <= endTs))
            .sorted((a, b) -> Long.compare(a.getTs(), b.getTs()))
            .collect(Collectors.toList());
        if (limit != null && range.size() > limit) {
            range = range.subList(0, limit);
        }
        KlineResponse resp = new KlineResponse();
        resp.setStockcode(stockcode);
        resp.setMarketId(marketId);
        range.forEach(resp::addPricePoint);
        return resp;
    }

    public void putBatch(KlineResponse response, long ttlSec) {
        store.put(key(response.getStockcode(), response.getMarketId()), new ArrayList<>(response.getData()));
    }

    private String key(String stockcode, String marketId) {
        return stockcode + ":" + marketId;
    }
}
