# EDOTS Project Constitution

## 1. Purpose & Vision
The **Event-Driven Order Tracking System (EDOTS)** provides a real-time, robust, and auditable order fulfilment tracking platform for EDOTS. The system tracks an order's entire lifecycle across eight fulfilment stages with strict state machine validation, an asynchronous Kafka-based event backbone, tamper-proof audit trails, and intuitive interfaces for customers, field delivery agents, and administrators.

---

## 2. Core Architectural Principles
1. **Event-Driven Backbone**: Every state change publishes a domain event to Apache Kafka (`edots.order.events`). Downstream workflows (notifications, rescheduling, audit logging, analytics) consume events asynchronously.
2. **State Machine Enforcement**: State transitions are strictly validated in the domain layer before database persistence or event publishing. Out-of-sequence transitions are rejected with typed domain exceptions and specific reason codes.
3. **Immutable Event & Audit Logs**: The `order_events` and `audit_log` tables are append-only. No `UPDATE` or `DELETE` statements are permitted. A scheduled retention worker enforces a 90-day retention policy on audit logs.
4. **Actor Attribution**: Every event attributes `actor_id` and `actor_type` (`AGENT`, `SYSTEM`, `CARRIER`, `ADMIN`). Zero anonymous events are permitted in the system.
5. **CQRS-Lite**: High-frequency read queries (such as public order tracking) read a denormalized/materialized `current_stage` directly from `orders` with optimized B-tree indexes, while the full history is assembled from `order_events`.
6. **Fail-Safe & Graceful Degradation**:
   - If notification dispatch fails, the core order lifecycle remains unblocked.
   - If carrier webhooks deliver unmappable statuses, errors are logged to the audit trail and HTTP 400 is returned without corrupting existing order state.

---

## 3. Technology Stack Standards

### Backend Stack
- **Runtime**: Java 21+ LTS (leveraging modern records, pattern matching, sealed interfaces, virtual threads).
- **Framework**: Spring Boot 3.4+ (latest stable).
- **Messaging**: Apache Kafka (KRaft mode, no ZooKeeper dependency).
- **Database**: PostgreSQL 16+ with Flyway migrations.
- **ORM & Persistence**: Spring Data JPA with Hibernate 6.
- **Security**: Spring Security 6 with stateless JWT authentication (for agents and administrators); public endpoints explicitly permitted.
- **Live Communication**: Spring WebSocket with STOMP over SockJS (`/ws/dashboard`).
- **Documentation**: Springdoc OpenAPI 3.1 (`/swagger-ui.html`).
- **Build Tool**: Gradle with Kotlin DSL (`build.gradle.kts`), structured as a multi-module project.
- **Testing**: JUnit 5, Mockito, AssertJ, and Testcontainers.

### Frontend Stack
- **Runtime & Build**: Node.js 24 LTS, Vite 6+, TypeScript 5.7+.
- **Framework**: React 19.
- **Styling**: Tailwind CSS 4, shadcn/ui component primitives, Lucide React icons.
- **State Management**: Zustand.
- **Networking**: Axios with centralized request/response interceptors for JWT token management.
- **Real-Time Client**: `@stomp/stompjs` with native WebSocket transport.
- **Routing**: React Router 7.
- **Forms & Validation**: React Hook Form with Zod schemas.
- **Charts & Feedback**: Recharts, Sonner toasts.

### Infrastructure & Deployment
- **Containerization**: Docker Compose orchestrating:
  - PostgreSQL 16
  - Kafka in KRaft mode
  - Spring Boot multi-module backend JAR
  - Nginx serving the production React frontend build and reverse-proxying `/api` and `/ws`
- **Configuration**: Strict separation of configuration via Spring profiles (`dev`, `test`, `docker`) and environment variables.

---

## 4. Coding & Design Standards
- **Immutability First**: Use Java `record`s for all DTOs, Kafka message envelopes, and event payloads.
- **Domain Modeling**: Use sealed interfaces for domain command and event hierarchies.
- **Error Handling**: Use RFC 7807 `ProblemDetail` responses for all REST error payloads with structured machine-readable error codes.
- **Naming Conventions**:
  - Kafka topics: `edots.<domain>.<event-type>`
  - Database tables: snake_case plural (`orders`, `order_events`, `delivery_agents`)
  - REST endpoints: kebab-case, resource-oriented (`/api/admin/notifications/rules`)
  - Java classes: PascalCase; methods and fields: camelCase
  - TypeScript types: PascalCase; React components: PascalCase

---

## 5. Security Model
- **Public Surface**: Customer tracking (`/api/tracking/**`) requires no authentication; queries are scoped by unique, non-sequential `order_reference`.
- **Delivery Agent Surface**: Requires JWT with role `ROLE_AGENT`. Endpoints enforce ownership: an agent can only view and transition orders assigned to them.
- **Admin Surface**: Requires JWT with role `ROLE_ADMIN`.
- **Carrier Webhooks**: Authenticated via pre-shared carrier API key in header `X-Api-Key` verified against `carrier_config.api_key_hash`.
- **Passwords**: Hashed with BCrypt (strength 12).
