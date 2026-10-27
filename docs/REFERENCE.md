# Repository reference

Facts below describe the checked-in configuration and source. Runtime behavior
is verified by reading configuration only unless explicitly identified as
author-confirmed.

## 1. Port and URL map

| Version | App and Actuator | Collector input and output | Backends and UI | Connection direction | Sources |
|---|---|---|---|---|---|
| v1-metrics | Order API `:8080`; Actuator `:8081` | None | Prometheus `:9090`; Grafana `:3000` | Prometheus pulls `http://host.docker.internal:8081/actuator/prometheus`; Grafana queries Prometheus | `v1-metrics/prometheus.yml`; `order-service/src/main/resources/application.yaml` at `eb50be7` |
| v2-opentelemetry | Order API `:8080` / Actuator `:8081`; Payment API `:8082` / Actuator `:8083` | OTLP gRPC `:4317`, HTTP `:4318`; Prometheus exporter `:8889` | Prometheus `:9090`; Grafana `:3000` | Apps push OTLP; Prometheus pulls `http://otel-collector:8889/metrics`; Grafana queries Prometheus; Collector traces go to debug output | `v2-opentelemetry/docker-compose.yml`; `v2-opentelemetry/otel-collector-config.yaml`; `v2-opentelemetry/prometheus.yml`; `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| v3-tempo | Same current app ports as v2 | OTLP `:4317` / `:4318`; metrics exporter `:8889` | Prometheus `:9090`; Tempo API `:3200`; Grafana `:3000` | Apps push OTLP; Prometheus pulls `:8889/metrics`; Collector pushes traces to internal `tempo:4317`; Grafana queries Prometheus and Tempo | `v3-tempo/docker-compose.yml`; `v3-tempo/otel-collector-config.yaml`; `v3-tempo/prometheus.yml`; `v3-tempo/tempo.yaml`; `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| v4-loki | Same current app ports as v2 | OTLP `:4317` / `:4318`; metrics exporter `:8889` | Prometheus `:9090`; Tempo `:3200`; Loki `:3100`; Grafana `:3000` | Apps push OTLP; Prometheus pulls `:8889/metrics`; Collector pushes traces to `tempo:4317` and logs to `http://loki:3100/otlp`; Grafana queries all three backends | `v4-loki/docker-compose.yml`; `v4-loki/otel-collector-config.yaml`; `v4-loki/prometheus.yml`; `v4-loki/tempo.yaml`; `v4-loki/loki.yaml`; `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| v5-opensearch | Same current app ports as v2 | OTLP `:4317` / `:4318`; metrics exporter `:8889` | Prometheus `:9090`; Tempo `:3200`; OpenSearch `:9200`; OpenSearch Dashboards `:5601`; Grafana `:3000` | Apps push OTLP; Prometheus pulls `:8889/metrics`; Collector pushes traces to `tempo:4317` and logs to `http://opensearch:9200`; Grafana and OpenSearch Dashboards query their configured backends | `v5-opensearch/docker-compose.yml`; `v5-opensearch/otel-collector-config.yaml`; `v5-opensearch/prometheus.yml`; `v5-opensearch/tempo.yaml`; `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |

In v3-v5, Tempo listens internally on OTLP ports `4317` and `4318`, but only
its HTTP API on `3200` is published to the host. v1's historical Actuator path
is sourced from the `eb50be7` app configuration; the current app configuration
does not expose `/actuator/prometheus`. (source: `v3-tempo/docker-compose.yml`,
`v4-loki/docker-compose.yml`, `v5-opensearch/docker-compose.yml`,
`order-service/src/main/resources/application.yaml` at `eb50be7`,
`order-service/src/main/resources/application.yaml`)

## 2. Credentials and defaults

| Item | Default or behavior | Source |
|---|---|---|
| Order-service database user | `DB_USERNAME`, default `root` | `order-service/src/main/resources/application.yaml` |
| Order-service database password | `DB_PASSWORD`, default `rootPassword` | `order-service/src/main/resources/application.yaml` |
| Database | `orders_db` at `localhost:3306` | `order-service/src/main/resources/application.yaml` |
| Grafana login | User `admin`, password `admin` in v1-v5 Compose files | `v1-metrics/docker-compose.yml`; `v2-opentelemetry/docker-compose.yml`; `v3-tempo/docker-compose.yml`; `v4-loki/docker-compose.yml`; `v5-opensearch/docker-compose.yml` |
| OpenSearch authentication | Security plugin disabled; HTTP endpoint has no configured login | `v5-opensearch/docker-compose.yml` |
| Loki authentication | `auth_enabled: false`; no Loki tenant authentication | `v4-loki/loki.yaml` |
| Prometheus and Tempo authentication | No authentication is configured by these local stack files | `v1-metrics/docker-compose.yml`; `v3-tempo/docker-compose.yml` |
| Payment simulation | `min-delay-ms: 50`, `max-delay-ms: 200`, `failure-rate: 0.2`, `decline-above: 5000` | `payment-service/src/main/resources/application.yaml` |

