# Vox

Vox is the backend for evaluating oral assessments of high school students. It runs exams and class tests,
practice sessions, AI-assisted grading, appeals, and the school subscription, quota and billing system behind them.

It is a Spring Boot 4.1 / Java 25 service that exposes REST, GraphQL and gRPC APIs, stores data in PostgreSQL,
and exchanges events with the AI grading and streaming services over Kafka and gRPC.

## What it does

- **Assessments**: blueprints, papers, schedules, candidates, sessions and turns for exams and class tests.
- **Practice**: practice planning, practice sessions and per-attempt evaluation.
- **Grading**: AI evaluation results arrive as events; teachers review and assign grading, and students can file appeals.
- **Question bank and rubrics**: questions, topics, frameworks, scoring rules, rubrics and assessment policies at system, school and teacher level.
- **Schools and users**: schools, grades, classes, rooms, registration forms, bulk import, and the roles `SYSTEM_ADMIN`, `SCHOOL_ADMIN`, `TEACHER` and `STUDENT`.
- **Billing**: subscriptions, orders, payments (PayOS and SePay), invoices, school balance and usage quota.
- **Platform**: notifications (email and Firebase push), recordings in S3, proctoring alerts, and an admin console.

## Tech stack

| Area | Technology |
|---|---|
| Runtime | Java 25, Spring Boot 4.1, Gradle (wrapper) |
| APIs | REST (springdoc OpenAPI), GraphQL (Spring for GraphQL), gRPC |
| Data | PostgreSQL 18, Flyway migrations, Spring Data JPA |
| Messaging | Kafka with a transactional outbox |
| Cache | Redis |
| Storage | S3-compatible object storage |
| Integrations | PayOS, SePay, Firebase Cloud Messaging, Google OAuth2, SMTP |
| Quality | JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit |

## Architecture

Vox uses a hexagonal layout. The dependency rules are enforced by ArchUnit
(`src/test/java/com/sep/vox/architecture/LayeredArchitectureTests.java`), so a violation fails the build.

```
interfaces (rest, graphql, grpc, kafka) --\
                                           >--> application --> domain
infrastructure (persistence, security...) -/
```

| Layer | Path under `src/main/java/com/sep/vox` | Responsibility |
|---|---|---|
| domain | `domain` | Plain-Java models, value objects, rules and repository interfaces. No Spring or JPA. |
| application | `application` | One use case per class, commands and queries, events, and the ports the app needs from the outside world. |
| infrastructure | `infrastructure` | JPA entities, repository implementations, Kafka, Redis, S3, PayOS, SePay and Firebase adapters, scheduled jobs. |
| interfaces | `interfaces` | REST and GraphQL controllers, gRPC services, Kafka consumers. |

Other pieces worth knowing:

- **Events**: outgoing events are written to an outbox table and relayed to Kafka by `OutboxEventScheduler`. Event payloads are versioned (`...PayloadV1`).
- **Background jobs**: `infrastructure/worker` holds scheduled work such as exam status transitions, deferred grading, grading deadline reminders, subscription expiry, pending order reconciliation, and quota price calibration.
- **Companion services**: Vox calls a separate `vox-streaming` service over gRPC and consumes events produced by the AI grading service.

Contributor rules (naming, layering, testing and what needs approval) are in [AGENTS.md](AGENTS.md).

## Getting started

### Prerequisites

- JDK 25 (the Gradle toolchain enforces it)
- Docker, for the local infrastructure and for the integration tests (`./gradlew test` does not need it)
- Use the `./gradlew` wrapper, not a system Gradle

### 1. Configure the environment

Copy `.env.example` to `.env` in the project root and replace the placeholders, or set the same variables in your IDE
run configuration. Never commit `.env`. The block below shows the required variables.

```env
# Database
DB_URL=localhost:5432
DB_NAME=vox
DB_USER=postgres
DB_PASSWORD=postgres

# Auth
JWT_SECRET=replace-with-a-long-secret
JWT_STREAM_SECRET=replace-with-another-long-secret
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH2_URL=http://localhost:3000
PASSWORD_SETUP_URL=http://localhost:3000/setup-password

# Admin console (the app refuses to start when these are blank)
SBA_ADMIN_USERNAME=admin
SBA_ADMIN_PASSWORD=change-me

# Mail
SMTP_USERNAME=your-email@example.com
SMTP_PASSWORD=your-smtp-password

# Initial system admin account
SYS_ADMIN_EMAIL=admin@example.com
SYS_ADMIN_PASSWORD=change-me
SYS_ADMIN_PHONE=0123456789
SYS_ADMIN_FULLNAME=System Admin
SYS_ADMIN_DATE_OF_BIRTH=1990-01-01
SYS_ADMIN_ADDRESS=Admin address

# Ports used by docker-compose.yml
APP_PORT=8081
POSTGRES_PORT=5432
REDIS_PORT=6379
KAFKA_PORT=9092
KAFKA_UI_PORT=8089
```

