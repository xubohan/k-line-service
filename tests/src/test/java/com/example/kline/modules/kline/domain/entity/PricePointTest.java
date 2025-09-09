package com.example.kline.modules.kline.domain.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PricePointTest {

    @Test
    public void testIsValidFalseWhenMissingFields() {
        PricePoint p = new PricePoint();
        p.setTs(1L);
        // 其余为 null，应判定为无效
        Assertions.assertFalse(p.isValid());

        // 逐步补齐但仍缺少一个字段 -> 仍然无效
        p.setOpen(BigDecimal.ONE);
        p.setHigh(BigDecimal.ONE);
        p.setLow(BigDecimal.ONE);
        p.setClose(BigDecimal.ONE);
        Assertions.assertFalse(p.isValid()); // 缺少 vol
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

        Assertions.assertTrue(p.isValid());
        // 覆盖 getters
        Assertions.assertEquals(2L, p.getTs());
        Assertions.assertEquals(new BigDecimal("1.2"), p.getOpen());
        Assertions.assertEquals(new BigDecimal("2.3"), p.getHigh());
        Assertions.assertEquals(new BigDecimal("0.8"), p.getLow());
        Assertions.assertEquals(new BigDecimal("1.5"), p.getClose());
        Assertions.assertEquals(10L, p.getVol());
    }
}

