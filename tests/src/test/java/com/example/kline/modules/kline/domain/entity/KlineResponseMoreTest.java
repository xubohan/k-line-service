package com.example.kline.modules.kline.domain.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Extended unit tests for KlineResponse domain entity.
 * Tests validation scenarios and property accessors.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class KlineResponseMoreTest {

    private static PricePoint valid(long ts) {
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
    public void testValidateDataTrueAndFalse() {
        KlineResponse r = new KlineResponse();
        r.addPricePoint(valid(1));
        r.addPricePoint(valid(2));
        Assertions.assertTrue(r.validateData());

        // 加入一个不合法的点 -> validateData 为 false
        PricePoint bad = new PricePoint();
        bad.setTs(3L);
        // 不设置 open/high/low/close/vol -> 无效
        r.addPricePoint(bad);
        Assertions.assertFalse(r.validateData());
    }

    @Test
    public void testStockNameSetterGetter() {
        KlineResponse r = new KlineResponse();
        r.setStockName("ABC");
        Assertions.assertEquals("ABC", r.getStockName());
    }
}

