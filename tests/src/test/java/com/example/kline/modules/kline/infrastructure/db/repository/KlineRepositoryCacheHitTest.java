package com.example.kline.modules.kline.infrastructure.db.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Cache hit tests for KlineRepository.
 * Tests caching effectiveness and hit ratios using mocks.
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
        // Arrange
        RedisKlineCache mockCache = Mockito.mock(RedisKlineCache.class);
        KlineDao mockDao = Mockito.mock(KlineDao.class);
        KlineRepositoryImpl repo = new KlineRepositoryImpl(mockCache, mockDao);
        
        String stockcode = "SC";
        String marketId = "MK";
        
        // Mock cache hit with data
        KlineResponse cachedResponse = new KlineResponse();
        cachedResponse.setStockcode(stockcode);
        cachedResponse.setMarketId(marketId);
        cachedResponse.addPricePoint(pp(1));
        cachedResponse.addPricePoint(pp(2));
        
        when(mockCache.getRange(stockcode, marketId, null, null, 1))
            .thenReturn(cachedResponse);

        // Act
        KlineResponse result = repo.findRange(stockcode, marketId, null, null, 1);
        
        // Assert
        Assertions.assertEquals(2, result.getData().size());
        Assertions.assertEquals(stockcode, result.getStockcode());
        Assertions.assertEquals(marketId, result.getMarketId());
        
        // Verify cache was used and DAO was not accessed
        verify(mockCache, times(1)).getRange(stockcode, marketId, null, null, 1);
        verify(mockDao, never()).selectRange(any(), any(), any(), any(), any());
        verify(mockCache, never()).putBatch(any(), anyLong());
    }
    
    @Test
    public void testFindRange_CacheMiss_AccessesDao() {
        // Arrange
        RedisKlineCache mockCache = Mockito.mock(RedisKlineCache.class);
        KlineDao mockDao = Mockito.mock(KlineDao.class);
        KlineRepositoryImpl repo = new KlineRepositoryImpl(mockCache, mockDao);
        
        String stockcode = "SC2";
        String marketId = "MK2";
        
        // Mock cache miss (empty response)
        KlineResponse emptyResponse = new KlineResponse();
        when(mockCache.getRange(stockcode, marketId, null, null, null))
            .thenReturn(emptyResponse);
        
        // Mock DAO response
        when(mockDao.selectRange(stockcode, marketId, null, null, null))
            .thenReturn(java.util.Arrays.asList(pp(10), pp(20)));

        // Act
        KlineResponse result = repo.findRange(stockcode, marketId, null, null, null);
        
        // Assert
        Assertions.assertEquals(2, result.getData().size());
        
        // Verify both cache and DAO were accessed
        verify(mockCache, times(1)).getRange(stockcode, marketId, null, null, null);
        verify(mockDao, times(1)).selectRange(stockcode, marketId, null, null, null);
        verify(mockCache, times(1)).putBatch(any(KlineResponse.class), eq(900L));
    }
}

