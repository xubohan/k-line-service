package com.example.kline.modules.kline.infrastructure.cache;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisKlineCache.
 * Tests caching operations and data retrieval logic using mocks.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class RedisKlineCacheTest {
    
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
    public void testInMemoryMode_PutAndRangeAndLimit() {
        // Test with in-memory mode (no external Redis)
        RedisKlineCache cache = new RedisKlineCache();
        
        KlineResponse resp = new KlineResponse();
        resp.setStockcode("000001");
        resp.setMarketId("SZ");
        resp.addPricePoint(pp(1));
        resp.addPricePoint(pp(2));
        resp.addPricePoint(pp(3));
        cache.putBatch(resp, 900);

        KlineResponse r1 = cache.getRange("000001", "SZ", 2L, 3L, null);
        Assertions.assertEquals(2, r1.getData().size());

        KlineResponse r2 = cache.getRange("000001", "SZ", null, null, 2);
        Assertions.assertEquals(2, r2.getData().size());

        KlineResponse r3 = cache.getRange("000001", "SZ", 4L, 5L, null);
        Assertions.assertEquals(0, r3.getData().size());
    }
    
    @Test
    public void testGetRange_InvalidInputs_ReturnsEmpty() {
        RedisKlineCache cache = new RedisKlineCache();
        
        // Test null/empty stockcode
        KlineResponse result1 = cache.getRange(null, "SZ", null, null, null);
        Assertions.assertTrue(result1.getData().isEmpty());
        
        KlineResponse result2 = cache.getRange("", "SZ", null, null, null);
        Assertions.assertTrue(result2.getData().isEmpty());
        
        // Test negative limit
        KlineResponse result3 = cache.getRange("SC", "MK", null, null, -1);
        Assertions.assertTrue(result3.getData().isEmpty());
    }
}

