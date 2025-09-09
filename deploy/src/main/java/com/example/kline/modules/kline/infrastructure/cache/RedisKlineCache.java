package com.example.kline.modules.kline.infrastructure.cache;

import com.example.kline.modules.kline.domain.entity.KlineResponse;
import com.example.kline.modules.kline.domain.entity.PricePoint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Simulated Redis cache for k-line data.
 *
 * @author xubohan@myhexin.com
 * @date 2025-09-08 20:24:08
 */
@Component
public class RedisKlineCache {
    private final Map<String, List<PricePoint>> store = new ConcurrentHashMap<>();
    private static final ObjectMapper M = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    private final boolean externalEnabled;
    private final String redisHost;
    private final int redisPort;
    private volatile JedisPool pool; // lazy init

    public RedisKlineCache() {
        this.externalEnabled = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        this.redisHost = getProp("spring.redis.host", "127.0.0.1");
        this.redisPort = Integer.parseInt(getProp("spring.redis.port", "6379"));
    }

    @Autowired
    public RedisKlineCache(Environment env) {
        boolean fromSpring = env.getProperty("app.redis.external", Boolean.class, false);
        boolean fromSys = Boolean.parseBoolean(getProp("app.redis.external", "false"));
        this.externalEnabled = fromSpring || fromSys;
        this.redisHost = coalesce(env.getProperty("spring.redis.host"), getProp("spring.redis.host", "127.0.0.1"));
        this.redisPort = Integer.parseInt(coalesce(env.getProperty("spring.redis.port"), getProp("spring.redis.port", "6379")));
    }

    public KlineResponse getRange(String stockcode, String marketId, Long startTs, Long endTs, Integer limit) {
        if (stockcode == null || stockcode.trim().isEmpty() || marketId == null || marketId.trim().isEmpty()) {
            return new KlineResponse();
        }
        if (limit != null && limit < 0) {
            // invalid limit -> return empty response by convention
            return new KlineResponse();
        }
        List<PricePoint> list;
        if (externalEnabled) {
            // Prefer ZSET storage per contract; fallback to string value if not present
            list = loadFromZSet(stockcode, marketId, startTs, endTs, limit);
            if (list == null || list.isEmpty()) {
                list = loadFromRedisString(stockcode, marketId);
            }
        } else {
            list = store.get(key(stockcode, marketId));
        }
        if (list == null) {
            return new KlineResponse();
        }
        List<PricePoint> range = list.stream()
            .filter(p -> p != null && p.getTs() != null)
            .filter(p -> (startTs == null || p.getTs() >= startTs)
                && (endTs == null || p.getTs() <= endTs))
            .sorted((a, b) -> Long.compare(a.getTs(), b.getTs()))
            .collect(Collectors.toList());
        if (limit != null && range.size() > limit) {
            range = range.subList(0, limit);
        }
        KlineResponse resp = new KlineResponse();
        resp.setStockcode(stockcode);
        resp.setMarketId(marketId);
        range.forEach(resp::addPricePoint);
        return resp;
    }

    public void putBatch(KlineResponse response, long ttlSec) {
        if (externalEnabled) {
            // Optional: write to Redis as array of simple points (ts, open, high, low, close, vol)
            String k = redisDataKey(response.getStockcode(), response.getMarketId());
            try (Jedis j = jedis().getResource()) {
                String json = M.writeValueAsString(response.getData());
                if (ttlSec > 0 && ttlSec < Integer.MAX_VALUE) {
                    j.setex(k, (int) ttlSec, json);
                } else {
                    j.set(k, json);
                }
                return;
            } catch (Exception ignore) {
                // fall through to in-memory
            }
        }
        store.put(key(response.getStockcode(), response.getMarketId()), new ArrayList<>(response.getData()));
    }

    private String key(String stockcode, String marketId) {
        return stockcode + ":" + marketId;
    }

