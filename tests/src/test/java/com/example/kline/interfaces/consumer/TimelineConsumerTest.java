package com.example.kline.interfaces.consumer;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for TimelineConsumer.
 * Tests consumer behavior with mock repository.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class TimelineConsumerTest {
    @Test
    public void testConsumeDelegatesToRepository() {
        KlineRepository repo = Mockito.mock(KlineRepository.class);
        TimelineConsumer consumer = new TimelineConsumer(repo);

        KlineResponse resp = new KlineResponse();
        resp.setStockcode("600000");
        resp.setMarketId("SH");
        PricePoint p = new PricePoint();
        p.setTs(1L);
        p.setOpen(BigDecimal.ONE);
        p.setHigh(BigDecimal.TEN);
        p.setLow(BigDecimal.ZERO);
        p.setClose(BigDecimal.ONE);
        p.setVol(100L);
        resp.addPricePoint(p);

        consumer.run(resp);

        Mockito.verify(repo, Mockito.times(1)).upsertBatch(resp);
        Mockito.verifyNoMoreInteractions(repo);
    }
}

