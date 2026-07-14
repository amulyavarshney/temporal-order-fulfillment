# Temporal Order Fulfillment Platform

Multi-module Java 21 platform that orchestrates e-commerce order fulfillment with **Temporal**, a **Spring Boot REST API**, **JPA persistence**, saga compensations, and human approval for high-value orders.

## Architecture

```
Client → order-api (REST) → Temporal → order-worker
                              ↓
                     Payment / Inventory / Delivery services
                              ↓
                         PostgreSQL (or H2 locally)
```

| Module | Purpose |
|--------|---------|
| `order-common` | Models, DTOs, exceptions, service interfaces, Temporal config |
| `order-services` | JPA entities + Payment / Inventory / Delivery implementations |
| `order-workflows` | Temporal workflow + activities (approval, saga, queries) |
| `order-worker` | Spring Boot worker process |
| `order-api` | REST API + workflow client |

### Workflow

1. Validate order
2. If total > `$10,000` (configurable), wait for `approve` / `reject` signal
3. Process payment
4. Reserve inventory (decrements stock)
5. Deliver order
6. On failure after payment/inventory: saga compensations (`refundPayment`, `releaseInventory`)

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for Temporal + optional full stack)

## Quick start (Docker)

```bash
make up
make demo
make demo-approve
```

- API: http://localhost:8080
- Worker health: http://localhost:8081/actuator/health
- Temporal UI: http://localhost:8233
- Prometheus metrics: `/actuator/prometheus` on API and worker

## Local development

```bash
# Terminal A – Temporal
docker run --rm -p 7233:7233 -p 8233:8233 --name temporal-server temporalio/auto-setup:1.25.2

# Terminal B – Worker
make worker

# Terminal C – API
make api

# Submit an order
make demo
```

Local API/worker share a file-based H2 database under `.data/` (auto-server mode). For Docker Compose, both use PostgreSQL.

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/orders` | Start fulfillment workflow |
| `GET` | `/api/orders/{orderId}` | Query workflow status |
| `POST` | `/api/orders/{orderId}/approve` | Approve high-value order |
| `POST` | `/api/orders/{orderId}/reject?reason=` | Reject high-value order |
| `GET` | `/api/inventory` | List stock levels |

### Example request

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "order": {
      "items": [
        {"itemName": "Pima Cotton T-Shirt", "itemPrice": 49.99, "quantity": 2}
      ],
      "payment": {
        "creditCard": {"number": "4111111111111111", "expiration": "12/28"}
      }
    }
  }'
```

Use credit card expiration `12/23` to simulate a non-retryable payment failure.

Item names ending in `@@@` are treated as invalid inventory (demo failure path) — they will not match stock rows and trigger compensation after payment.

## Configuration (environment)

| Variable | Default | Description |
|----------|---------|-------------|
| `TEMPORAL_ADDRESS` | `localhost:7233` | Temporal gRPC target |
| `TEMPORAL_NAMESPACE` | `default` | Temporal namespace |
| `TEMPORAL_TASK_QUEUE` | `OrderFulfillTaskQueue` | Task queue name |
| `APPROVAL_THRESHOLD` | `10000` | Orders above this require approval |
| `SPRING_DATASOURCE_URL` | file H2 under `.data/` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `sa` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | _(empty)_ | DB password |
| `SERVER_PORT` | `8080` | API port |
| `WORKER_PORT` | `8081` | Worker actuator port |

## Extensibility

- Swap backends by providing alternate Spring `@Service` beans for `PaymentService`, `InventoryService`, or `DeliveryService`.
- Activities depend only on interfaces from `order-common`.
- Workflow returns structured `OrderFulfillmentResult` and exposes `getStatus` queries / approval signals.

## Tests & CI

```bash
make test
```

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` on Java 21 with JaCoCo reports.

## Build artifacts

```bash
make package
java -jar order-api/target/order-api-1.0.0.jar
java -jar order-worker/target/order-worker-1.0.0.jar
```

## License

MIT
