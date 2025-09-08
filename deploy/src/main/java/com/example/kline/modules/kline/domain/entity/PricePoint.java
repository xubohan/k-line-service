package com.example.kline.modules.kline.domain.entity;

import java.math.BigDecimal;
import lombok.Data;

/**
 * Price point of stock k-line.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@Data
public class PricePoint {
    private Long ts;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long vol;

    /**
     * Validate price point.
     *
     * @return true when fields are non-null.
     */
    public boolean isValid() {
        return ts != null && open != null && high != null && low != null && close != null && vol != null;
    }
}
