package com.example.kline.modules.kline.domain.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PricePointTest {

    @Test
    void testIsValid_AllFieldsNull() {
        PricePoint point = new PricePoint();
        assertFalse(point.isValid());
    }

    @Test
    void testIsValid_PartialNull() {
        PricePoint point = new PricePoint();
        point.setTs(1L);
        point.setOpen(BigDecimal.ONE);
        assertFalse(point.isValid());
    }

    @Test
    public void testIsValidTrueAndGetters() {
        PricePoint p = new PricePoint();
        p.setTs(2L);
        p.setOpen(new BigDecimal("1.2"));
        p.setHigh(new BigDecimal("2.3"));
        p.setLow(new BigDecimal("0.8"));
        p.setClose(new BigDecimal("1.5"));
        p.setVol(10L);

        assertTrue(p.isValid());
        assertEquals(2L, p.getTs());
        assertEquals(new BigDecimal("1.2"), p.getOpen());
        assertEquals(new BigDecimal("2.3"), p.getHigh());
        assertEquals(new BigDecimal("0.8"), p.getLow());
        assertEquals(new BigDecimal("1.5"), p.getClose());
        assertEquals(10L, p.getVol());
    }

    @Test
    void testPriceRelations() {
        PricePoint point = new PricePoint();
        point.setTs(1L);
        point.setOpen(BigDecimal.ONE);
        point.setHigh(BigDecimal.valueOf(1));
        point.setLow(BigDecimal.valueOf(2));
        point.setClose(BigDecimal.TEN);
        point.setVol(1L);
        // current implementation does not verify price relations
        assertTrue(point.isValid());
    }

    @Test
    void testNegativeValues() {
        PricePoint point = new PricePoint();
        point.setTs(1L);
        BigDecimal negative = BigDecimal.valueOf(-1);
        point.setOpen(negative);
        point.setHigh(negative);
        point.setLow(negative);
        point.setClose(negative);
        point.setVol(-1L);
        // current implementation only checks for nulls
        assertTrue(point.isValid());
    }
}
