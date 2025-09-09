package com.example.kline.interfaces.rest;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests exception handling for REST API errors.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void testHandleBadRequest_MissingServletRequestParameterException() {
        // Arrange
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("stockcode", "String");
        
        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);
        
        // Assert
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("400", body.get("code"));
        Assertions.assertTrue(body.get("message").toString().contains("stockcode"));
        Assertions.assertNull(body.get("data"));
        Assertions.assertTrue(body.get("list") instanceof java.util.List);
        Assertions.assertTrue(((java.util.List<?>) body.get("list")).isEmpty());
    }

    @Test
    public void testHandleBadRequest_IllegalArgumentException() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("stockcode must not be blank");
        
        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);
        
        // Assert
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("400", body.get("code"));
        Assertions.assertEquals("stockcode must not be blank", body.get("message"));
        Assertions.assertNull(body.get("data"));
        Assertions.assertTrue(body.get("list") instanceof java.util.List);
        Assertions.assertTrue(((java.util.List<?>) body.get("list")).isEmpty());
    }

    @Test
    public void testHandleBadRequest_MethodArgumentTypeMismatchException() {
        // Arrange
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "abc", Integer.class, "limit", null, null);
        
        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex);
        
        // Assert
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("400", body.get("code"));
        Assertions.assertNotNull(body.get("message"));
        Assertions.assertNull(body.get("data"));
        Assertions.assertTrue(body.get("list") instanceof java.util.List);
        Assertions.assertTrue(((java.util.List<?>) body.get("list")).isEmpty());
    }

    @Test
    public void testHandleServerError_GeneralThrowable() {
        // Arrange
        RuntimeException ex = new RuntimeException("Internal server error");
        
        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleServerError(ex);
        
        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("500", body.get("code"));
        Assertions.assertEquals("internal error", body.get("message"));
        Assertions.assertNull(body.get("data"));
        Assertions.assertTrue(body.get("list") instanceof java.util.List);
        Assertions.assertTrue(((java.util.List<?>) body.get("list")).isEmpty());
    }

    @Test
    public void testHandleServerError_NullPointerException() {
        // Arrange
        NullPointerException ex = new NullPointerException("Null value encountered");
        
        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleServerError(ex);
        
        // Assert
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals("500", body.get("code"));
        Assertions.assertEquals("internal error", body.get("message"));
        Assertions.assertNull(body.get("data"));
        Assertions.assertTrue(body.get("list") instanceof java.util.List);
        Assertions.assertTrue(((java.util.List<?>) body.get("list")).isEmpty());
    }

    @Test
    public void testResponseStructure_ConsistentFormat() {
        // Test that both error handlers return consistent response structure
        IllegalArgumentException badRequestEx = new IllegalArgumentException("Bad request");
        RuntimeException serverEx = new RuntimeException("Server error");
        
        ResponseEntity<Map<String, Object>> badResponse = handler.handleBadRequest(badRequestEx);
        ResponseEntity<Map<String, Object>> serverResponse = handler.handleServerError(serverEx);
        
        // Both should have the same structure
        Map<String, Object> badBody = badResponse.getBody();
        Map<String, Object> serverBody = serverResponse.getBody();
        
        Assertions.assertTrue(badBody.containsKey("code"));
        Assertions.assertTrue(badBody.containsKey("message"));
        Assertions.assertTrue(badBody.containsKey("data"));
        Assertions.assertTrue(badBody.containsKey("list"));
        
        Assertions.assertTrue(serverBody.containsKey("code"));
        Assertions.assertTrue(serverBody.containsKey("message"));
        Assertions.assertTrue(serverBody.containsKey("data"));
        Assertions.assertTrue(serverBody.containsKey("list"));
    }
}