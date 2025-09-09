package com.example.kline.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests for parsing test data JSON files.
 * Validates that boundary and error data files can be parsed correctly.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
public class TestDataJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void boundaryDataJsonShouldParse() {
        assertDoesNotThrow(() -> read("/boundary_data.json"));
    }

    @Test
    void errorDataJsonShouldParse() {
        assertDoesNotThrow(() -> read("/error_data.json"));
    }

    private List<Map<String, Object>> read(String path) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return mapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
        }
    }
}
