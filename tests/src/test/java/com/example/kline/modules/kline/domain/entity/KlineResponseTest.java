package com.example.kline.modules.kline.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * Unit tests for KlineResponse entity.
 * Tests price point management and range operations.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class KlineResponseTest {

    private PricePoint createPoint(long ts) {
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
    void testAddPricePoint_Normal() {
        KlineResponse resp = new KlineResponse();
        PricePoint point = createPoint(1L);
        resp.addPricePoint(point);
        assertEquals(1, resp.getData().size());
        assertSame(point, resp.getData().get(0));
    }

    @Test
    void testAddPricePoint_Null() {
        KlineResponse resp = new KlineResponse();
        resp.addPricePoint(null);
        assertTrue(resp.getData().isEmpty());
    }

    @Test
    void testGetDataInRange_BoundaryCase() {
        KlineResponse resp = new KlineResponse();
        resp.addPricePoint(createPoint(1L));
        resp.addPricePoint(createPoint(2L));
        List<PricePoint> result = resp.getDataInRange(1L, 1L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getTs());
    }

    @Test
    void testGetDataInRange_EmptyResult() {
        KlineResponse resp = new KlineResponse();
        resp.addPricePoint(createPoint(1L));
        List<PricePoint> result = resp.getDataInRange(2L, 3L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateData_MixedValid() {
        KlineResponse resp = new KlineResponse();
        resp.addPricePoint(createPoint(1L));
        PricePoint invalid = new PricePoint();
        invalid.setTs(2L);
        // missing other fields -> invalid
        resp.addPricePoint(invalid);
        assertFalse(resp.validateData());
    }
}
