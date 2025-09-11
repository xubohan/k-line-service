package com.example.kline.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Logs the actual listening port when using a random server port (0).
 */
@Component
public class ServerPortLogger implements ApplicationListener<WebServerInitializedEvent> {
    private static final Logger log = LoggerFactory.getLogger(ServerPortLogger.class);

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        log.info("Application started on port {}", port);
    }
}

