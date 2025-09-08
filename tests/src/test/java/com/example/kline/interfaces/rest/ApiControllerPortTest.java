package com.example.kline.interfaces.rest;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@org.springframework.test.context.TestPropertySource(properties = {
        "server.port=18080"
})
public class ApiControllerPortTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KlineDao klineDao;

    @BeforeEach
    public void seed() {
        java.util.List<PricePoint> list = new java.util.ArrayList<>();
        list.add(pp(100L));
        list.add(pp(200L));
        klineDao.insertBatch("900001", "SZ", list);
    }

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
    public void testFixedPortEndpointResponds() {
        String url = "http://localhost:18080/kline?stockcode=900001&marketId=SZ&startTs=50&endTs=250";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        Assertions.assertEquals(200, resp.getStatusCodeValue());
        Assertions.assertTrue(resp.getBody() != null && resp.getBody().contains("900001"));
    }
}

