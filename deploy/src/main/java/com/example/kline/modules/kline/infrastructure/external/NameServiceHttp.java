package com.example.kline.modules.kline.infrastructure.external;

import org.springframework.stereotype.Component;

/**
 * Simulated external name service.
 *
 * @author wangzilong2@myhexin.com
 * @date 2025-06-18 22:30:00
 */
@Component
public class NameServiceHttp {
    public String fetchName(String stockcode, String marketId) {
        // simple stub implementation
        return "NAME-" + stockcode + "-" + marketId;
    }
}