Every `${...}` placeholder in `src/main/resources/application.yaml` without a default value must be set, otherwise the
application will not start. The ones above are the required set. Placeholders with a default (Redis host, Kafka
bootstrap servers, CORS origins, cookie settings, quota tuning and so on) can be overridden when needed.

Optional integrations stay off until you configure them:

| Integration | Variables |
|---|---|
| Object storage | `STORAGE_PROVIDER`, `AWS_S3_*`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `RECORDING_S3_BUCKET` |
| Payments | `PAYOS_*`, `SEPAY_*` |
| Push notifications | `FIREBASE_ENABLED`, `FIREBASE_CREDENTIALS_PATH` or `FIREBASE_CREDENTIALS_JSON` |
| `vox-streaming` link | `VOX_STREAMING_GRPC_TARGET`, `GRPC_SERVICE_TOKEN` |
| Demo data | `DEMO_DATA_ENABLED`, `DEMO_DATA_PASSWORD` |

### 2. Start the infrastructure

```bash
docker compose up -d postgres redis kafka
```

`docker-compose.yml` also defines the `app` service (builds the `Dockerfile`) and `kafka-ui`. Running
`docker compose up -d` starts all of them, with the app already wired to the containers.

### 3. Run the application

```bash
./gradlew bootRun
```

Flyway applies the migrations on startup, and the initial system admin is created from the `SYS_ADMIN_*` variables.

### Local endpoints

The default HTTP port is `8081`. gRPC listens on `9090`, with health checks and reflection enabled.

| What | URL |
|---|---|
| Swagger UI | `http://localhost:8081/swagger-ui/index.html` (also `/api-doc.html`) |
| OpenAPI spec | `http://localhost:8081/v3/api-spec` |
| GraphiQL | `http://localhost:8081/graphiql` |
| REST base path | `http://localhost:8081/api/v1` |
| Admin console | `http://localhost:8081/admin` |

GraphQL schemas live in `src/main/resources/graphql/*.graphqls`. REST endpoints are protected with
`@PreAuthorize` role checks.

## Development

```bash
./gradlew compileJava                       # fastest "does it compile" check
./gradlew test                              # unit and web-slice tests, no Docker
./gradlew integrationTest                   # tests tagged "integration", needs Docker (Testcontainers, Postgres 18)
./gradlew check                             # both suites
./gradlew test --tests '*CarryQuota*'       # run a subset
./gradlew clean build --no-daemon           # exactly what CI runs
```

Notes:

- Tests run with the `Asia/Ho_Chi_Minh` time zone, and the Docker image sets `TZ` to match. Do not depend on the machine's default time zone.
- Repository and query tests use `@RepositoryTest`, and full-context tests use `@IntegrationTest` (both in `src/test/.../support`). They run against a real Postgres container from `config/TestcontainersConfiguration` and belong to the `integrationTest` suite. `application-test.yaml` turns off Kafka listeners and the fixed gRPC port, so tests never touch a running local app or broker.
- Pagination is 1-based at every boundary. Only the persistence adapters and query implementations convert to Spring Data's 0-based pages.
- Schema changes go in a new Flyway migration, `src/main/resources/db/migration/V<n>__snake_case_description.sql`. Never edit a migration that has been merged.

## Project layout

```
src/main/java/com/sep/vox/
  domain/            models, value objects, repository interfaces
  application/       use cases, commands, queries, events, ports
  infrastructure/    persistence, security, config, workers, external clients
  interfaces/        rest, graphql, grpc, kafka
src/main/proto/      gRPC definitions
src/main/resources/
  graphql/           GraphQL schemas
  db/migration/      Flyway migrations
  templates/         email templates
src/test/            unit, slice and architecture tests
```

## CI/CD

- **CI** (`.github/workflows/ci-build.yml`): runs `./gradlew clean build --no-daemon` on JDK 25 for pushes and pull requests to `default`.
- **CD** (`.github/workflows/cd.yml`): builds the Docker image, pushes it to ECR, and updates the image tag in the Helm repository.
- **Security**: CodeQL analysis runs from `.github/workflows/codeql-analysis.yml`.
