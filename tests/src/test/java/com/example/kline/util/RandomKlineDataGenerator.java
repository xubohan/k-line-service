package com.example.kline.util;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Helper to generate random but valid k-line points for tests.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public final class RandomKlineDataGenerator {
    private RandomKlineDataGenerator() {}

    public static List<PricePoint> generateSequential(int count, long startEpoch) {
        List<PricePoint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(pp(startEpoch + i));
        }
        return list;
    }

    public static List<PricePoint> generateRandom(int count, String yyyymmdd) {
        Random r = new Random(1234);
        List<PricePoint> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int H = 9 + r.nextInt(6); // 9-14
            int M = r.nextInt(60);
            long ts = toEpoch(yyyymmdd, H, M);
            list.add(pp(ts));
        }
        return list;
    }

    private static long toEpoch(String yyyymmdd, int H, int M) {
        int y = Integer.parseInt(yyyymmdd.substring(0, 4));
        int m = Integer.parseInt(yyyymmdd.substring(4, 6));
        int d = Integer.parseInt(yyyymmdd.substring(6, 8));
        return LocalDateTime.of(y, m, d, H, M).toEpochSecond(ZoneOffset.UTC);
    }

    private static PricePoint pp(long ts) {
        PricePoint p = new PricePoint();
        p.setTs(ts);
        BigDecimal v = BigDecimal.valueOf(1.0);
        p.setOpen(v); p.setHigh(v); p.setLow(v); p.setClose(v); p.setVol(1L);
        return p;
    }
}

