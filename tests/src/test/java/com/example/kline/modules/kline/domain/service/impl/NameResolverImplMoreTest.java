package com.example.kline.modules.kline.domain.service.impl;

import com.example.kline.modules.kline.infrastructure.cache.RedisNameCache;
import com.example.kline.modules.kline.infrastructure.external.NameServiceHttp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Extended unit tests for NameResolverImpl service.
 * Tests advanced scenarios and edge cases.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class NameResolverImplMoreTest {

    static class CountingService extends NameServiceHttp {
        volatile int calls = 0;
        @Override
        public String fetchName(String stockcode, String marketId) {
            calls++;
            return "NAME-" + stockcode + '-' + marketId;
        }
    }

    @Test
    public void testResolveEmptyInput_returnsNonNullByCurrentImpl() {
        RedisNameCache cache = new RedisNameCache();
        CountingService svc = new CountingService();
        NameResolverImpl resolver = new NameResolverImpl(cache, svc);

        String out = resolver.resolve("", "");
        Assertions.assertNotNull(out);
        Assertions.assertFalse(out.trim().isEmpty());
    }

    @Test
    public void testResolveConcurrentAccess_afterWarmup_hasSingleExternalCall() throws Exception {
        RedisNameCache cache = new RedisNameCache();
        CountingService svc = new CountingService();
        NameResolverImpl resolver = new NameResolverImpl(cache, svc);

        // warmup once -> cache populated
        String sc = "600000"; String mk = "SH";
        String name1 = resolver.resolve(sc, mk);
        Assertions.assertEquals("NAME-600000-SH", name1);
        Assertions.assertEquals(1, svc.calls);

        int threads = 10;
        java.util.concurrent.ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try { resolver.resolve(sc, mk); } finally { latch.countDown(); }
            });
        }
        latch.await(3, TimeUnit.SECONDS);
        pool.shutdownNow();

        // after warmup, subsequent resolves should read cache only
        Assertions.assertEquals(1, svc.calls);
    }
}
