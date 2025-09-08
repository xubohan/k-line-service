package com.example.kline.modules.kline.domain.service;

/**
 * Resolve stock name by code and market.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
public interface NameResolver {
    /**
     * Resolve stock name.
     *
     * @param stockcode stock code
     * @param marketId  market identifier
     * @return stock name or null
     */
    String resolve(String stockcode, String marketId);
}
