package com.example.kline.modules.kline.infrastructure.external;

import org.springframework.stereotype.Component;

/**
 * Simulated external name service.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@Component
public class NameServiceHttp {
    public String fetchName(String stockcode, String marketId) {
        // simple stub implementation
        return "NAME-" + stockcode + "-" + marketId;
    }
}
