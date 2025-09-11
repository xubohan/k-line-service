package com.example.kline.interfaces.ingest;

import com.example.kline.modules.kline.infrastructure.cache.TimelineRedisWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Optional startup ingestor: loads a timeline JSON array file into Redis ZSET.
 * Enable with property: app.ingest.file=/absolute/or/relative/path.json
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-09 22:30:00
 */
@Component
@ConditionalOnProperty(name = "app.ingest.file")
public class TimelineFileIngestor implements org.springframework.boot.CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(TimelineFileIngestor.class);
    private static final ObjectMapper M = new ObjectMapper();
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HHmm");

    @Autowired private org.springframework.core.env.Environment env;
    @Autowired private TimelineRedisWriter writer;

    @Override
    public void run(String... args) throws Exception {
        String path = env.getProperty("app.ingest.file");
        if (path == null || path.trim().isEmpty()) return;
        File f = new File(path);
        if (!f.exists()) {
            log.warn("Ingest file not found: {}", path);
            return;
        }
        byte[] bytes = Files.readAllBytes(f.toPath());
        JsonNode arr = M.readTree(bytes);
        if (!arr.isArray()) {
            log.warn("Ingest file is not a JSON array: {}", path);
            return;
        }
        int ok = 0, skip = 0;
        for (JsonNode n : arr) {
            try {
                String sc = text(n, "stockCode");
                String mk = text(n, "marketId");
                String date = text(n, "date");
                String time = text(n, "time");
                java.math.BigDecimal price = n.hasNonNull("price") ? n.get("price").decimalValue() : null;
                if (isBlank(sc) || isBlank(mk) || isBlank(date) || isBlank(time) || price == null) { skip++; continue; }
                long ts = toEpoch(date, time);
                writer.write(sc, mk, ts, price);
                ok++;
            } catch (Exception e) {
                skip++;
            }
        }
        log.info("Ingested timeline from {}: ok={}, skip={}", path, ok, skip);
    }

    private static String text(JsonNode n, String f) { JsonNode v = n.get(f); return (v==null||v.isNull())?null:v.asText(); }
    private static boolean isBlank(String s){ return s==null||s.trim().isEmpty(); }
    private static long toEpoch(String date, String time){
        LocalDate d = LocalDate.parse(date, DF); LocalTime t = LocalTime.parse(time, TF);
        return d.atTime(t).toInstant(ZoneOffset.UTC).getEpochSecond();
    }
}

