package com.example.kline.modules.kline.infrastructure.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for RedisNameCache.
 * Tests caching functionality for stock names.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-10 01:30:00
 */
public class RedisNameCacheTest {

    @Test
    public void testSetAndGetName() {
        // Arrange
        RedisNameCache cache = new RedisNameCache();
        String stockcode = "600000";
        String marketId = "SH";
        String stockName = "浦发银行";
        
        // Act
        cache.setName(stockcode, marketId, stockName, 3600);
        String result = cache.getName(stockcode, marketId);
        
        // Assert
        Assertions.assertEquals(stockName, result);
    }

    @Test
    public void testGetName_NonExistent_ReturnsNull() {
        // Arrange
        RedisNameCache cache = new RedisNameCache();
        
        // Act
        String result = cache.getName("NONEXISTENT", "XX");
        
        // Assert
        Assertions.assertNull(result);
    }

    @Test
    public void testSetName_EmptyValues() {
        // Arrange
        RedisNameCache cache = new RedisNameCache();
        
        // Act & Assert - should not throw exceptions
        cache.setName("", "", "", 3600);
        cache.setName(null, null, null, 3600);
        
        String result1 = cache.getName("", "");
        String result2 = cache.getName(null, null);
        
        // These may return null or empty based on implementation
        // The test mainly ensures no exceptions are thrown
    }

    @Test
    public void testSetName_ZeroTTL() {
        // Arrange
        RedisNameCache cache = new RedisNameCache();
        String stockcode = "300033";
        String marketId = "SZ";
        String stockName = "同花顺";
        
        // Act
        cache.setName(stockcode, marketId, stockName, 0);
        
        // Assert - behavior with 0 TTL
        String result = cache.getName(stockcode, marketId);
        // May or may not be cached depending on implementation
    }

    @Test
    public void testGetName_DifferentMarkets() {
        // Arrange
        RedisNameCache cache = new RedisNameCache();
        String stockcode = "000001";
        String stockName = "平安银行";
        
        // Act
        cache.setName(stockcode, "SZ", stockName, 3600);
        
        // Assert
        Assertions.assertEquals(stockName, cache.getName(stockcode, "SZ"));
        Assertions.assertNull(cache.getName(stockcode, "SH")); // Different market
    }

    @Test
    public void testCacheKeyGeneration() {
        // Arrange
        RedisNameCache cache = new RedisNameCache();
        
        // Act & Assert - Test that different stock/market combinations are treated separately
        cache.setName("600000", "SH", "浦发银行", 3600);
        cache.setName("600000", "SZ", "不同银行", 3600); // Same stock, different market
        
        Assertions.assertEquals("浦发银行", cache.getName("600000", "SH"));
        Assertions.assertEquals("不同银行", cache.getName("600000", "SZ"));
    }
}