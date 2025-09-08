package com.example.kline.modules.kline.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregate root representing k-line data response.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
public class KlineResponse {
    private String stockcode;
    private String marketId;
    private String stockName;
    private final List<PricePoint> data = new ArrayList<>();

    public String getStockcode() { return stockcode; }
    public void setStockcode(String stockcode) { this.stockcode = stockcode; }
    public String getMarketId() { return marketId; }
    public void setMarketId(String marketId) { this.marketId = marketId; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public List<PricePoint> getData() { return data; }

    /**
     * Append price point.
     *
     * @param point point to add
     */
    public void addPricePoint(PricePoint point) {
        if (point != null) {
            data.add(point);
        }
    }

    /**
     * Get points in time range.
     *
     * @param startTs start timestamp
     * @param endTs   end timestamp
     * @return list in range
     */
    public List<PricePoint> getDataInRange(Long startTs, Long endTs) {
        return data.stream()
            .filter(p -> (startTs == null || p.getTs() >= startTs)
                && (endTs == null || p.getTs() <= endTs))
            .collect(Collectors.toList());
    }

    /**
     * Validate all points.
     *
     * @return true if valid
     */
    public boolean validateData() {
        return data.stream().allMatch(PricePoint::isValid);
    }
}
