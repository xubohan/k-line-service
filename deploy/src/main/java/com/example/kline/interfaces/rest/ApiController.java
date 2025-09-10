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
        // Basic parameter validation to align with API restrictions
        if (stockcode == null || stockcode.trim().isEmpty()) {
            throw new IllegalArgumentException("stockcode must not be blank");
        }
        if (marketId == null || marketId.trim().isEmpty()) {
            throw new IllegalArgumentException("marketId must not be blank");
        }
        // Length limits: prevent abuse and enforce black-box expectations
        final int MAX_STOCKCODE_LEN = 64;
        final int MAX_MARKETID_LEN = 16;
        if (stockcode.length() > MAX_STOCKCODE_LEN) {
            throw new IllegalArgumentException("stockcode too long");
        }
        if (marketId.length() > MAX_MARKETID_LEN) {
            throw new IllegalArgumentException("marketId too long");
        }
        // Limit must be non-negative when provided
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        // Clamp excessive limits to protect resources (DoS prevention)
        final int MAX_LIMIT = 1000;
        Integer effectiveLimit = limit == null ? null : Math.min(limit, MAX_LIMIT);

        KlineResponse response = klineRepository.findRange(stockcode, marketId, startTs, endTs, effectiveLimit);
        if (response == null) {
            response = new KlineResponse();
        }
        String stockName = nameResolver.resolve(stockcode, marketId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", "0");
        resp.put("message", "success");
        
        List<Map<String, Object>> list = response.getData().stream()
            .sorted(Comparator.comparingLong(p -> p.getTs() == null ? 0L : p.getTs()))
            .map(p -> toTimelineItem(p, stockcode, marketId))
            .collect(Collectors.toList());
            
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stockName", stockName);
        data.put("list", list);
        resp.put("data", data);
        
        return resp;
    }

    /**
     * Convert PricePoint to timeline message format.
     * Returns timeline format: {stockCode, marketId, price, date, time}
     */
    private static Map<String, Object> toTimelineItem(PricePoint p, String stockcode, String marketId) {
        Map<String, Object> m = new LinkedHashMap<>();
        Instant instant = Instant.ofEpochSecond(p.getTs() == null ? 0L : p.getTs());
        m.put("stockCode", stockcode);
        m.put("marketId", marketId);
        // Use close price as the representative price for timeline format
        m.put("price", p.getClose());
        m.put("date", DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(instant));
        m.put("time", DateTimeFormatter.ofPattern("HHmm").withZone(ZoneOffset.UTC).format(instant));
        return m;
    }
}
