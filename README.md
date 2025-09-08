# K-Line Service

A minimal stock K-line data service following a domain-driven design (DDD) structure. The service listens to Kafka messages for stock price updates, stores data in a database, caches recent data in Redis, and exposes an HTTP API to query minute-level K-line data for a specific stock.

## Goals
- Consume Kafka messages and persist K-line data.
- Provide an HTTP endpoint to query minute-level K-line data for a stock.
- Retrieve stock names from a dedicated name service with caching.
- Use Redis for recent data caching and a database for full data storage.

## Architecture
- **API Module:** shared interfaces and DTOs (placeholder for future RPC interfaces).
- **Deploy Module:** Spring Boot application implementing domain, infrastructure and interface layers.
- **Domain Layer:** `KlineResponse`, `PricePoint`, `NameResolver`, `KlineRepository`.
- **Infrastructure Layer:** in-memory implementations simulating Redis cache, name service, and database access.
- **Interfaces Layer:** REST controller for `/kline` query and a skeleton Kafka consumer.

## Tech Stack
- Java 8
- Spring Boot 2.3.12.RELEASE
- Maven multi-module project
- Kafka (simulated), Redis (simulated), MyBatis (simulated)
- JUnit 5 for tests

## Development Plan
See [PLAN.md](./PLAN.md) for step-by-step progress.