These are local-learning defaults, not production credentials or deployment
guidance. (source: `order-service/src/main/resources/application.yaml`,
`payment-service/src/main/resources/application.yaml`,
`v1-metrics/docker-compose.yml`, `v2-opentelemetry/docker-compose.yml`,
`v3-tempo/docker-compose.yml`, `v4-loki/docker-compose.yml`,
`v5-opensearch/docker-compose.yml`, `v4-loki/loki.yaml`)

## 3. HTTP API

| Service | Method and path | Request | Response and statuses | Validation and behavior | Source |
|---|---|---|---|---|---|
| order-service | `POST /orders` | `customerId`, `productId`, `quantity`, `totalAmount` | `201 Created`, `OrderResponse`, and a `Location` path | Nonblank IDs; quantity at least 1; amount at least `0.01` | `order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`; `order-service/src/main/java/com/yasirkhan/os/dtos/CreateOrderRequest.java` |
| order-service | `POST /orders/checkout` | Same as `POST /orders` | `201 Created` with an order response; payment unavailability returns `503` | Creates the order, calls payment, and marks it confirmed or failed; business decline is an order result, not a payment-service 503 | `order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`; `order-service/src/main/java/com/yasirkhan/os/services/OrderWorkflowService.java`; `order-service/src/main/java/com/yasirkhan/os/handlers/GlobalExceptionHandler.java` |
| order-service | `GET /orders/{id}` | Path variable `id` | `200 OK`; `404` if no order exists | Returns one order response | `order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`; `order-service/src/main/java/com/yasirkhan/os/services/OrderService.java`; `order-service/src/main/java/com/yasirkhan/os/handlers/GlobalExceptionHandler.java` |
| order-service | `GET /orders` | Optional `status`, plus pageable parameters | `200 OK` with a list of order responses | Default page size 20, sorted by `createdAt` descending; `status` is `OrderStatus` | `order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`; `order-service/src/main/java/com/yasirkhan/os/services/OrderService.java` |
| order-service | `PATCH /orders/{id}/confirm` | No body | `200 OK`; `404` missing; `409` if not pending | Only pending orders can change state | `order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`; `order-service/src/main/java/com/yasirkhan/os/services/OrderService.java`; `order-service/src/main/java/com/yasirkhan/os/handlers/GlobalExceptionHandler.java` |
| order-service | `PATCH /orders/{id}/fail` | Nonblank `reason` | `200 OK`; `400` invalid body; `404` missing; `409` if not pending | Only pending orders can change state | `order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`; `order-service/src/main/java/com/yasirkhan/os/dtos/FailOrderRequest.java`; `order-service/src/main/java/com/yasirkhan/os/handlers/GlobalExceptionHandler.java` |
| payment-service | `POST /payments` | Non-null `orderId`, nonblank `customerId`, amount at least `0.01` | `200 OK` with `PaymentResponse`; `400` invalid body; simulated gateway failure returns `503` | An amount over `decline-above` returns a normal `DECLINED` response; failure-rate errors are technical failures | `payment-service/src/main/java/com/yasirkhan/ps/controllers/PaymentController.java`; `payment-service/src/main/java/com/yasirkhan/ps/dtos/PaymentRequest.java`; `payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java`; `payment-service/src/main/java/com/yasirkhan/ps/handlers/GlobalExceptionHandler.java` |

`OrderResponse` contains `id`, customer/product IDs, quantity, total amount,
status, failure reason, and created/updated timestamps. The standard order
validation error response includes message, numeric status, timestamp, and path;
payment validation uses Spring's error handling. (source:
`order-service/src/main/java/com/yasirkhan/os/dtos/OrderResponse.java`,
`order-service/src/main/java/com/yasirkhan/os/dtos/ErrorResponse.java`,
`order-service/src/main/java/com/yasirkhan/os/handlers/GlobalExceptionHandler.java`,
`payment-service/src/main/java/com/yasirkhan/ps/handlers/GlobalExceptionHandler.java`)

PowerShell:

```powershell
curl.exe -i -X POST http://localhost:8080/orders/checkout -H "Content-Type: application/json" -d '{"customerId":"c1","productId":"p1","quantity":1,"totalAmount":49.99}'
curl.exe -i -X POST http://localhost:8082/payments -H "Content-Type: application/json" -d '{"orderId":1,"customerId":"c1","amount":49.99}'
```

bash:

