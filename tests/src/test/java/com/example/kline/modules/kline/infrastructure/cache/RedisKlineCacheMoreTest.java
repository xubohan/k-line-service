package com.example.kline.modules.kline.infrastructure.cache;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedisKlineCacheMoreTest {

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
    public void testGetRangeWhenNoKey_returnsEmpty() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineResponse r = cache.getRange("X", "Y", null, null, null);
        Assertions.assertNotNull(r);
        Assertions.assertTrue(r.getData().isEmpty());
    }

    @Test
    public void testLimitBoundaryOne() {
        RedisKlineCache cache = new RedisKlineCache();
        KlineResponse resp = new KlineResponse();
        resp.setStockcode("S"); resp.setMarketId("M");
        resp.addPricePoint(pp(1)); resp.addPricePoint(pp(2));
        cache.putBatch(resp, 900);
        KlineResponse out = cache.getRange("S", "M", null, null, 1);
        Assertions.assertEquals(1, out.getData().size());
        Assertions.assertEquals(1L, out.getData().get(0).getTs().longValue());
    }
}