    private String redisDataKey(String stockcode, String marketId) {
        return "kline:" + key(stockcode, marketId);
    }

    private String redisZSetKey(String stockcode, String marketId) {
        // per contract: kline:1m:{marketId}:{stockCode}
        return "kline:1m:" + marketId + ":" + stockcode;
    }

    private JedisPool jedis() {
        JedisPool p = pool;
        if (p == null) {
            synchronized (this) {
                if (pool == null) {
                    pool = new JedisPool(new JedisPoolConfig(), redisHost, redisPort);
                }
                p = pool;
            }
        }
        return p;
    }

    private List<PricePoint> loadFromRedisString(String stockcode, String marketId) {
        String k = redisDataKey(stockcode, marketId);
        try (Jedis j = jedis().getResource()) {
            String val = j.get(k);
            if (val == null) return null;
            return parsePoints(val);
        } catch (Exception e) {
            return null;
        }
    }

    private List<PricePoint> loadFromZSet(String stockcode, String marketId, Long startTs, Long endTs, Integer limit) {
        String k = redisZSetKey(stockcode, marketId);
        try (Jedis j = jedis().getResource()) {
            double min = (startTs == null) ? Double.NEGATIVE_INFINITY : (double) (startTs / 60L);
            double max = (endTs == null) ? Double.POSITIVE_INFINITY : (double) (endTs / 60L);
            java.util.Set<redis.clients.jedis.Tuple> tuples;
            if (limit != null && limit >= 0) {
                tuples = j.zrangeByScoreWithScores(k, min, max, 0, limit);
            } else {
                tuples = j.zrangeByScoreWithScores(k, min, max);
            }
            if (tuples == null || tuples.isEmpty()) return new ArrayList<>();
            List<PricePoint> out = new ArrayList<>(tuples.size());
            for (redis.clients.jedis.Tuple t : tuples) {
                long ts = (long) t.getScore() * 60L;
                String priceStr = t.getElement();
                PricePoint p = new PricePoint();
                p.setTs(ts);
                java.math.BigDecimal price = null;
                try { price = new java.math.BigDecimal(priceStr); } catch (Exception ignore) {}
                p.setOpen(price); p.setHigh(price); p.setLow(price); p.setClose(price); p.setVol(0L);
                out.add(p);
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private List<PricePoint> parsePoints(String json) throws Exception {
        String s = json.trim();
        if (!s.startsWith("[")) {
            return null;
        }
        // First, try to parse as PricePoint array directly; only accept when any ts is non-null
        try {
            List<PricePoint> pts = M.readValue(s, new TypeReference<List<PricePoint>>() {});
            if (pts != null && pts.stream().anyMatch(p -> p != null && p.getTs() != null)) {
                return pts;
            }
        } catch (Exception ignore) {
            // fall through to timeline schema
        }
        // Then, parse as timeline messages with schema: stockCode, marketId, price, date, time
        List<PricePoint> out = new ArrayList<>();
        JsonNode arr = M.readTree(s);
        for (JsonNode node : arr) {
            String date = text(node, "date");
            String time = text(node, "time");
            if (date == null || time == null) continue;
            long ts = toEpochSeconds(date, time);
            BigDecimal price = node.hasNonNull("price") ? node.get("price").decimalValue() : null;
            PricePoint p = new PricePoint();
            p.setTs(ts);
            p.setOpen(price);
            p.setHigh(price);
            p.setLow(price);
            p.setClose(price);
            p.setVol(0L);
            out.add(p);
        }
        return out;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private static long toEpochSeconds(String date, String time) {
        LocalDate d = LocalDate.parse(date, DATE_FMT);
        LocalTime t = LocalTime.parse(time, TIME_FMT);
        return d.atTime(t).toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    private static String getProp(String key, String def) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(key.replace('.', '_').toUpperCase());
        return v != null ? v : def;
    }

    private static String coalesce(String a, String b) { return a != null ? a : b; }
}
