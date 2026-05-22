# Phase 3 Event-Driven Architecture - Implementation Guide

## Overview

Phase 3 extends the Phase 2 microservices with **Apache Kafka** for asynchronous event-driven communication. This guide covers running the Phase 3 implementation locally.

## Architecture Additions (Phase 3)

### Event Flow
```
auth-service
    ↓ (publishes on successful login)
    UserAuthenticatedEvent
        ↓
    Kafka: user-authenticated topic
        ↓
    core-service
        ↓ (consumes and updates read models)
    Customer session updates
```

## Prerequisites

- Java 21
- Docker & Docker Compose (includes Kafka)
- Gradle 8.13+

## Getting Started

### 1. Start Infrastructure

Start all services (Postgres, Redis, Kafka):

```bash
docker compose up -d
```

Verify all containers are running:

```bash
docker compose ps
```

You should see:
- `corebank-ms-postgres` - Port 5432
- `corebank-ms-redis` - Port 6379
- `corebank-ms-kafka` - Port 9092

**Wait ~10 seconds for Kafka to be ready** (check health):

```bash
docker compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

Expected output shows Kafka version info (no errors).

### 2. Build All Modules

```bash
./gradlew clean build -x test
```

This compiles all modules including the new Kafka adapters.

### 3. Run the Services

**Terminal 1 — auth-service (event publisher)**:
```bash
./gradlew :auth-service:bootRun
```

**Terminal 2 — core-service (event consumer)**:
```bash
./gradlew :core-service:bootRun
```

Both services should start on ports 8081 and 8082.

### 4. Test the Event Flow

#### Step A: Login (triggers event publishing)

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-CustIdentNum: 123456789" \
  -H "X-CustIdentType: CC" \
  -d '{"username":"user","password":"password"}'
```

**Watch the logs:**
- `auth-service` logs: `"Published UserAuthenticatedEvent for user: user"`
- `core-service` logs: `"Received UserAuthenticatedEvent: ..."` → Consumes event

#### Step B: Access protected endpoint (uses JWT from login)

```bash
# Extract token from login response
export TOKEN="<token-from-login-response>"

curl http://localhost:8082/api/home/balance \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-CustIdentNum: 123456789" \
  -H "X-CustIdentType: CC"
```

### 5. Monitor Events

**Check Kafka topics exist:**

```bash
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

You should see:
- `user-authenticated`
- `account-created` (auto-created by Spring Kafka)
- `balance-updated` (auto-created by Spring Kafka)

**View topic contents (for debugging):**

```bash
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-authenticated \
  --from-beginning \
  --max-messages 5
```

## Key Implementation Files

### Events (shared in `banking-commons`)
- `BaseDomainEvent.java` - Base event with metadata (eventId, aggregateId, timestamp, traceId)
- `UserAuthenticatedEvent.java` - Published by auth-service
- `AccountCreatedEvent.java` - (prepared for future)
- `BalanceUpdatedEvent.java` - (prepared for future)
- `EventPublisherPort.java` - Output port interface

### auth-service (Publisher)
- `KafkaConfig.java` - Idempotent producer configuration
- `KafkaEventPublisherAdapter.java` - Implements EventPublisherPort
- `AuthApplicationService.java` - Publishes events after successful login

### core-service (Consumer)
- `KafkaConfig.java` - Consumer configuration with manual acknowledgment
- `KafkaEventConsumerAdapter.java` - Listens on 3 topics, handles events

## Event Processing Guarantees

### Publisher (auth-service)
- **Idempotent producer**: Enabled at Kafka level
- **Unique event ID**: Each event has UUID-based eventId
- **Non-blocking**: Event publishing is async; failures don't fail the login

### Consumer (core-service)
- **Manual commit**: Acknowledged only after successful processing
- **At-least-once**: Consumers must be idempotent (use eventId deduplication)
- **Dead-letter strategy**: Configure separate DLQ topic for failed events (future enhancement)

## Configuration

### Kafka Bootstrap Servers

Both services configured via `application.yaml`:

**auth-service:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

**core-service:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: core-service-group
```

Override via environment:
```bash
export KAFKA_BOOTSTRAP_SERVERS=my-kafka-host:9092
./gradlew :auth-service:bootRun
```

## Stopping

```bash
# Stop services (Ctrl+C in each terminal)

# Stop infrastructure
docker compose down -v
```

## Kafka Configuration Details

### docker-compose.yml Setup
The configuration uses **Confluent Kafka 7.5.0 with traditional Zookeeper** (stable for local development):
- Zookeeper on port 2181 (internal only)
- Kafka broker on port 9092
- Auto-topic creation enabled
- Auto offset reset to `earliest` for new consumers

### Bootstrap Server Addresses
- **Internal (within Docker network)**: `kafka:29092`
- **External (local machine)**: `localhost:9092`

Java services running locally use `localhost:9092`.

### Topics Auto-Created on First Publish
- `user-authenticated` - Created by auth-service when publishing UserAuthenticatedEvent
- `account-created` - Ready for future use
- `balance-updated` - Ready for future use

Offset tracking: Stored in Kafka's `__consumer_offsets` topic, managed by core-service group `core-service-group`.

## Troubleshooting

### Kafka Connection Refused (9092)
```
kafka.errors.KafkaError: NoBrokersAvailable
```
**Fix**: Ensure Kafka container is running:
```bash
docker compose up -d kafka
sleep 5
```

### Topic Not Created
Topics auto-create by default. If not:
```bash
docker compose exec kafka kafka-topics \
  --create \
  --bootstrap-server localhost:9092 \
  --topic user-authenticated \
  --partitions 1 \
  --replication-factor 1
```

### Consumer Not Receiving Events
Check logs in core-service for errors. Verify:
1. Kafka is running: `docker compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092`
2. Topic exists: `docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list`
3. Consumer group created: `docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list`

## Testing Phase 3 Features

### Unit Tests (existing)
```bash
./gradlew clean build  # Runs all tests with ≥80% coverage requirement
```

### Integration Tests (examples)
Event publishing and consumption tested via:
- `auth-service` integration tests: KafkaEventPublisherAdapter tests with EmbeddedKafka
- `core-service` integration tests: KafkaEventConsumerAdapter tests with EmbeddedKafka

Examples to be added: See `/memories/session/phase3_implementation_progress.md`

## Architecture Decision Matrix

| Scenario                    | REST | Kafka |
|-----------------------------|------|-------|
| Real-time client response   | ✅   | ❌    |
| Strong consistency required | ✅   | ❌    |
| Multiple independent consumers | ❌ | ✅ |
| Event replay/audit trail    | ❌   | ✅    |
| Loose coupling desired       | ❌   | ✅    |
| Load spikes on single event  | ❌   | ✅    |

## Next: Phase 4 (Planned)

Future phases may include:
- CQRS read models for customer dashboard
- Event sourcing for audit trail
- Streaming analytics with Kafka Streams
- Multi-region event replication

---

For full Phase 3 ERD and design, see: `documentation/phase_3/phase3_ERD.md`
