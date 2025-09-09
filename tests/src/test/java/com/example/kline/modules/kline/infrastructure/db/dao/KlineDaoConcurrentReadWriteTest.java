package com.example.kline.modules.kline.infrastructure.db.dao;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Concurrency test: concurrent writers and readers on the same key should not throw
 * ConcurrentModificationException and results should remain sorted and consistent.
 */
public class KlineDaoConcurrentReadWriteTest {

    @Test
    public void testConcurrentInsertAndSelect_sameKey_noCME_andConsistent() throws Exception {
        KlineDao dao = new KlineDao();
        final String stock = "CONC";
        final String market = "SZ";

        final int writerThreads = 4;
        final int readerThreads = 4;
        final int writerIterations = 200; // each writer writes 5 points per iteration => 1000 per writer
        final int batchSize = 5;

        ExecutorService pool = Executors.newFixedThreadPool(writerThreads + readerThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(writerThreads + readerThreads);
        AtomicLong seq = new AtomicLong(1L);
        AtomicInteger insertedTotal = new AtomicInteger(0);
        Queue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // writers
        for (int w = 0; w < writerThreads; w++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < writerIterations; i++) {
                        List<PricePoint> pts = new ArrayList<>(batchSize);
                        long base = seq.getAndAdd(batchSize);
                        for (int j = 0; j < batchSize; j++) {
                            PricePoint p = new PricePoint();
                            p.setTs(base + j);
                            p.setOpen(BigDecimal.ONE);
                            p.setHigh(BigDecimal.ONE);
                            p.setLow(BigDecimal.ONE);
                            p.setClose(BigDecimal.ONE);
                            p.setVol(1L);
                            pts.add(p);
                        }
                        int n = dao.insertBatch(stock, market, pts);
                        insertedTotal.addAndGet(n);
                        // small yield to increase interleaving
                        if ((i & 7) == 0) Thread.yield();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // readers
        for (int r = 0; r < readerThreads; r++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < writerIterations * 2; i++) {
                        List<PricePoint> out = dao.selectRange(stock, market, null, null, null);
                        // verify sorted and non-null ts
                        long prev = Long.MIN_VALUE;
                        for (PricePoint p : out) {
                            Assertions.assertNotNull(p.getTs());
                            long cur = p.getTs();
                            if (cur < prev) {
                                Assertions.fail("result not sorted ascending by ts");
                            }
                            prev = cur;
                        }
                        if ((i & 15) == 0) Thread.yield();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // fire!
        startGate.countDown();
        boolean finished = doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        Assertions.assertTrue(finished, "concurrent tasks did not finish in time");
        if (!errors.isEmpty()) {
            errors.forEach(Throwable::printStackTrace);
        }
        Assertions.assertTrue(errors.isEmpty(), "errors occurred during concurrent read/write: " + errors);

        // After writers done, final snapshot should match insertedTotal
        List<PricePoint> finalOut = dao.selectRange(stock, market, null, null, null);
        Assertions.assertEquals(writerThreads * writerIterations * batchSize, insertedTotal.get());
        Assertions.assertEquals(insertedTotal.get(), finalOut.size());
        // final sorted check
        long prev = Long.MIN_VALUE;
        for (PricePoint p : finalOut) {
            long cur = p.getTs();
            if (cur < prev) {
                Assertions.fail("final result not sorted ascending by ts");
            }
            prev = cur;
        }
    }
}