```bash
curl -i -X POST http://localhost:8080/orders/checkout -H 'Content-Type: application/json' -d '{"customerId":"c1","productId":"p1","quantity":1,"totalAmount":49.99}'
curl -i -X POST http://localhost:8082/payments -H 'Content-Type: application/json' -d '{"orderId":1,"customerId":"c1","amount":49.99}'
```

The API ports and request shapes are defined by the controller mappings, DTOs,
and each service's `application.yaml`. (source:
`order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`,
`order-service/src/main/java/com/yasirkhan/os/dtos/CreateOrderRequest.java`,
`order-service/src/main/resources/application.yaml`,
`payment-service/src/main/java/com/yasirkhan/ps/controllers/PaymentController.java`,
`payment-service/src/main/java/com/yasirkhan/ps/dtos/PaymentRequest.java`,
`payment-service/src/main/resources/application.yaml`)

## 4. Actuator endpoints

| Service | Management port | Exposed endpoints | Access and probe configuration | Source |
|---|---:|---|---|---|
| order-service | `8081` | `/actuator/health`, `/actuator/info`, `/actuator/metrics`; health probes `/actuator/health/liveness` and `/actuator/health/readiness` | Default endpoint access is `none`; only listed endpoints are exposed. Health, info, and metrics are `read-only`. Liveness includes `livenessState`; readiness includes `readinessState,db`. | `order-service/src/main/resources/application.yaml` |
| payment-service | `8083` | `/actuator/health`, `/actuator/info`; health probes `/actuator/health/liveness` and `/actuator/health/readiness` | Default endpoint access is `none`; only health and info are exposed. Health and info are `read-only`; probes are enabled. | `payment-service/src/main/resources/application.yaml` |

Both use `/actuator` as the web base path. The current order-service configuration
does not expose `/actuator/prometheus`; that endpoint belongs to the historical
v1 app state. (source: `order-service/src/main/resources/application.yaml`,
`payment-service/src/main/resources/application.yaml`,
`order-service/src/main/resources/application.yaml` at `eb50be7`)

## 5. Metric catalog

| Name in code/config | Type, tags, and unit | Where and what it measures | Expected Prometheus name |
|---|---|---|---|
| `orders.created` | Counter; `status=success` or `status=failed`; unit is event count | `OrderMetrics` in order-service; number of completed orders by outcome | ⚠️ Not verified; metrics are exported over OTLP and may be renamed |
| `orders.processing` | Timer with percentile histogram; tag `status=successTEst`; records `Duration` in seconds per configured base time unit | `OrderMetrics` in order-service; elapsed time from order creation until confirm/fail | ⚠️ Not verified; OTLP-to-Prometheus translation is not specified |
| `orders.pending` | Gauge; no tags; order count | `OrderMetrics` in order-service; current pending-order count | ⚠️ Not verified; OTLP-to-Prometheus translation is not specified |
| `order.save` | Observation-generated timer; low-cardinality `operation=insert`; duration in seconds per configured base time unit | `OrderService.create`; time spent saving a new order | ⚠️ Not verified |
| `payment.process` | Observation-generated timer; low-cardinality `gateway=simulated`; high-cardinality `order.id`; duration in seconds per configured base time unit | `PaymentService.process`; time spent in the simulated payment workflow | ⚠️ Not verified |
| `http.server.requests` | Automatic server request timer; Spring/Micrometer tags; seconds; histogram enabled with SLO boundaries `100ms`, `300ms`, `500ms`, `1s` | Both services' application configs; request latency and counts | ⚠️ Not verified after OTLP translation |

Custom metric registrations and tags are in `OrderMetrics`; the observations
are in `OrderService` and `PaymentService`. The histogram bounds for the
custom timer are configured in code (minimum `50ms`, maximum `10m`); the
automatic HTTP server histogram and SLO bounds are in YAML. (source:
`order-service/src/main/java/com/yasirkhan/os/metrics/OrderMetrics.java`,
`order-service/src/main/java/com/yasirkhan/os/services/OrderService.java`,
`payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java`,
`order-service/src/main/resources/application.yaml`,
`payment-service/src/main/resources/application.yaml`)

## 6. Trace and span catalog

