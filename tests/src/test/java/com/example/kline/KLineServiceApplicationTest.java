package com.example.kline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * Unit tests for KLineServiceApplication.
 * Tests main application functionality.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-10 01:30:00
 */
public class KLineServiceApplicationTest {

    @Test
    public void testMainMethodExists() {
        // Test that the main method exists and can be referenced
        try {
            KLineServiceApplication.class.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            Assertions.fail("Main method should exist");
        }
    }

    @Test
    public void testApplicationClassLoads() {
        // Test that the application class can be loaded
        Assertions.assertNotNull(KLineServiceApplication.class);
        Assertions.assertEquals("KLineServiceApplication", KLineServiceApplication.class.getSimpleName());
    }
}