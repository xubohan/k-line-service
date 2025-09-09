package com.example.kline.modules.kline.domain.service.impl;

import com.example.kline.modules.kline.infrastructure.cache.RedisNameCache;
import com.example.kline.modules.kline.infrastructure.external.NameServiceHttp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * Unit tests for NameResolverImpl service.
 * Tests caching behavior and service integration using mocks.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class NameResolverImplTest {

    @Test
    public void testResolveCachesResult() {
        // Arrange
        RedisNameCache mockCache = Mockito.mock(RedisNameCache.class);
        NameServiceHttp mockService = Mockito.mock(NameServiceHttp.class);
        NameResolverImpl resolver = new NameResolverImpl(mockCache, mockService);
        
        String stockcode = "600000";
        String marketId = "SH";
        String expectedName = "NAME-600000-SH";
        
        // Mock cache miss on first call, hit on second
        when(mockCache.getName(stockcode, marketId))
            .thenReturn(null)  // first call - cache miss
            .thenReturn(expectedName);  // second call - cache hit
        when(mockService.fetchName(stockcode, marketId)).thenReturn(expectedName);

        // Act & Assert - First call should fetch from service
        String name1 = resolver.resolve(stockcode, marketId);
        Assertions.assertEquals(expectedName, name1);
        
        // Act & Assert - Second call should use cache
        String name2 = resolver.resolve(stockcode, marketId);
        Assertions.assertEquals(expectedName, name2);
        
        // Verify interactions
        verify(mockCache, times(2)).getName(stockcode, marketId);
        verify(mockService, times(1)).fetchName(stockcode, marketId);
        verify(mockCache, times(1)).setName(stockcode, marketId, expectedName, 3600);
    }

    @Test
    public void testResolveReturnsCachedValueOverService() {
        // Arrange
        RedisNameCache mockCache = Mockito.mock(RedisNameCache.class);
        NameServiceHttp mockService = Mockito.mock(NameServiceHttp.class);
        NameResolverImpl resolver = new NameResolverImpl(mockCache, mockService);
        
        String stockcode = "000001";
        String marketId = "SZ";
        String cachedName = "CACHED-NAME";
        
        // Mock cache hit
        when(mockCache.getName(stockcode, marketId)).thenReturn(cachedName);

        // Act
        String name = resolver.resolve(stockcode, marketId);
        
        // Assert
        Assertions.assertEquals(cachedName, name);
        
        // Verify service was never called
        verify(mockCache, times(1)).getName(stockcode, marketId);
        verify(mockService, never()).fetchName(any(), any());
        verify(mockCache, never()).setName(any(), any(), any(), anyLong());
    }
}

