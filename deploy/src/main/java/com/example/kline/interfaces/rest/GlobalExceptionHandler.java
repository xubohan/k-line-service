package com.example.kline.interfaces.rest;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for REST controllers.
 * Handles common parameter validation errors and server exceptions.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "400");
        body.put("message", ex.getMessage());
        body.put("data", null);
        body.put("list", java.util.Collections.emptyList());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleServerError(Throwable ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "500");
        body.put("message", "internal error");
        body.put("data", null);
        body.put("list", java.util.Collections.emptyList());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
