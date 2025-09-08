# Plan

## Step 1: Initialize project structure
- [x] Create Maven parent project with modules `api` and `deploy`.
- [x] Add basic dependencies and build plugins.
- [ ] **Checkpoint:** `mvn -q -pl deploy -am test` succeeds (network issue).

## Step 2: Implement domain layer
- [x] Create `PricePoint` and `KlineResponse` entities.
- [x] Define `NameResolver` service and `KlineRepository` interface.
- [x] **Checkpoint:** compilation succeeds.

## Step 3: Implement infrastructure layer
- [x] Add in-memory caches `RedisNameCache` and `RedisKlineCache`.
- [x] Implement `NameServiceHttp`, `NameResolverImpl`, `KlineDao`, `KlineRepositoryImpl`.
- [ ] **Checkpoint:** compilation and simple test (waiting for dependency resolution).

## Step 4: Implement interfaces layer
- [x] Provide REST `ApiController` and Kafka `TimelineConsumer` skeleton.
- [ ] **Checkpoint:** endpoint compiles; unit tests pass.

## Step 5: Add unit tests
- [x] Test `KlineResponse.getDataInRange`.
- [ ] **Checkpoint:** tests pass.

