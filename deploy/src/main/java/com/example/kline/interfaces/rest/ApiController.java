package com.example.kline.interfaces.rest;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import com.example.kline.modules.kline.domain.service.NameResolver;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * K-line query API.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@RestController
@RequestMapping("/kline")
public class ApiController {
    private final KlineRepository klineRepository;
    private final NameResolver nameResolver;

    @Autowired
    public ApiController(KlineRepository klineRepository, NameResolver nameResolver) {
        this.klineRepository = klineRepository;
        this.nameResolver = nameResolver;
    }

    /**
     * Query k-line data.
     *
     * @param stockcode stock code
     * @param marketId  market id
     * @param startTs   start timestamp
     * @param endTs     end timestamp
     * @param limit     limit
     * @return response
     */
    @GetMapping
    public Map<String, Object> getKline(@RequestParam String stockcode,
                                        @RequestParam String marketId,
                                        @RequestParam(required = false) Long startTs,
                                        @RequestParam(required = false) Long endTs,
                                        @RequestParam(required = false) Integer limit) {
        KlineResponse response = klineRepository.findRange(stockcode, marketId, startTs, endTs, limit);
        String stockName = nameResolver.resolve(stockcode, marketId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", "0");
        resp.put("message", "success");
        resp.put("data", new LinkedHashMap<String, Object>() {{ put("stockName", stockName); }});

        List<Map<String, Object>> list = response.getData().stream()
            .sorted(Comparator.comparingLong(PricePoint::getTs))
            .map(ApiController::toItem)
            .collect(Collectors.toList());
        resp.put("list", list);
        return resp;
    }

    private static Map<String, Object> toItem(PricePoint p) {
        Map<String, Object> m = new LinkedHashMap<>();
        Instant instant = Instant.ofEpochSecond(p.getTs() == null ? 0L : p.getTs());
        m.put("date", DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(instant));
        m.put("time", DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC).format(instant));
        // Include OHLC and vol for completeness; contract requires ordering by date,time
        m.put("open", p.getOpen());
        m.put("high", p.getHigh());
        m.put("low", p.getLow());
        m.put("close", p.getClose());
        m.put("vol", p.getVol());
        return m;
    }
}
