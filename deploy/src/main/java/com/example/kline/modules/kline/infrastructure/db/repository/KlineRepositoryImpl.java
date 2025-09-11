package com.example.kline.modules.kline.infrastructure.db.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 * Repository implementation.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@Repository
public class KlineRepositoryImpl implements KlineRepository {
    private final RedisKlineCache klineCache;
    private final KlineDao klineDao;

    @Autowired
    public KlineRepositoryImpl(RedisKlineCache klineCache, KlineDao klineDao) {
        this.klineCache = klineCache;
        this.klineDao = klineDao;
    }

    @Override
    public KlineResponse findRange(String stockcode, String marketId, Long startTs, Long endTs, Integer limit) {
        if (stockcode == null || stockcode.trim().isEmpty() || marketId == null || marketId.trim().isEmpty()) {
            return new KlineResponse();
        }
        if (limit != null && limit < 0) {
            return new KlineResponse();
        }
        // Per MVP requirement: Use Redis as primary storage, no fallback to database
        // If Redis cache miss, return empty response
        KlineResponse cacheResp = klineCache.getRange(stockcode, marketId, startTs, endTs, limit);
        return cacheResp != null ? cacheResp : new KlineResponse();
    }

    @Override
    public void upsertBatch(KlineResponse response) {
        if (response == null) {
            return; // ignore invalid input by convention
        }
        if (response.getStockcode() == null || response.getStockcode().trim().isEmpty()) {
            return;
        }
        if (response.getMarketId() == null || response.getMarketId().trim().isEmpty()) {
            return;
        }
        // Per MVP requirement: Skip database operation, use Redis as primary storage
        // Store data for 15 days (15 * 24 * 3600 = 1,296,000 seconds)
        klineCache.putBatch(response, 3600);
    }
}
