package com.example.kline.modules.kline.domain.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused tests for PricePoint.isValid().
 * 当前实现规则：仅校验字段非空；不校验数值范围与高低关系。
 */
public class PricePointIsValidTest {

    private static PricePoint full() {
        PricePoint p = new PricePoint();
        p.setTs(1L);
        p.setOpen(BigDecimal.ONE);
        p.setHigh(BigDecimal.ONE);
        p.setLow(BigDecimal.ONE);
        p.setClose(BigDecimal.ONE);
        p.setVol(1L);
        return p;
    }

    @Test
    public void valid_when_all_fields_non_null() {
        assertTrue(full().isValid());
    }

    @Test
    public void invalid_when_ts_null() {
        PricePoint p = full();
        p.setTs(null);
        assertFalse(p.isValid());
    }

    @Test
    public void invalid_when_open_null() {
        PricePoint p = full();
        p.setOpen(null);
        assertFalse(p.isValid());
    }

    @Test
    public void invalid_when_high_null() {
        PricePoint p = full();
        p.setHigh(null);
        assertFalse(p.isValid());
    }

    @Test
    public void invalid_when_low_null() {
        PricePoint p = full();
        p.setLow(null);
        assertFalse(p.isValid());
    }

    @Test
    public void invalid_when_close_null() {
        PricePoint p = full();
        p.setClose(null);
        assertFalse(p.isValid());
    }

    @Test
    public void invalid_when_vol_null() {
        PricePoint p = full();
        p.setVol(null);
        assertFalse(p.isValid());
    }
}

