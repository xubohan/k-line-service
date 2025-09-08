package com.example.kline.modules.kline.infrastructure.db.dao;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * In-memory DAO for k-line points.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@Repository
public class KlineDao {
    private final Map<String, List<PricePoint>> store = new ConcurrentHashMap<>();

    public List<PricePoint> selectRange(String stockcode, String marketId, Long startTs, Long endTs, Integer limit) {
        List<PricePoint> list = store.getOrDefault(key(stockcode, marketId), Collections.emptyList());
        List<PricePoint> range = list.stream()
            .filter(p -> (startTs == null || p.getTs() >= startTs)
                && (endTs == null || p.getTs() <= endTs))
            .sorted((a, b) -> Long.compare(a.getTs(), b.getTs()))
            .collect(Collectors.toList());
        if (limit != null && range.size() > limit) {
            return new ArrayList<>(range.subList(0, limit));
        }
        return range;
    }

    public int insertBatch(String stockcode, String marketId, List<PricePoint> points) {
        store.computeIfAbsent(key(stockcode, marketId), k -> new ArrayList<>()).addAll(points);
        return points.size();
    }

    private String key(String stockcode, String marketId) {
        return stockcode + ":" + marketId;
    }
}
