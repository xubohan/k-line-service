package com.example.kline.modules.kline.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * Aggregate root representing k-line data response.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@Data
public class KlineResponse {
    private String stockcode;
    private String marketId;
    private String stockName;
    private final List<PricePoint> data = new ArrayList<>();

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
