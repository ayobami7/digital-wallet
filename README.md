# Digital Wallet Transaction System

A microservices-based digital wallet system demonstrating PostgreSQL transactions and Kafka event streaming.

## Services
- **Wallet Service** (port 8080): Manages wallets and transactions
- **History Service** (port 8081): Builds event-sourced audit trail

## Tech Stack
- Java 17 + Spring Boot 3.4
- PostgreSQL 15
- Apache Kafka
- Docker Compose

## Quick Start
```bash
# Start infrastructure
docker-compose up -d

# Run wallet service
cd wallet-service && ./mvnw spring-boot:run

# Run history service (new terminal)
cd history-service && ./mvnw spring-boot:run

# Check health
curl http://localhost:8080/health
curl http://localhost:8081/health
```

## Architecture
- Synchronous: Balance updates in PostgreSQL (immediate consistency)
- Asynchronous: Event history via Kafka (eventual consistency)

## Learning Goals
- PostgreSQL optimistic locking
- Kafka producer/consumer patterns
- Eventual consistency trade-offs
- Distributed transaction handling