package com.example.kline.e2e;

import com.example.kline.interfaces.consumer.TimelineConsumer;
import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import com.example.kline.modules.kline.infrastructure.cache.RedisKlineCache;
import com.example.kline.modules.kline.infrastructure.cache.RedisNameCache;
import com.example.kline.modules.kline.infrastructure.db.dao.KlineDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

/**
 * Kafka integration end-to-end test.
 * Tests complete data flow from JSON ingestion to API response.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
public class KafkaE2eTest {

    @Autowired private KlineRepository repo;
    @Autowired private KlineDao dao;
    @Autowired private RedisKlineCache cache;
    @Autowired private RedisNameCache nameCache;
    @Autowired private TestRestTemplate http;
    @LocalServerPort private int port;

    @Test
    public void ingestKafkaJson_persistAndQuery_ok() throws Exception {
        // 1) load kafka_data.json from classpath
        ObjectMapper om = new ObjectMapper();
        List<Map<String, Object>> rows;
        try (InputStream is = getClass().getResourceAsStream("/kafka_data.json")) {
            rows = om.readValue(is, new TypeReference<List<Map<String, Object>>>(){});
        }
        Assertions.assertFalse(rows.isEmpty());

        String stock = (String) rows.get(0).get("stockCode");
        String market = (String) rows.get(0).get("marketId");

        // 2) build response and simulate consumer
        KlineResponse r = new KlineResponse();
        r.setStockcode(stock);
        r.setMarketId(market);
        for (Map<String, Object> m : rows) {
            PricePoint p = new PricePoint();
            p.setTs(toEpoch((String)m.get("date"), (String)m.get("time")));
            BigDecimal bd = new BigDecimal(String.valueOf(((Number)m.get("price")).doubleValue()));
            p.setOpen(bd); p.setHigh(bd); p.setLow(bd); p.setClose(bd); p.setVol(1L);
            r.addPricePoint(p);
        }
        nameCache.setName("300033", "33", "wuhan city", 3600);
        new TimelineConsumer(repo).run(r);

        // 3) verify dao + cache
        Assertions.assertEquals(rows.size(), dao.selectRange(stock, market, null, null, null).size());
        Assertions.assertEquals(rows.size(), cache.getRange(stock, market, null, null, null).getData().size());

        // 4) verify API contract
        String url = String.format("http://localhost:%d/kline?stockcode=%s&marketId=%s", port, stock, market);
        ResponseEntity<Map> resp = http.getForEntity(url, Map.class);
        Assertions.assertEquals(200, resp.getStatusCodeValue());
        Map body = resp.getBody();
        Assertions.assertEquals("0", body.get("code"));
        Map data = (Map) body.get("data");
        if ("300033".equals(stock) && "33".equals(market)) {
            Assertions.assertEquals("wuhan city", data.get("stockName"));
        }
        List list = (List) body.get("list");
        Assertions.assertEquals(rows.size(), list.size());
        Map first = (Map) list.get(0);
        Map last = (Map) list.get(list.size()-1);
        Assertions.assertEquals("20200101", first.get("date"));
        Assertions.assertEquals("0940", first.get("time"));
        Assertions.assertEquals("20200101", last.get("date"));
        Assertions.assertEquals("0959", last.get("time"));
    }

    private static long toEpoch(String yyyymmdd, String hhmm) {
        int y=Integer.parseInt(yyyymmdd.substring(0,4));
        int m=Integer.parseInt(yyyymmdd.substring(4,6));
        int d=Integer.parseInt(yyyymmdd.substring(6,8));
        int H=Integer.parseInt(hhmm.substring(0,2));
        int M=Integer.parseInt(hhmm.substring(2,4));
        return LocalDateTime.of(y,m,d,H,M).toEpochSecond(ZoneOffset.UTC);
    }
}
