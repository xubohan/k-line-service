package com.example.kline.modules.kline.infrastructure.db.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

/**
 * Repository implementation.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@Repository
@RequiredArgsConstructor
public class KlineRepositoryImpl implements KlineRepository {
    private final RedisKlineCache klineCache;
    private final KlineDao klineDao;

    @Override
    public KlineResponse findRange(String stockcode, String marketId, Long startTs, Long endTs, Integer limit) {
        KlineResponse cacheResp = klineCache.getRange(stockcode, marketId, startTs, endTs, limit);
        if (!CollectionUtils.isEmpty(cacheResp.getData())) {
            return cacheResp;
        }
        List<PricePoint> dbData = klineDao.selectRange(stockcode, marketId, startTs, endTs, limit);
        KlineResponse resp = new KlineResponse();
        resp.setStockcode(stockcode);
        resp.setMarketId(marketId);
        dbData.forEach(resp::addPricePoint);
        klineCache.putBatch(resp, 900);
        return resp;
    }

    @Override
    public void upsertBatch(KlineResponse response) {
        klineDao.insertBatch(response.getStockcode(), response.getMarketId(), response.getData());
        klineCache.putBatch(response, 900);
    }
}