| Span or trace source | Kind and attributes | Cardinality | Where created and source |
|---|---|---|---|
| Incoming HTTP request | Server span from Spring instrumentation; exact exported span name and framework attributes ⚠️ Not verified | No app-defined keys; exact framework low/high-cardinality keys ⚠️ Not verified | Web/Actuator dependencies and controllers; `order-service/pom.xml`; `payment-service/pom.xml`; `order-service/src/main/java/com/yasirkhan/os/controllers/OrderController.java`; `payment-service/src/main/java/com/yasirkhan/ps/controllers/PaymentController.java` |
| Outgoing order-to-payment call | Client span from the injected Spring `RestClient.Builder`; `traceparent` is propagated; exact exported name/attributes ⚠️ Not verified | No app-defined keys; framework dimensions ⚠️ Not verified | `order-service/src/main/java/com/yasirkhan/os/configs/RestClientConfig.java`; `order-service/src/main/java/com/yasirkhan/os/clients/PaymentClient.java` |
| `order.save` | Manual Observation and span around `repo.save`; kind is not explicitly set in code | Low cardinality: `operation=insert`; no high-cardinality key added | `order-service/src/main/java/com/yasirkhan/os/services/OrderService.java` |
| `payment.process` | Manual Observation and child span around the simulated payment workflow; kind is not explicitly set in code | Low cardinality: `gateway=simulated`; high cardinality: `order.id` | `payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java` |
| Scheduled pending-order refresh | `@Scheduled(fixedDelay = 5000)` method exists; an active span is not established by the code | No span attributes are added in the method | `order-service/src/main/java/com/yasirkhan/os/metrics/PendingOrdersJob.java`; `order-service/src/main/java/com/yasirkhan/os/OrderServiceApplication.java` |

Both apps configure sampling probability `1.0` and resource attributes
`service.name` (the Spring application name) and
`deployment.environment=local`. In a checkout, the incoming order HTTP server
span is the parent context for the order workflow; the injected client builder
propagates W3C `traceparent` to payment-service, whose HTTP server span
continues that trace. The explicit `order.save` and `payment.process`
observations add work spans. The app code does not define custom span IDs.
(source: `order-service/src/main/resources/application.yaml`,
`payment-service/src/main/resources/application.yaml`,
`order-service/src/main/java/com/yasirkhan/os/configs/RestClientConfig.java`,
`order-service/src/main/java/com/yasirkhan/os/services/OrderWorkflowService.java`,
`payment-service/src/main/java/com/yasirkhan/ps/controllers/PaymentController.java`,
`order-service/src/main/java/com/yasirkhan/os/services/OrderService.java`,
`payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java`)

`PendingOrdersJob` is not a Spring component in the current source, and
`@EnableScheduling` is commented out, so the scheduled refresh is not active
as checked in. (source:
`order-service/src/main/java/com/yasirkhan/os/metrics/PendingOrdersJob.java`,
`order-service/src/main/java/com/yasirkhan/os/OrderServiceApplication.java`)

The v2 Collector sends traces to its debug exporter; v3-v5 send them to Tempo.
v1 uses the historical Micrometer Prometheus setup and has no configured trace
pipeline. (source: `v2-opentelemetry/otel-collector-config.yaml`,
`v3-tempo/otel-collector-config.yaml`, `v4-loki/otel-collector-config.yaml`,
`v5-opensearch/otel-collector-config.yaml`, `v1-metrics/docker-compose.yml`)

## 7. Log pipeline

