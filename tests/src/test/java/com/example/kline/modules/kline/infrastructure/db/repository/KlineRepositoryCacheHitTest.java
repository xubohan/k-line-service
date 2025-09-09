package com.example.kline.modules.kline.infrastructure.db.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Cache hit tests for KlineRepository.
 * Tests caching effectiveness and hit ratios.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class KlineRepositoryCacheHitTest {

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
    public void testFindRangeReturnsCacheWhenPresent() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineDao dao = new KlineDao();
        KlineRepositoryImpl repo = new KlineRepositoryImpl(cache, dao);

        KlineResponse seeded = new KlineResponse();
        seeded.setStockcode("SC");
        seeded.setMarketId("MK");
        seeded.addPricePoint(pp(1));
        seeded.addPricePoint(pp(2));
        cache.putBatch(seeded, 900);

        KlineResponse out = repo.findRange("SC", "MK", null, null, 1);
        Assertions.assertEquals(1, out.getData().size());
        Assertions.assertEquals(1L, out.getData().get(0).getTs().longValue());
    }
}

