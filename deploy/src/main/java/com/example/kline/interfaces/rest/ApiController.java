package com.example.kline.interfaces.rest;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.repository.KlineRepository;
import com.example.kline.modules.kline.domain.service.NameResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * K-line query API.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@RestController
@RequestMapping("/kline")
@RequiredArgsConstructor
public class ApiController {
    private final KlineRepository klineRepository;
    private final NameResolver nameResolver;

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
    public KlineResponse getKline(@RequestParam String stockcode,
                                  @RequestParam String marketId,
                                  @RequestParam(required = false) Long startTs,
                                  @RequestParam(required = false) Long endTs,
                                  @RequestParam(required = false) Integer limit) {
        KlineResponse response = klineRepository.findRange(stockcode, marketId, startTs, endTs, limit);
        response.setStockName(nameResolver.resolve(stockcode, marketId));
        return response;
    }
}