| Stage or backend | Configuration and behavior | Source |
|---|---|---|
| Logback | Both services include `src/main/resources/logback-spring.xml`; the root logger writes to console and an `OpenTelemetryAppender` | `order-service/src/main/resources/logback-spring.xml`; `payment-service/src/main/resources/logback-spring.xml` |
| Appender dependency | `io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0`, version `2.28.0-alpha` | `order-service/pom.xml`; `payment-service/pom.xml` |
| SDK installation | `InstallOpenTelemetryAppender` receives Spring's `OpenTelemetry` bean and calls `OpenTelemetryAppender.install` after properties are set. Logback starts first, so startup logs before installation remain console-only. | `order-service/src/main/java/com/yasirkhan/os/configs/InstallOpenTelemetryAppender.java`; `payment-service/src/main/java/com/yasirkhan/ps/configs/InstallOpenTelemetryAppender.java` |
| OTLP export | Both apps set `management.opentelemetry.logging.export.otlp.endpoint` to `http://localhost:4318/v1/logs` | `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| v4 Loki storage | Collector `otlphttp/loki` exporter sends to Loki's `/otlp` base path; schema uses TSDB, schema v13, and enables structured metadata. Loki's config comments identify `trace_id` and `span_id` as structured metadata; Grafana maps `service.name` to `service_name`. | `v4-loki/otel-collector-config.yaml`; `v4-loki/loki.yaml`; `v4-loki/grafana/provisioning/datasources/tempo-datasource.yml` |
| v5 OpenSearch storage | Collector uses index prefix `otel-logs` and daily format `yyyy.MM.dd`, with mapping mode `ss4o`. Grafana expects `@timestamp`, `body`, and `severity.text.keyword`. | `v5-opensearch/otel-collector-config.yaml`; `v5-opensearch/grafana/provisioning/datasources/opensearch-datasource.yml` |

The configured path is Logback → OpenTelemetry appender → SDK → OTLP HTTP →
Collector → Loki (v4) or OpenSearch (v5). The appender's code comments say
trace and span identifiers accompany records associated with active spans.
Exact serialized fields beyond those declared by the backend configurations
are ⚠️ Not verified. (source: `order-service/src/main/resources/logback-spring.xml`,
`payment-service/src/main/resources/logback-spring.xml`, `order-service/pom.xml`,
`payment-service/pom.xml`, `order-service/src/main/resources/application.yaml`,
`payment-service/src/main/resources/application.yaml`,
`order-service/src/main/java/com/yasirkhan/os/configs/InstallOpenTelemetryAppender.java`,
`payment-service/src/main/java/com/yasirkhan/ps/configs/InstallOpenTelemetryAppender.java`,
`v4-loki/otel-collector-config.yaml`, `v5-opensearch/otel-collector-config.yaml`)

## 8. Collector pipelines per version

| Version | Signal | Receiver | Processor | Exporter or destination | Source |
|---|---|---|---|---|---|
| v1-metrics | Metrics | No Collector; Prometheus scrapes the app | N/A | App `/actuator/prometheus` → Prometheus | `v1-metrics/prometheus.yml` |
| v2-opentelemetry | Metrics | OTLP gRPC `:4317` and HTTP `:4318` | `batch` | Prometheus exporter `:8889` | `v2-opentelemetry/otel-collector-config.yaml` |
| v2-opentelemetry | Traces | OTLP | `batch` | `debug` exporter; no trace backend configured | `v2-opentelemetry/otel-collector-config.yaml` |
| v2-opentelemetry | Logs | No active logs pipeline | N/A | No logs exporter configured | `v2-opentelemetry/otel-collector-config.yaml` |
| v3-tempo | Metrics | OTLP gRPC and HTTP | `batch` | Prometheus exporter `:8889` | `v3-tempo/otel-collector-config.yaml` |
| v3-tempo | Traces | OTLP | `batch` | `otlp/tempo` to `tempo:4317`, plus `debug` | `v3-tempo/otel-collector-config.yaml` |
| v3-tempo | Logs | No active logs pipeline | N/A | No logs exporter configured | `v3-tempo/otel-collector-config.yaml` |
| v4-loki | Metrics | OTLP gRPC and HTTP | `batch` | Prometheus exporter `:8889` | `v4-loki/otel-collector-config.yaml` |
| v4-loki | Traces | OTLP | `batch` | `otlp/tempo` to `tempo:4317`, plus `debug` | `v4-loki/otel-collector-config.yaml` |
| v4-loki | Logs | OTLP | `batch` | `otlphttp/loki` to `http://loki:3100/otlp`, plus `debug` | `v4-loki/otel-collector-config.yaml` |
| v5-opensearch | Metrics | OTLP gRPC and HTTP | `batch` | Prometheus exporter `:8889` | `v5-opensearch/otel-collector-config.yaml` |
| v5-opensearch | Traces | OTLP | `batch` | `otlp/tempo` to `tempo:4317`, plus `debug` | `v5-opensearch/otel-collector-config.yaml` |
| v5-opensearch | Logs | OTLP | `batch` | OpenSearch exporter at `http://opensearch:9200`, index prefix `otel-logs`, plus `debug` | `v5-opensearch/otel-collector-config.yaml` |

For v2-v5, the app pushes OTLP to the Collector; Prometheus separately pulls
the Collector's Prometheus exporter. (source:
`v2-opentelemetry/otel-collector-config.yaml`,
`v2-opentelemetry/prometheus.yml`, `v3-tempo/otel-collector-config.yaml`,
`v3-tempo/prometheus.yml`, `v4-loki/otel-collector-config.yaml`,
`v4-loki/prometheus.yml`, `v5-opensearch/otel-collector-config.yaml`,
`v5-opensearch/prometheus.yml`)

## 9. Configuration glossary

