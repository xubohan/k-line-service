# Changes for input validation and robustness

Date: 2025-09-09

Scope: deploy module — add defensive checks for incoming parameters to align with API restrictions and ensure service does not interrupt on invalid inputs.

Modified files:

- deploy/src/main/java/com/example/kline/interfaces/rest/ApiController.java
  - (previously implemented) Validate `stockcode/marketId` non-blank and length limits; `limit` must be >= 0.

- deploy/src/main/java/com/example/kline/interfaces/rest/GlobalExceptionHandler.java
  - (previously implemented) Map `IllegalArgumentException` to 400 Bad Request.

- deploy/src/main/java/com/example/kline/modules/kline/infrastructure/db/repository/KlineRepositoryImpl.java
  - New: Guard `findRange` parameters — non-blank `stockcode/marketId`, `limit >= 0`; throw `IllegalArgumentException` on violations.
  - New: Guard `upsertBatch` — `response` non-null, and non-blank `stockcode/marketId`.

- deploy/src/main/java/com/example/kline/modules/kline/infrastructure/db/dao/KlineDao.java
  - New: `selectRange` returns empty list when `stockcode/marketId` blank; throws `IllegalArgumentException` if `limit < 0`.
  - New: `insertBatch` validates non-blank `stockcode/marketId`; treat null/empty points as no-op (return 0).

- deploy/src/main/java/com/example/kline/modules/kline/infrastructure/cache/RedisKlineCache.java
  - New: `getRange` returns empty `KlineResponse` for blank `stockcode/marketId`; throws `IllegalArgumentException` if `limit < 0`.

- deploy/src/main/java/com/example/kline/interfaces/consumer/TimelineConsumer.java
  - New: Ignore null/blank `stockcode/marketId` in incoming messages to avoid interrupting service.

Notes:
- NameResolverImpl input behavior remains unchanged to preserve existing tests requiring empty input to return a non-null name placeholder.
- Public API layer (ApiController) already enforces input validation; repository/DAO/cache now include defensive checks to protect internal entry points and batch consumers.

