package com.example.kline.modules.kline.domain.service.impl;

import com.example.kline.modules.kline.infrastructure.cache.RedisNameCache;
import com.example.kline.modules.kline.infrastructure.external.NameServiceHttp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for NameResolverImpl service.
 * Tests caching behavior and service integration.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class NameResolverImplTest {
    static class CountingNameService extends NameServiceHttp {
        int calls = 0;
        @Override
        public String fetchName(String stockcode, String marketId) {
            calls++;
            return "NAME-" + stockcode + '-' + marketId;
        }
    }

    @Test
    public void testResolveCachesResult() {
        RedisNameCache cache = new RedisNameCache();
        CountingNameService svc = new CountingNameService();
        NameResolverImpl resolver = new NameResolverImpl(cache, svc);

        String name1 = resolver.resolve("600000", "SH");
        Assertions.assertEquals("NAME-600000-SH", name1);
        Assertions.assertEquals(1, svc.calls);

        String name2 = resolver.resolve("600000", "SH");
        Assertions.assertEquals("NAME-600000-SH", name2);
        Assertions.assertEquals(1, svc.calls);
    }

    @Test
    public void testResolveReturnsCachedValueOverService() {
        RedisNameCache cache = new RedisNameCache();
        cache.setName("000001", "SZ", "CACHED-NAME", 3600);
        CountingNameService svc = new CountingNameService();
        NameResolverImpl resolver = new NameResolverImpl(cache, svc);

        String name = resolver.resolve("000001", "SZ");
        Assertions.assertEquals("CACHED-NAME", name);
        Assertions.assertEquals(0, svc.calls);
    }
}

