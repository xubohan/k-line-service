package com.example.kline.modules.kline.infrastructure.db.dao;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KlineDaoTest {
    private PricePoint pp(long ts) {
        PricePoint p = new PricePoint();
        p.setTs(ts);
        p.setOpen(BigDecimal.ONE);
        p.setHigh(BigDecimal.TEN);
        p.setLow(BigDecimal.ZERO);
        p.setClose(BigDecimal.ONE);
        p.setVol(1L);
        return p;
    }

    @Test
    public void testInsertAndSelectWithLimit() {
        KlineDao dao = new KlineDao();
        dao.insertBatch("100001", "SH", Arrays.asList(pp(1), pp(2), pp(3)));
        List<PricePoint> all = dao.selectRange("100001", "SH", null, null, null);
        Assertions.assertEquals(3, all.size());
        List<PricePoint> limited = dao.selectRange("100001", "SH", null, null, 2);
        Assertions.assertEquals(2, limited.size());
    }
}

