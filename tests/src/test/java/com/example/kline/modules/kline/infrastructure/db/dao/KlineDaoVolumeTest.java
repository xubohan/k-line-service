package com.example.kline.modules.kline.infrastructure.db.dao;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.util.RandomKlineDataGenerator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KlineDaoVolumeTest {

    @Test
    public void testInsertAndQuery_3600_points_ok() {
        KlineDao dao = new KlineDao();
        List<PricePoint> pts = RandomKlineDataGenerator.generateSequential(3600, 1L);
        int n = dao.insertBatch("VOL", "SZ", pts);
        Assertions.assertEquals(3600, n);

        List<PricePoint> all = dao.selectRange("VOL", "SZ", null, null, null);
        Assertions.assertEquals(3600, all.size());
        // verify ordering ascending by ts
        Assertions.assertTrue(all.get(0).getTs() <= all.get(1).getTs());
        Assertions.assertTrue(all.get(3598).getTs() <= all.get(3599).getTs());
    }
}

