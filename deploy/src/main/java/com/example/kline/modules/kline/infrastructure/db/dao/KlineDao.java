package com.example.kline.modules.kline.infrastructure.db.dao;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * In-memory DAO for k-line points.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@Repository
public class KlineDao {
    private final Map<String, List<PricePoint>> store = new ConcurrentHashMap<>();

    public List<PricePoint> selectRange(String stockcode, String marketId, Long startTs, Long endTs, Integer limit) {
        if (stockcode == null || stockcode.trim().isEmpty() || marketId == null || marketId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (limit != null && limit < 0) {
            // invalid limit -> return empty by convention to avoid interrupting service
            return Collections.emptyList();
        }
        List<PricePoint> list = store.get(key(stockcode, marketId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // take a snapshot to avoid ConcurrentModificationException while iterating
        List<PricePoint> snapshot = new ArrayList<>(list);
        List<PricePoint> range = snapshot.stream()
            .filter(p -> p != null && p.getTs() != null)
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
        if (stockcode == null || stockcode.trim().isEmpty() || marketId == null || marketId.trim().isEmpty()) {
            // ignore invalid key
            return 0;
        }
        if (points == null || points.isEmpty()) {
            return 0;
        }
        store.computeIfAbsent(key(stockcode, marketId), k -> new CopyOnWriteArrayList<>()).addAll(points);
        return points.size();
    }

    private String key(String stockcode, String marketId) {
        return stockcode + ":" + marketId;
    }
}
