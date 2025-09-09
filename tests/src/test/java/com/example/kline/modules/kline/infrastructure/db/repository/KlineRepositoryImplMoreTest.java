package com.example.kline.modules.kline.infrastructure.db.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Extended unit tests for KlineRepositoryImpl.
 * Tests advanced repository patterns and integration scenarios.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class KlineRepositoryImplMoreTest {
    private static PricePoint pp(long ts) {
        PricePoint p = new PricePoint();
        p.setTs(ts);
        p.setOpen(BigDecimal.ONE);
        p.setHigh(BigDecimal.ONE);
        p.setLow(BigDecimal.ONE);
        p.setClose(BigDecimal.ONE);
        p.setVol(1L);
        return p;
    }

    @Test
    public void testFindRange_LimitExceedsData_returnsAll() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineDao dao = new KlineDao();
        KlineRepositoryImpl repo = new KlineRepositoryImpl(cache, dao);
        String sc = "LIM"; String mk = "SZ";
        dao.insertBatch(sc, mk, java.util.Arrays.asList(pp(1), pp(2), pp(3), pp(4), pp(5)));

        KlineResponse out = repo.findRange(sc, mk, null, null, Integer.MAX_VALUE);
        Assertions.assertEquals(5, out.getData().size());
    }

    @Test
    public void testUpsertBatch_EmptyData_noException() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineDao dao = new KlineDao();
        KlineRepositoryImpl repo = new KlineRepositoryImpl(cache, dao);

        KlineResponse empty = new KlineResponse();
        empty.setStockcode("E"); empty.setMarketId("M");
        repo.upsertBatch(empty);

        KlineResponse out = repo.findRange("E", "M", null, null, null);
        Assertions.assertTrue(out.getData().isEmpty());
    }
}

