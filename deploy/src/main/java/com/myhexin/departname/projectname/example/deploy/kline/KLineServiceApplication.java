package com.myhexin.departname.projectname.example.deploy.kline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application entry.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@SpringBootApplication
@EnableAsync
public class KLineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KLineServiceApplication.class, args);
    }
}
