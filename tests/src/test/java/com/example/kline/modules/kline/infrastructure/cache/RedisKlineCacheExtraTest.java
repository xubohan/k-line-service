package com.example.kline.modules.kline.infrastructure.cache;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Extra unit tests for RedisKlineCache.
 * Tests additional functionality and edge cases.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class RedisKlineCacheExtraTest {
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
    public void testPutBatchOverwrite() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineResponse r1 = new KlineResponse();
        r1.setStockcode("S"); r1.setMarketId("M");
        r1.addPricePoint(pp(1)); r1.addPricePoint(pp(2));
        cache.putBatch(r1, 900);

        KlineResponse r2 = new KlineResponse();
        r2.setStockcode("S"); r2.setMarketId("M");
        r2.addPricePoint(pp(10));
        cache.putBatch(r2, 900);

        KlineResponse out = cache.getRange("S", "M", null, null, null);
        Assertions.assertEquals(1, out.getData().size());
        Assertions.assertEquals(10L, out.getData().get(0).getTs().longValue());
    }

    @Test
    public void testGetRangeSortOrder_ascending() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineResponse r = new KlineResponse();
        r.setStockcode("S"); r.setMarketId("M");
        r.addPricePoint(pp(3)); r.addPricePoint(pp(1)); r.addPricePoint(pp(2));
        cache.putBatch(r, 900);

        KlineResponse out = cache.getRange("S", "M", null, null, null);
        Assertions.assertEquals(3, out.getData().size());
        Assertions.assertEquals(1L, out.getData().get(0).getTs().longValue());
        Assertions.assertEquals(2L, out.getData().get(1).getTs().longValue());
        Assertions.assertEquals(3L, out.getData().get(2).getTs().longValue());
    }
}

