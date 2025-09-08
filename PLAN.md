# Plan

## Step 1: Initialize project structure
- [x] Create Maven parent project with modules `api` and `deploy`.
- [x] Add basic dependencies and build plugins.
- [x] **Checkpoint:** `mvn -q -pl deploy -am test` succeeds.

## Step 2: Implement domain layer
- [x] Create `PricePoint` and `KlineResponse` entities.
- [x] Define `NameResolver` service and `KlineRepository` interface.
- [x] **Checkpoint:** compilation succeeds.

## Step 3: Implement infrastructure layer
- [x] Add in-memory caches `RedisNameCache` and `RedisKlineCache`.
- [x] Implement `NameServiceHttp`, `NameResolverImpl`, `KlineDao`, `KlineRepositoryImpl`.
- [x] **Checkpoint:** compilation and simple test.

## Step 4: Implement interfaces layer
- [x] Provide REST `ApiController` and Kafka `TimelineConsumer` skeleton.
- [x] **Checkpoint:** endpoint compiles; unit tests pass.

## Step 5: Add unit tests
- [x] Test `KlineResponse.getDataInRange`.
- [x] **Checkpoint:** tests pass.
