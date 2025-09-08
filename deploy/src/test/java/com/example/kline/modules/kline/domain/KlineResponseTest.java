package com.example.kline.modules.kline.domain;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for KlineResponse.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
public class KlineResponseTest {
    @Test
    public void testGetDataInRange() {
        KlineResponse resp = new KlineResponse();
        resp.addPricePoint(create(1L));
        resp.addPricePoint(create(2L));
        resp.addPricePoint(create(3L));
        List<PricePoint> result = resp.getDataInRange(2L, 3L);
        Assertions.assertEquals(2, result.size());
    }

    private PricePoint create(Long ts) {
        PricePoint p = new PricePoint();
        p.setTs(ts);
        p.setOpen(BigDecimal.ONE);
        p.setHigh(BigDecimal.TEN);
        p.setLow(BigDecimal.ZERO);
        p.setClose(BigDecimal.ONE);
        p.setVol(1L);
        return p;
    }
}
