package com.example.kline.modules.kline.infrastructure.db.dao;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.util.RandomKlineDataGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KlineDaoMoreTest {

    @Test
    public void testBatchInsertPerformance_1000_under1s() {
        KlineDao dao = new KlineDao();
        List<PricePoint> pts = RandomKlineDataGenerator.generateSequential(1000, 1L);
        long t0 = System.nanoTime();
        int n = dao.insertBatch("PERF", "SZ", pts);
        long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        Assertions.assertEquals(1000, n);
        Assertions.assertTrue(costMs < 1000, "insert 1000 should <1s but was " + costMs + "ms");
    }

    @Test
    public void testLargeDataQuery_withLimitAndRange() {
        KlineDao dao = new KlineDao();
        List<PricePoint> pts = RandomKlineDataGenerator.generateSequential(5000, 10L);
        dao.insertBatch("BIG", "SH", pts);

        // range in the middle
        List<PricePoint> mid = dao.selectRange("BIG", "SH", 2000L, 2010L, null);
        Assertions.assertEquals(11, mid.size());

        // limit greater than data size
        List<PricePoint> limited = dao.selectRange("BIG", "SH", 0L, 100L, 1000);
        Assertions.assertEquals(91, limited.size()); // ts=10..100 inclusive
    }

    @Test
    public void testConcurrentInsert_multipleKeys_noDeadlock() throws Exception {
        KlineDao dao = new KlineDao();
        int threads = 8;
        CountDownLatch latch = new CountDownLatch(threads);
        java.util.concurrent.ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    String stock = "C" + idx;
                    List<PricePoint> pts = RandomKlineDataGenerator.generateSequential(200, idx * 1000L);
                    dao.insertBatch(stock, "MK", pts);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        int total = 0;
        for (int i = 0; i < threads; i++) {
            total += dao.selectRange("C" + i, "MK", null, null, null).size();
        }
        Assertions.assertEquals(threads * 200, total);
    }
}
