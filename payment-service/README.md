# payment-service

`payment-service` is a database-free simulated payment gateway called by
`order-service` during checkout.

## Endpoints

The API listens on port `8082`; Actuator has its own port, `8083`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/payments` | Simulate processing a payment |
| `GET` | `/actuator/health` | Read health on the management port |

`POST /payments` requires a non-null `orderId`, a non-blank `customerId`, and
an `amount` of at least `0.01`. It returns an `APPROVED` or `DECLINED` result
when processing succeeds. A simulated gateway failure returns HTTP 503.

## Configuration

The simulation settings are in `src/main/resources/application.yaml`:

| Property | Default | Effect |
|---|---:|---|
| `payment.simulation.min-delay-ms` | `50` | Minimum simulated gateway delay |
| `payment.simulation.max-delay-ms` | `200` | Maximum simulated gateway delay |
| `payment.simulation.failure-rate` | `0.2` | Probability of a simulated HTTP 503 |
| `payment.simulation.decline-above` | `5000` | Larger amounts receive a normal `DECLINED` result |

Change these YAML values and restart the service to apply them. There are no
database settings or application environment variables.

## Run

Use Java 21 and run these commands from this folder:

```powershell
.\mvnw.cmd spring-boot:run
```

```bash
./mvnw spring-boot:run
```

Example request (PowerShell):

```powershell
curl.exe -i -X POST http://localhost:8082/payments -H "Content-Type: application/json" -d "{\"orderId\":1,\"customerId\":\"c1\",\"amount\":49.99}"
```

Example request (bash):

```bash
curl -i -X POST http://localhost:8082/payments -H "Content-Type: application/json" -d '{"orderId":1,"customerId":"c1","amount":49.99}'
```

Check health at `http://localhost:8083/actuator/health`. Health and info are
the only Actuator web endpoints exposed.

## Observability hooks

The service pushes metrics, traces, and logs over OTLP to the Collector at
`http://localhost:4318`. A `payment.process` observation creates a span and
timer; Spring instrumentation creates HTTP spans and propagates incoming trace
context. The Logback OpenTelemetry appender forwards log records to the SDK.

⚠️ This service is configured for local learning, not production. The simulated
failure rate is intentionally nonzero: occasional HTTP 503 results are expected,
and the payment-service Postman collection accepts them. A decline above the
configured amount is a business result, not a gateway error.
