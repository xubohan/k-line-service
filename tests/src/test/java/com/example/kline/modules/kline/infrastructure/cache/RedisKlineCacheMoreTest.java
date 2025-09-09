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
 * Extended unit tests for RedisKlineCache.
 * Tests advanced caching scenarios and validation using mocks.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
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
        // Arrange
        RedisKlineCache cache = new RedisKlineCache();
        
        // Act
        KlineResponse r = cache.getRange("X", "Y", null, null, null);
        
        // Assert
        Assertions.assertNotNull(r);
        Assertions.assertTrue(r.getData().isEmpty());
    }

    @Test
    public void testLimitBoundaryOne() {
        // Arrange
        RedisKlineCache cache = new RedisKlineCache();
        
        KlineResponse resp = new KlineResponse();
        resp.setStockcode("S");
        resp.setMarketId("M");
        resp.addPricePoint(pp(1));
        resp.addPricePoint(pp(2));
        cache.putBatch(resp, 900);
        
        // Act
        KlineResponse out = cache.getRange("S", "M", null, null, 1);
        
        // Assert
        Assertions.assertEquals(1, out.getData().size());
        Assertions.assertEquals(1L, out.getData().get(0).getTs().longValue());
    }
    
    @Test
    public void testConfiguration_ExternalDisabled() {
        // Arrange
        // Act
        RedisKlineCache cache = new RedisKlineCache();
        
        // Test that it works in memory mode
        KlineResponse resp = new KlineResponse();
        resp.setStockcode("TEST");
        resp.setMarketId("MK");
        resp.addPricePoint(pp(100));
        cache.putBatch(resp, 900);
        
        KlineResponse result = cache.getRange("TEST", "MK", null, null, null);
        
        // Assert
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertEquals(100L, result.getData().get(0).getTs().longValue());
    }
}

