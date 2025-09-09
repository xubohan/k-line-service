# High-Risk Bugs and Vulnerabilities

- Summary: Major issues found in configuration, concurrency, input handling, and data validation that can crash the service or cause operational risk. Each item includes impact, how to reproduce, root cause, and a suggested fix.

## 1) Misconfiguration: duplicate `spring.autoconfigure.exclude` (overrides earlier excludes)
- Location: `deploy/src/main/resources/application.properties:6` and `deploy/src/main/resources/application.properties:15`
- Impact: The later property overrides the earlier one, unintentionally re-enabling Kafka and Redis auto-configuration despite comments saying they’re disabled. This can start client beans, allocate threads, or attempt connections in some environments, causing unpredictable startup behavior and resource usage differences between test and prod.
- Reproduce: Start the app with the packaged `application.properties` and inspect the auto-config report or logs; Kafka/Redis auto-config beans are active even though the file comment says they’re fully disabled.
- Root cause: The same property key is declared twice; Spring Boot uses the last occurrence.
- Fix: Merge all exclusions into a single property line (or YAML list) and remove the duplicate.

## 2) Thread-safety bug in in-memory DAO (can corrupt data / crash under load)
- Location: `deploy/src/main/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDao.java:50` and `:34`
- Impact: Concurrent writes/reads on the same key use a non-thread-safe `ArrayList`. This can cause data races, lost updates, or `ConcurrentModificationException`, crashing request handling or the Kafka consumer.
- Reproduce: In parallel, call `insertBatch("X","MK", …)` and `selectRange("X","MK", …)` repeatedly; with enough concurrency you’ll observe `ConcurrentModificationException` or inconsistent results.
- Root cause: `store` is a `ConcurrentHashMap`, but values are plain `ArrayList` mutated by `addAll` while `selectRange` streams and sorts the same list.
- Fix options:
  - Use `CopyOnWriteArrayList` or `Collections.synchronizedList(new ArrayList<>())` for per-key lists, and snapshot before streaming; or
  - Guard per-key operations with a lock; or
  - Replace with an append-only concurrent structure and copy-on-read.

## 3) Null-safety bug: sort on potentially null timestamps (NPE)
- Location: `deploy/src/main/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDao.java:34` and `deploy/src/main/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCache.java:37`
- Impact: If any `PricePoint.ts` is null (ingestion doesn’t forbid this), the comparator `Long.compare(a.getTs(), b.getTs())` will unbox null and throw `NullPointerException`, crashing list queries and the API.
- Reproduce: Insert a `PricePoint` with `ts=null` into DAO or cache, then call `selectRange`/`getRange`.
- Root cause: Unboxing `Long` without a null check.
- Fix: Use a null-safe comparator, e.g. `Comparator.comparing(p -> Optional.ofNullable(p.getTs()).orElse(0L))` or pre-filter invalid points.

## 4) Unbounded `limit` allows DoS-style large responses
- Location: `deploy/src/main/java/com/example/kline/interfaces/rest/ApiController.java:71`
- Impact: Accepts arbitrarily large `limit` (e.g., `Integer.MAX_VALUE`). With large datasets this can drive excessive CPU/memory usage for sorting, JSON building and network I/O.
- Reproduce: Seed many points for a stock, then call `/kline?limit=2147483647`.
- Root cause: No upper bound is enforced for `limit`.
- Fix: Enforce a reasonable max (e.g., 1000) and document it; clamp larger values to the max.

## 5) Kafka consumer does not validate schema; also consumes wrong shape
- Location: `deploy/src/main/java/com/example/kline/interfaces/consumer/TimelineConsumer.java:31`
- Impact: The code consumes `KlineResponse` directly and only checks `stockcode/marketId` emptiness. This violates the stated guardrails that every Kafka message must be validated against the authoritative schema (required fields: stockCode, marketId, price, date, time; formats and non-negativity). Bad or malicious messages can be stored silently, leading to inconsistent data or downstream failures (e.g., null ts crash above).
- Reproduce: Deliver a message with missing/invalid fields (e.g., null ts or negative price) and observe it being persisted.
- Root cause: No schema validation and consumer signature not aligned with the documented message contract.
- Fix: Deserialize the exact message type and validate required fields and formats; on violation, log and drop/dead-letter per policy.

## 6) Cache TTL ignored (staleness risk)
- Location: `deploy/src/main/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCache.java:49-51`
- Impact: `ttlSec` parameter is ignored, so cached data never expires in this implementation. Under changing data this can return stale results until the next write replaces the entire list.
- Reproduce: Write once with a short TTL and read after TTL; data still present.
- Root cause: In-memory stub discards TTL.
- Fix: Track expiry timestamps per key, or document the stub’s limitation and ensure production cache honors TTL.

## 7) Cache key too coarse for time-ranged queries (functional inefficiency)
- Location: `deploy/src/main/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCache.java:53` and usage in `KlineRepositoryImpl`
- Impact: The cache key is only `stockcode:marketId`. Different time ranges overwrite each other, reducing hit ratio and causing thrash. While correctness is preserved via DB fallback when cache is empty for a range, performance is degraded.
- Fix: Include range/window in the cache key or store full history per key and append on write.

# Flow Compliance Notes

- Kafka ingestion: Does not follow the documented message contract or validation rules (see item 5). This is a deviation from the UML/API restriction doc and should be fixed before enabling the consumer.
- HTTP API: Response structure and ordering match the contract; error handling returns code/message per tests. However, the unbounded `limit` conflicts with robustness expectations and should be constrained.

