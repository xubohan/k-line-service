package com.example.kline.modules.kline.infrastructure.external;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for NameServiceHttp service.
 * Tests basic functionality without reflection.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class NameServiceHttpTest {

    @Test
    public void testFetchName_BasicFunctionality() {
        // Arrange
        NameServiceHttp service = new NameServiceHttp();

        // Act & Assert - 测试基本的fallback行为
        String result1 = service.fetchName("000001", "SH");
        Assertions.assertNotNull(result1);
        Assertions.assertTrue(result1.contains("000001"));
        Assertions.assertTrue(result1.contains("SH"));

        String result2 = service.fetchName("600000", "SZ");
        Assertions.assertNotNull(result2);
        Assertions.assertTrue(result2.contains("600000"));
        Assertions.assertTrue(result2.contains("SZ"));
    }

    @Test
    public void testFetchName_NullInputs() {
        // Arrange
        NameServiceHttp service = new NameServiceHttp();

        // Act & Assert
        String result = service.fetchName(null, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("NAME-null-null", result);
    }

    @Test
    public void testFetchName_EmptyInputs() {
        // Arrange
        NameServiceHttp service = new NameServiceHttp();

        // Act & Assert
        String result = service.fetchName("", "");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("NAME--", result);
    }

    @Test
    public void testFetchName_SpecialCharacters() {
        // Arrange
        NameServiceHttp service = new NameServiceHttp();

        // Act & Assert
        String result1 = service.fetchName("@#$", "!@#");
        Assertions.assertEquals("NAME-@#$-!@#", result1);

        String result2 = service.fetchName("中文", "测试");
        Assertions.assertEquals("NAME-中文-测试", result2);
    }

    @Test
    public void testFetchName_NormalStockCodes() {
        // Arrange
        NameServiceHttp service = new NameServiceHttp();

        // Act & Assert
        Assertions.assertEquals("NAME-000001-SZ", service.fetchName("000001", "SZ"));
        Assertions.assertEquals("NAME-600000-SH", service.fetchName("600000", "SH"));
        Assertions.assertEquals("NAME-300033-SZ", service.fetchName("300033", "SZ"));
    }
}