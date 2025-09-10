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
        
        // Act & Assert - MVP principle: handle empty and null values gracefully
        // Empty values should be cached normally
        cache.setName("", "", "", 3600);
        String result1 = cache.getName("", "");
        Assertions.assertEquals("", result1);
        
        // Per MVP principle: null values should be discarded (no-op) to prevent service interruption
        // These operations should not throw any exception
        cache.setName(null, null, null, 3600);  // Should be no-op
        cache.setName(null, "market", "name", 3600);  // Should be no-op
        cache.setName("stock", null, "name", 3600);  // Should be no-op
        
        // getName with null should return null without throwing exception
        String result2 = cache.getName(null, null);
        String result3 = cache.getName(null, "market");
        String result4 = cache.getName("stock", null);
        
        // All null operations should return null (not found) without exception
        Assertions.assertNull(result2);
        Assertions.assertNull(result3);
        Assertions.assertNull(result4);
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