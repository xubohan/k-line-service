package com.example.kline.interfaces.rest;

import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import com.example.kline.modules.kline.infrastructure.cache.RedisNameCache;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@org.springframework.test.context.TestPropertySource(properties = {
        "server.port=18080",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
public class ApiControllerHttpTest {

    @Autowired private TestRestTemplate rest;
    @Autowired private KlineDao dao;
    @Autowired private RedisNameCache nameCache;

    private String stockcode;
    private String marketId;

    @BeforeEach
    public void seed() throws Exception {
        // Load kafka_data.json from test resources and seed in-memory DAO
        ObjectMapper om = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/kafka_data.json")) {
            List<Map<String, Object>> rows = om.readValue(is, new TypeReference<List<Map<String, Object>>>(){});
            // Use a unique stock code per test run to avoid accumulating data across test methods
            stockcode = ((String) rows.get(0).get("stockCode")) + "-" + System.nanoTime();
            marketId = (String) rows.get(0).get("marketId");
            java.util.List<PricePoint> list = new java.util.ArrayList<>();
            for (Map<String, Object> m : rows) {
                PricePoint p = new PricePoint();
                long ts = toEpoch((String)m.get("date"), (String)m.get("time"));
                p.setTs(ts);
                BigDecimal bd = new BigDecimal(String.valueOf(((Number)m.get("price")).doubleValue()));
                p.setOpen(bd); p.setHigh(bd); p.setLow(bd); p.setClose(bd);
                p.setVol(1L);
                list.add(p);
            }
            dao.insertBatch(stockcode, marketId, list);
        }
        // Preload name cache for requested stock
        nameCache.setName("300033", "33", "wuhan city", 3600);
    }

    @Test
    public void testFixedPortWithKafkaData_printAndValidate() throws Exception {
        String url = String.format("http://localhost:18080/kline?stockcode=%s&marketId=%s", stockcode, marketId);
        ResponseEntity<Map> resp = rest.getForEntity(url, Map.class);
        Assertions.assertEquals(200, resp.getStatusCodeValue());
        Map body = resp.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("0", body.get("code"));
        Assertions.assertTrue(body.containsKey("data"));
        Assertions.assertTrue(body.containsKey("list"));
        // Print response for manual inspection in surefire report
        System.out.println("/kline response: " + new ObjectMapper().writeValueAsString(body));

        List list = (List) body.get("list");
        Assertions.assertEquals(20, list.size());
        Map first = (Map) list.get(0);
        Map last = (Map) list.get(list.size()-1);
        Assertions.assertEquals("20200101", first.get("date"));
        Assertions.assertEquals("0940", first.get("time"));
        Assertions.assertEquals("20200101", last.get("date"));
        Assertions.assertEquals("0959", last.get("time"));
    }

    @Test
    public void testBadRequestWhenMissingRequiredParams() {
        String urlMissingMarket = String.format("http://localhost:18080/kline?stockcode=%s", stockcode);
        ResponseEntity<Map> resp1 = rest.getForEntity(urlMissingMarket, Map.class);
        Assertions.assertEquals(400, resp1.getStatusCodeValue());
        Map b1 = resp1.getBody();
        Assertions.assertNotNull(b1);
        Assertions.assertNotEquals("0", b1.get("code"));

        String urlMissingStock = "http://localhost:18080/kline?marketId=SZ";
        ResponseEntity<Map> resp2 = rest.getForEntity(urlMissingStock, Map.class);
        Assertions.assertEquals(400, resp2.getStatusCodeValue());
        Map b2 = resp2.getBody();
        Assertions.assertNotNull(b2);
        Assertions.assertNotEquals("0", b2.get("code"));
    }

    @Test
    public void testBadRequestOnInvalidLimitType() {
        String url = String.format("http://localhost:18080/kline?stockcode=%s&marketId=%s&limit=abc", stockcode, marketId);
        ResponseEntity<Map> resp = rest.getForEntity(url, Map.class);
        Assertions.assertEquals(400, resp.getStatusCodeValue());
        Map b = resp.getBody();
        Assertions.assertNotNull(b);
        Assertions.assertNotEquals("0", b.get("code"));
    }

    private long toEpoch(String yyyymmdd, String hhmm) {
        int y=Integer.parseInt(yyyymmdd.substring(0,4));
        int m=Integer.parseInt(yyyymmdd.substring(4,6));
        int d=Integer.parseInt(yyyymmdd.substring(6,8));
        int H=Integer.parseInt(hhmm.substring(0,2));
        int M=Integer.parseInt(hhmm.substring(2,4));
        return LocalDateTime.of(y,m,d,H,M).toEpochSecond(ZoneOffset.UTC);
    }

    @Test
    public void testNameCachedAfterFirstHttpCall() {
        // Use a unique stock code to ensure cache miss
        String sc = "T" + System.nanoTime();
        String mk = "SZ";
        java.util.List<PricePoint> list = new java.util.ArrayList<>();
        PricePoint p1 = new PricePoint(); p1.setTs(1L); p1.setOpen(BigDecimal.ONE); p1.setHigh(BigDecimal.ONE); p1.setLow(BigDecimal.ONE); p1.setClose(BigDecimal.ONE); p1.setVol(1L);
        PricePoint p2 = new PricePoint(); p2.setTs(2L); p2.setOpen(BigDecimal.ONE); p2.setHigh(BigDecimal.ONE); p2.setLow(BigDecimal.ONE); p2.setClose(BigDecimal.ONE); p2.setVol(1L);
        list.add(p1); list.add(p2);
        dao.insertBatch(sc, mk, list);

        // Call HTTP once, which should resolve name via service and write into cache
        String url = String.format("http://localhost:18080/kline?stockcode=%s&marketId=%s", sc, mk);
        ResponseEntity<Map> resp = rest.getForEntity(url, Map.class);
        Assertions.assertEquals(200, resp.getStatusCodeValue());
        // Verify cache contains the resolved name
        String cached = nameCache.getName(sc, mk);
        Assertions.assertEquals("NAME-" + sc + '-' + mk, cached);
    }
}
