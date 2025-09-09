package com.example.kline.modules.kline.infrastructure.db.repository;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Unit tests for KlineRepositoryImpl.
 * Tests repository pattern with cache and DAO integration using mocks.
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
    public void testFindRange_CacheMiss_FallsBackToDao() {
        // Arrange
        RedisKlineCache mockCache = Mockito.mock(RedisKlineCache.class);
        KlineDao mockDao = Mockito.mock(KlineDao.class);
        KlineRepositoryImpl repo = new KlineRepositoryImpl(mockCache, mockDao);
        
        String stockcode = "000002";
        String marketId = "SZ";
        
        // Mock cache miss (empty response)
        KlineResponse emptyResponse = new KlineResponse();
        when(mockCache.getRange(stockcode, marketId, 2L, 3L, null)).thenReturn(emptyResponse);
        
        // Mock DAO data
        when(mockDao.selectRange(stockcode, marketId, 2L, 3L, null))
            .thenReturn(Arrays.asList(pp(2), pp(3)));

        // Act
        KlineResponse result = repo.findRange(stockcode, marketId, 2L, 3L, null);
        
        // Assert
        Assertions.assertEquals(2, result.getData().size());
        Assertions.assertEquals(stockcode, result.getStockcode());
        Assertions.assertEquals(marketId, result.getMarketId());
        
        // Verify interactions
        verify(mockCache, times(1)).getRange(stockcode, marketId, 2L, 3L, null);
        verify(mockDao, times(1)).selectRange(stockcode, marketId, 2L, 3L, null);
        verify(mockCache, times(1)).putBatch(any(KlineResponse.class), eq(900L));
    }
    
    @Test
    public void testFindRange_CacheHit_SkipsDao() {
        // Arrange
        RedisKlineCache mockCache = Mockito.mock(RedisKlineCache.class);
        KlineDao mockDao = Mockito.mock(KlineDao.class);
        KlineRepositoryImpl repo = new KlineRepositoryImpl(mockCache, mockDao);
        
        String stockcode = "000003";
        String marketId = "SZ";
        
        // Mock cache hit
        KlineResponse cachedResponse = new KlineResponse();
        cachedResponse.addPricePoint(pp(1));
        cachedResponse.addPricePoint(pp(2));
        when(mockCache.getRange(stockcode, marketId, null, null, null)).thenReturn(cachedResponse);

        // Act
        KlineResponse result = repo.findRange(stockcode, marketId, null, null, null);
        
        // Assert
        Assertions.assertEquals(2, result.getData().size());
        
        // Verify cache was called but DAO was not
        verify(mockCache, times(1)).getRange(stockcode, marketId, null, null, null);
        verify(mockDao, never()).selectRange(any(), any(), any(), any(), any());
        verify(mockCache, never()).putBatch(any(KlineResponse.class), anyLong());
    }

    @Test
    public void testUpsertBatch_WritesToBothDaoAndCache() {
        // Arrange
        RedisKlineCache mockCache = Mockito.mock(RedisKlineCache.class);
        KlineDao mockDao = Mockito.mock(KlineDao.class);
        KlineRepositoryImpl repo = new KlineRepositoryImpl(mockCache, mockDao);
        
        KlineResponse response = new KlineResponse();
        response.setStockcode("000003");
        response.setMarketId("SZ");
        response.addPricePoint(pp(10));
        response.addPricePoint(pp(11));
        
        when(mockDao.insertBatch("000003", "SZ", response.getData())).thenReturn(2);

        // Act
        repo.upsertBatch(response);
        
        // Assert
        verify(mockDao, times(1)).insertBatch("000003", "SZ", response.getData());
        verify(mockCache, times(1)).putBatch(response, 900L);
    }
    
    @Test
    public void testUpsertBatch_InvalidInput_Ignored() {
        // Arrange
        RedisKlineCache mockCache = Mockito.mock(RedisKlineCache.class);
        KlineDao mockDao = Mockito.mock(KlineDao.class);
        KlineRepositoryImpl repo = new KlineRepositoryImpl(mockCache, mockDao);
        
        // Act - null response
        repo.upsertBatch(null);
        
        // Act - empty stockcode
        KlineResponse invalidResponse = new KlineResponse();
        invalidResponse.setStockcode("");
        invalidResponse.setMarketId("SZ");
        repo.upsertBatch(invalidResponse);
        
        // Assert - no interactions should occur
        verify(mockDao, never()).insertBatch(any(), any(), any());
        verify(mockCache, never()).putBatch(any(), anyLong());
    }
}

