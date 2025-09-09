package com.example.kline.modules.kline.infrastructure.db.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for KlineRepositoryImpl.
 * Tests repository pattern with cache and DAO integration.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class KlineRepositoryImplTest {
    private PricePoint pp(long ts) {
        PricePoint p = new PricePoint();
        p.setTs(ts);
        p.setOpen(BigDecimal.ONE);
        p.setHigh(BigDecimal.TEN);
        p.setLow(BigDecimal.ZERO);
        p.setClose(BigDecimal.ONE);
        p.setVol(1L);
        return p;
    }

    @Test
    public void testFindRangeFallsBackToDaoThenCaches() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineDao dao = new KlineDao();
        KlineRepositoryImpl repo = new KlineRepositoryImpl(cache, dao);

        dao.insertBatch("000002", "SZ", java.util.Arrays.asList(pp(1), pp(2), pp(3)));

        KlineResponse r1 = repo.findRange("000002", "SZ", 2L, 3L, null);
        Assertions.assertEquals(2, r1.getData().size());

        KlineRepositoryImpl repo2 = new KlineRepositoryImpl(cache, new KlineDao());
        KlineResponse r2 = repo2.findRange("000002", "SZ", 2L, 3L, null);
        Assertions.assertEquals(2, r2.getData().size());
    }

    @Test
    public void testUpsertBatchWritesDaoAndCache() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineDao dao = new KlineDao();
        KlineRepositoryImpl repo = new KlineRepositoryImpl(cache, dao);

        KlineResponse resp = new KlineResponse();
        resp.setStockcode("000003");
        resp.setMarketId("SZ");
        resp.addPricePoint(pp(10));
        resp.addPricePoint(pp(11));

        repo.upsertBatch(resp);

        KlineResponse fromCache = cache.getRange("000003", "SZ", null, null, null);
        Assertions.assertEquals(2, fromCache.getData().size());

        KlineResponse fromDao = repo.findRange("000003", "SZ", null, null, null);
        Assertions.assertEquals(2, fromDao.getData().size());
    }
}

