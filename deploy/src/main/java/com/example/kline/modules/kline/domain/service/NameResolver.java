package com.example.kline.modules.kline.domain.service;

/**
 * Resolve stock name by code and market.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
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
