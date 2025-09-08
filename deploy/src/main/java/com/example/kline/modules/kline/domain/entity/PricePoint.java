package com.example.kline.modules.kline.domain.entity;

import java.math.BigDecimal;

/**
 * Price point of stock k-line.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
public class PricePoint {
    private Long ts;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long vol;

    public Long getTs() { return ts; }
    public void setTs(Long ts) { this.ts = ts; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public Long getVol() { return vol; }
    public void setVol(Long vol) { this.vol = vol; }

    /**
     * Validate price point.
     *
     * @return true when fields are non-null.
     */
    public boolean isValid() {
        return ts != null && open != null && high != null && low != null && close != null && vol != null;
    }
}