| Property | Purpose; effect if removed | Source |
|---|---|---|
| `spring.application.name` | Sets app identity used in telemetry resource attributes and metric tags; removal can leave those placeholders unresolved. | `order-service/src/main/resources/application.yaml` |
| `payment.base-url` | Supplies the RestClient base URL; the configuration bean requires it to build the payment client. | `order-service/src/main/resources/application.yaml`; `order-service/src/main/java/com/yasirkhan/os/configs/RestClientConfig.java` |
| `spring.http.client.connect-timeout`, `spring.http.client.read-timeout` | Bound payment-client connection and response waits; omission uses framework defaults ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `spring.datasource.url`, `username`, `password` | Connects to MySQL; missing or mismatched connection details prevent database access. Environment variables override the username/password defaults. | `order-service/src/main/resources/application.yaml` |
| `spring.datasource.hikari.pool-name`, `maximum-pool-size`, `minimum-idle`, `connection-timeout` | Names and sizes the connection pool and limits waiting for a connection; omitted values use Hikari defaults ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `spring.jpa.hibernate.ddl-auto` | Set to `create`; Hibernate recreates the schema on startup. Removing it changes schema handling and may leave the table absent ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `spring.jpa.open-in-view`, `show-sql` | Disables Open EntityManager in View and SQL console output; omitted behavior depends on framework defaults ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `spring.jpa.properties.hibernate.format_sql`, `spring.jpa.properties.hibernate.jdbc.time_zone` | Formats SQL output and uses UTC for JDBC time handling; omission loses those settings. | `order-service/src/main/resources/application.yaml` |
| `server.port`, `server.shutdown` | Selects API port `8080` and graceful shutdown; omission changes the port or shutdown behavior to framework defaults ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `management.server.port` | Separates Actuator onto port `8081`; omission removes the separate management listener. | `order-service/src/main/resources/application.yaml` |
| `management.endpoints.access.default`, `management.endpoints.web.base-path`, `exposure.include`, `exposure.exclude` | Restricts Actuator access, sets `/actuator`, and exposes only health/info/metrics while excluding sensitive endpoints; removal changes access or paths ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `management.endpoint.health.access`, `health.probes.enabled`, `health.group.liveness.include`, `health.group.readiness.include` | Enables read-only health and liveness/readiness probes; readiness includes database health. Removal disables probe paths or changes group behavior. | `order-service/src/main/resources/application.yaml` |
| `management.endpoint.info.access`, `management.endpoint.metrics.access` | Allows read-only info and metrics; with default access `none`, removing these leaves those endpoints inaccessible. | `order-service/src/main/resources/application.yaml` |
| `management.otlp.metrics.export.url`, `step`, `base-time-unit` | Pushes metrics to Collector `/v1/metrics` every 5 seconds and uses seconds as the base time unit; missing export settings can stop or alter metrics export ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `management.opentelemetry.resource-attributes.[service.name]`, `[deployment.environment]` | Attaches service identity and `local` environment to telemetry; removal loses those resource attributes. | `order-service/src/main/resources/application.yaml` |
| `management.opentelemetry.tracing.export.otlp.endpoint`, `management.opentelemetry.logging.export.otlp.endpoint` | Sends traces and logs to Collector OTLP HTTP paths; removing each endpoint can prevent that signal from reaching the configured Collector ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `management.tracing.sampling.probability` | Samples all traces at `1.0`; omission changes sampling to a framework default ⚠️ Not verified. | `order-service/src/main/resources/application.yaml` |
| `management.metrics.tags.application` | Adds the application name as a common metric tag; omission removes this configured tag. | `order-service/src/main/resources/application.yaml` |
| `management.metrics.distribution.percentiles-histogram.[http.server.requests]`, `management.metrics.distribution.slo.[http.server.requests]` | Enables HTTP request histograms and explicit latency boundaries; removal removes these configured distributions/boundaries. | `order-service/src/main/resources/application.yaml` |
| `info.app.name`, `info.app.description` | Supplies the app name and description exposed by Actuator info; omission removes these configured info values. | `order-service/src/main/resources/application.yaml` |

| Property | Purpose; effect if removed | Source |
|---|---|---|
| `spring.application.name` | Sets payment-service telemetry identity and metric tags; it is referenced by resource attributes. | `payment-service/src/main/resources/application.yaml` |
| `server.port`, `server.shutdown` | Selects API port `8082` and graceful shutdown; removal changes those configured behaviors. | `payment-service/src/main/resources/application.yaml` |
| `payment.simulation.min-delay-ms`, `max-delay-ms`, `failure-rate`, `decline-above` | Configures delay, technical-failure probability, and business decline threshold; the properties record also supplies defaults `50`, `200`, `0.2`, and `5000`. | `payment-service/src/main/resources/application.yaml`; `payment-service/src/main/java/com/yasirkhan/ps/configs/SimulationProperties.java` |
| `management.server.port` | Separates Actuator onto port `8083`; omission removes the separate management listener. | `payment-service/src/main/resources/application.yaml` |
| `management.endpoints.access.default`, `management.endpoints.web.base-path`, `exposure.include`, `exposure.exclude` | Restricts Actuator access, sets `/actuator`, and exposes health/info only while excluding sensitive endpoints; removal changes access or paths ⚠️ Not verified. | `payment-service/src/main/resources/application.yaml` |
| `management.endpoint.health.access`, `health.probes.enabled`, `management.endpoint.info.access` | Allows read-only health/info and enables health probe paths; removal changes endpoint access or probe availability. | `payment-service/src/main/resources/application.yaml` |
| `management.otlp.metrics.export.url`, `step`, `base-time-unit` | Pushes metrics to Collector `/v1/metrics` every 5 seconds with seconds as the base time unit; removal can stop or alter metrics export ⚠️ Not verified. | `payment-service/src/main/resources/application.yaml` |
| `management.opentelemetry.resource-attributes.[service.name]`, `[deployment.environment]` | Adds payment-service identity and `local` environment to telemetry; omission loses those resource attributes. | `payment-service/src/main/resources/application.yaml` |
| `management.opentelemetry.tracing.export.otlp.endpoint`, `management.opentelemetry.logging.export.otlp.endpoint` | Sends traces and logs to the Collector; removal can prevent the corresponding signal from being exported ⚠️ Not verified. | `payment-service/src/main/resources/application.yaml` |
| `management.tracing.sampling.probability` | Samples all traces at `1.0`; omission changes sampling to a framework default ⚠️ Not verified. | `payment-service/src/main/resources/application.yaml` |
| `management.metrics.tags.application` | Adds the service name as a common metric tag; omission removes the configured tag. | `payment-service/src/main/resources/application.yaml` |
| `management.metrics.distribution.percentiles-histogram.[http.server.requests]` | Enables HTTP server request latency histograms; removal removes the configured histogram. | `payment-service/src/main/resources/application.yaml` |

## 10. Concept-to-code map

| Concept | Where it appears in this repository | Source |
|---|---|---|
| Counter | Success and failure meters register the `orders.created` Counter | `order-service/src/main/java/com/yasirkhan/os/metrics/OrderMetrics.java` |
| Gauge | `orders.pending` Gauge reads an `AtomicLong`; the refresher is not active in the checked-in source | `order-service/src/main/java/com/yasirkhan/os/metrics/OrderMetrics.java`; `order-service/src/main/java/com/yasirkhan/os/metrics/PendingOrdersJob.java`; `order-service/src/main/java/com/yasirkhan/os/OrderServiceApplication.java` |
| Timer | `orders.processing` Timer; `order.save` and `payment.process` Observations also produce timed work | `order-service/src/main/java/com/yasirkhan/os/metrics/OrderMetrics.java`; `order-service/src/main/java/com/yasirkhan/os/services/OrderService.java`; `payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java` |
| Percentile histogram | Custom processing timer publishes a percentile histogram; both app configs enable `http.server.requests` histograms, with SLO boundaries in order-service | `order-service/src/main/java/com/yasirkhan/os/metrics/OrderMetrics.java`; `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| Tags and cardinality | `orders.created` uses `status`; `payment.process` uses low-cardinality `gateway` and high-cardinality `order.id` | `order-service/src/main/java/com/yasirkhan/os/metrics/OrderMetrics.java`; `payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java` |
| Observation | `order.save` wraps persistence; `payment.process` wraps simulated payment work | `order-service/src/main/java/com/yasirkhan/os/services/OrderService.java`; `payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java` |
| Span | Automatic web spans and explicit observations are sent to OTLP | `order-service/pom.xml`; `payment-service/pom.xml`; `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| Trace and parent/child spans | Checkout calls payment-service; the RestClient client span propagates context to the payment HTTP server span, with `payment.process` as a child observation | `order-service/src/main/java/com/yasirkhan/os/services/OrderWorkflowService.java`; `order-service/src/main/java/com/yasirkhan/os/configs/RestClientConfig.java`; `payment-service/src/main/java/com/yasirkhan/ps/controllers/PaymentController.java`; `payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java` |
| `traceparent` context propagation | The injected `RestClient.Builder` is used to carry W3C trace context | `order-service/src/main/java/com/yasirkhan/os/configs/RestClientConfig.java`; `order-service/src/main/java/com/yasirkhan/os/clients/PaymentClient.java` |
| Sampling | Both services configure trace sampling probability `1.0` | `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| Resource attributes | Both services set `service.name` and `deployment.environment=local` | `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| OTLP | Apps export metrics, traces, and logs to OTLP HTTP; Collector receivers accept OTLP gRPC/HTTP in v2-v5 | `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml`; `v2-opentelemetry/otel-collector-config.yaml`; `v5-opensearch/otel-collector-config.yaml` |
| Batch processor | Collector `batch` processor is used by v2-v5 pipelines | `v2-opentelemetry/otel-collector-config.yaml`; `v3-tempo/otel-collector-config.yaml`; `v4-loki/otel-collector-config.yaml`; `v5-opensearch/otel-collector-config.yaml` |
| Pull scrape vs push | Apps push OTLP to Collector; Prometheus pulls the Collector exporter. v1 Prometheus pulls the app endpoint directly. | `v2-opentelemetry/prometheus.yml`; `v2-opentelemetry/otel-collector-config.yaml`; `v1-metrics/prometheus.yml` |
| Structured metadata | Loki v4 enables structured metadata with TSDB schema v13; its provisioning refers to `trace_id` as metadata | `v4-loki/loki.yaml`; `v4-loki/grafana/provisioning/datasources/tempo-datasource.yml` |
| Index | v5 Collector creates daily `otel-logs-<date>` indices with SS4O mapping | `v5-opensearch/otel-collector-config.yaml`; `v5-opensearch/grafana/provisioning/datasources/opensearch-datasource.yml` |
| Retention | Prometheus is configured for 7 days; Tempo is configured for 1 hour; no Loki or OpenSearch retention period is set in these configs | `v2-opentelemetry/docker-compose.yml`; `v3-tempo/tempo.yaml`; `v4-loki/tempo.yaml`; `v5-opensearch/tempo.yaml`; `v4-loki/loki.yaml`; `v5-opensearch/docker-compose.yml` |
| Health probes | Actuator health/liveness/readiness config is in both apps; stack health endpoints include Tempo `/ready`, Loki `/ready`, and OpenSearch cluster health | `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml`; `v3-tempo/docker-compose.yml`; `v4-loki/docker-compose.yml`; `v5-opensearch/docker-compose.yml` |
| Graceful shutdown | Both APIs set `server.shutdown: graceful` | `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |
| Separate management port | Order Actuator uses `8081`, payment Actuator uses `8083`, separate from API ports | `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml` |

## 11. Terms as used in this repository

- **Actuator** — Spring management endpoints exposed on the separate service ports. (source: `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml`)
- **OTLP** — The protocol the apps use to push metrics, traces, and logs to the Collector. (source: `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml`)
- **Collector** — The OTLP receiver and signal router in v2-v5. (source: `v2-opentelemetry/otel-collector-config.yaml`; `v5-opensearch/otel-collector-config.yaml`)
- **Scrape** — Prometheus pulls the Collector's `/metrics` endpoint in v2-v5; v1 pulls the historical Actuator Prometheus endpoint. (source: `v2-opentelemetry/prometheus.yml`; `v1-metrics/prometheus.yml`)
- **Observation** — Micrometer instrumentation used for `order.save` and `payment.process`. (source: `order-service/src/main/java/com/yasirkhan/os/services/OrderService.java`; `payment-service/src/main/java/com/yasirkhan/ps/services/PaymentService.java`)
- **Trace context** — The injected RestClient builder propagates `traceparent` to payment-service. (source: `order-service/src/main/java/com/yasirkhan/os/configs/RestClientConfig.java`)
- **Resource attribute** — `service.name` and `deployment.environment` identify the producing app/environment. (source: `order-service/src/main/resources/application.yaml`; `payment-service/src/main/resources/application.yaml`)
- **Structured metadata** — Loki v4 enables metadata storage for OTLP log fields such as `trace_id` and `span_id`. (source: `v4-loki/loki.yaml`; `v4-loki/grafana/provisioning/datasources/tempo-datasource.yml`)
- **SS4O** — The OpenSearch exporter mapping mode configured for v5 logs. (source: `v5-opensearch/otel-collector-config.yaml`)
- **Index** — OpenSearch's daily log index prefix is `otel-logs`. (source: `v5-opensearch/otel-collector-config.yaml`)
- **Local retention** — Tempo configures one-hour block retention; Prometheus Compose configures seven-day TSDB retention. (source: `v3-tempo/tempo.yaml`; `v2-opentelemetry/docker-compose.yml`)

Loki's link configuration searches on `service_name` and filters structured
metadata using `trace_id`; trace-to-log and log-to-trace links are configured
but have not been confirmed end to end. v5 does not configure trace/log links.
Loki's `loki.yaml` is not present in the v5 pipeline; its similarly named file
is a leftover and is not mounted by v5 Compose. (source:
`v4-loki/grafana/provisioning/datasources/loki-datasource.yml`,
`v4-loki/grafana/provisioning/datasources/tempo-datasource.yml`,
`v5-opensearch/grafana/provisioning/datasources/tempo-datasource.yml`,
`v5-opensearch/docker-compose.yml`,
`v5-opensearch/otel-collector-config.yaml`)

`PendingOrdersJob.refresh()` would set the gauge from the database every five
seconds, but the class's `@Component` and application `@EnableScheduling`
annotations are commented out; no active gauge refresh is established by this
source. (source:
`order-service/src/main/java/com/yasirkhan/os/metrics/PendingOrdersJob.java`,
`order-service/src/main/java/com/yasirkhan/os/OrderServiceApplication.java`)

Other automatically instrumented JVM, process, connection-pool, and HTTP-client
meter names and tag sets are ⚠️ Not verified from the checked-in application
configuration. Check the Collector's `:8889/metrics` output before using
Prometheus-side names. (source: `order-service/pom.xml`,
`payment-service/pom.xml`, `v2-opentelemetry/otel-collector-config.yaml`)
