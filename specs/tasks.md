# EDOTS Ordered Implementation Tasks (specs/tasks.md)

This document breaks down the implementation of EDOTS into structured, sequential, dependency-mapped tasks.

---

## Phase 3.1 — Project Skeleton & Infrastructure
- [x] **TASK-001**: Root & Multi-Module Gradle Setup
  - **Description**: Configure root `build.gradle.kts` and `settings.gradle.kts` with Java 21 toolchain, Spring Boot 3.4 plugin, and 6 modules: `edots-domain`, `edots-event-engine`, `edots-api`, `edots-notification`, `edots-webhook`, `edots-scheduler`.
  - **Dependencies**: None
  - **Acceptance Criteria**: Multi-module project builds cleanly via `./gradlew projects`.
  - **Complexity**: M

- [x] **TASK-002**: Database Migrations with Flyway
  - **Description**: Write `V1__init_schema.sql` defining 10 tables, foreign keys, and indexes. Write `V2__seed_data.sql` with default admin, agents, carrier, rules, and sample orders.
  - **Dependencies**: TASK-001
  - **Acceptance Criteria**: Flyway applies schema and seed data cleanly on startup.
  - **Complexity**: M

- [x] **TASK-003**: Docker Compose & Application Configuration
  - **Description**: Create `docker-compose.yml` (PostgreSQL 16, Kafka KRaft, backend, frontend) and `application.yml` profiles (`dev`, `test`, `docker`).
  - **Dependencies**: TASK-002
  - **Acceptance Criteria**: Configuration binds to environment variables; profiles load correct properties.
  - **Complexity**: S

---

## Phase 3.2 — Domain & State Machine (US001)
- [x] **TASK-004**: Stage Enum & State Machine Engine (`edots-domain`)
  - **Description**: Define `Stage` enum (8 stages), immutable `Actor` and `OrderEvent` records, sealed transitions, and `OrderStateMachine` with strict transition map. Throw `InvalidStateTransitionException` with machine-readable reason code on illegal transition.
  - **Dependencies**: TASK-001
  - **Acceptance Criteria**: 100% test coverage for all 8 valid transitions and all invalid transitions.
  - **Complexity**: M

- [x] **TASK-005**: Domain Entities & JPA Repositories
  - **Description**: Implement JPA entities for `Order`, `OrderEventEntity`, `DeliveryAgent`, `AdminUser`, `NotificationRule`, `NotificationLog`, `OtpToken`, `AuditLog`, `CarrierConfig`, and `AdminConfig`.
  - **Dependencies**: TASK-002, TASK-004
  - **Acceptance Criteria**: Repositories support lookup by reference, stage, agent, and stale timestamps.
  - **Complexity**: M

---

## Phase 3.3 — Event Engine & Audit Log (US001 + US008)
- [x] **TASK-006**: Kafka Configuration & Order Event Producer (`edots-event-engine`)
  - **Description**: Implement Kafka producer config, `OrderEventPublisher` publishing to `edots.order.events`, `edots.notification.trigger`, and `edots.reschedule.trigger`.
  - **Dependencies**: TASK-004, TASK-005
  - **Acceptance Criteria**: State machine commits state change and publishes event to Kafka topic.
  - **Complexity**: M

- [x] **TASK-007**: Audit Log Consumer & Retention Job (`edots-event-engine` & `edots-scheduler`)
  - **Description**: Implement `AuditLogConsumer` listening on `edots.audit.log` inserting immutable rows into `audit_log`. Implement `@Scheduled` `AuditLogRetentionJob` deleting records > 90 days.
  - **Dependencies**: TASK-005, TASK-006
  - **Acceptance Criteria**: Audit events written immutably; retention job purges expired records.
  - **Complexity**: M

---

## Phase 3.4 — Agent Interface & OTP (US002) + Frontend Setup (Phase 3.9)
- [x] **TASK-008**: Spring Security & JWT Authentication (`edots-api`)
  - **Description**: Implement JWT token provider, filters, and authentication endpoints (`POST /api/auth/login`, `POST /api/auth/refresh`) supporting `ROLE_AGENT` and `ROLE_ADMIN`.
  - **Dependencies**: TASK-005
  - **Acceptance Criteria**: Valid credentials yield signed JWT; protected endpoints enforce roles.
  - **Complexity**: M

- [x] **TASK-009**: Delivery Agent API & OTP Verification (`edots-api`)
  - **Description**: Implement `AgentOrderController` (`GET /api/agent/orders`, `POST /api/agent/orders/{id}/transition`, `POST /api/agent/orders/{id}/otp/generate`, `POST /api/agent/orders/{id}/otp/verify`). Validate OTP before allowing `DELIVERED`. Require reason code for `FAILED_ATTEMPT`.
  - **Dependencies**: TASK-004, TASK-008
  - **Acceptance Criteria**: OTP generation, expiry check (10 min), and verification tests pass.
  - **Complexity**: M

- [x] **TASK-010**: Frontend Core Setup & Agent Dashboard (`frontend`)
  - **Description**: Scaffold React 19 + Vite + Tailwind CSS project with React Router, Zustand, Axios JWT interceptors, and build `AgentDashboard.tsx` (mobile-first, stage update dropdown, OTP modal, exception reason selector).
  - **Dependencies**: TASK-009
  - **Acceptance Criteria**: Agent dashboard loads assigned orders, transitions stage, and verifies OTP.
  - **Complexity**: L

---

## Phase 3.5 — Customer Tracking & Notifications (US003 + US004)
- [x] **TASK-011**: Public Customer Tracking API (`edots-api`)
  - **Description**: Implement `CustomerTrackingController` (`GET /api/tracking/{orderReference}`, `GET /api/tracking/{orderReference}/events`) with zero authentication and index-optimized query.
  - **Dependencies**: TASK-005
  - **Acceptance Criteria**: Lookup returns order status and timeline; query responds in < 3s.
  - **Complexity**: S

- [x] **TASK-012**: Notification Engine (`edots-notification`)
  - **Description**: Implement Kafka consumer on `edots.notification.trigger`, dynamic rule evaluation from `notification_rules`, simulated channel delivery (`EMAIL`, `SMS`, `IN_APP`), and logging to `notification_log`.
  - **Dependencies**: TASK-005, TASK-006
  - **Acceptance Criteria**: Notifications triggered for all stages and logged to `notification_log`.
  - **Complexity**: M

- [x] **TASK-013**: Customer Tracking UI (`frontend/src/pages/TrackingPage.tsx`)
  - **Description**: Build public tracking page with order ID search form, 8-stage progress bar, chronological event timeline with icons, assigned agent details, and deep-link `/track/:orderReference` support.
  - **Dependencies**: TASK-010, TASK-011
  - **Acceptance Criteria**: Tracking renders complete history and agent details in < 3s.
  - **Complexity**: M

---

## Phase 3.6 — Carrier Webhooks (US005)
- [x] **TASK-014**: Carrier Inbound Webhook API (`edots-webhook`)
  - **Description**: Implement `CarrierWebhookController` (`POST /api/webhooks/carrier/{carrierId}`), `X-Api-Key` authentication against `carrier_config.api_key_hash`, payload validation, stage mapping, and event publishing with `actor_type = CARRIER`.
  - **Dependencies**: TASK-004, TASK-005, TASK-006
  - **Acceptance Criteria**: Valid webhook transitions order and records carrier attribution; invalid key returns 401; bad payload returns 400.
  - **Complexity**: M

---

## Phase 3.7 — Auto-Rescheduling (US006)
- [x] **TASK-015**: Auto-Reschedule Engine (`edots-event-engine`)
  - **Description**: Implement consumer on `edots.reschedule.trigger`. Check `attempt_count < max_attempts`. If true, set next delivery slot (next business day 10:00 AM), transition order to `OUT_FOR_DELIVERY`, notify customer. If false, transition to `RETURNED` with reason `MAX_ATTEMPTS_EXCEEDED`.
  - **Dependencies**: TASK-004, TASK-006, TASK-012
  - **Acceptance Criteria**: Reschedules up to max attempts then triggers return; attributed to `SYSTEM`.
  - **Complexity**: M

---

## Phase 3.8 — Admin Dashboard & WebSockets (US007)
- [x] **TASK-016**: Admin Order Management & Notification Rules API (`edots-api`)
  - **Description**: Implement `AdminOrderController` (filterable paginated orders, stale order detection > 48h, dashboard summary stats) and `AdminNotificationRuleController` (CRUD for rules).
  - **Dependencies**: TASK-005, TASK-008
  - **Acceptance Criteria**: Filter queries, stale detection, and rule updates take immediate effect.
  - **Complexity**: M

- [x] **TASK-017**: STOMP WebSocket Live Updates (`edots-api`)
  - **Description**: Configure Spring WebSocket and STOMP message broker (`/ws/dashboard`, topic `/topic/dashboard-events`). Broadcast order stage updates to all connected admin clients.
  - **Dependencies**: TASK-006, TASK-008
  - **Acceptance Criteria**: Stage transition broadcasts event over WebSocket to subscribed clients.
  - **Complexity**: M

- [x] **TASK-018**: Admin Dashboard UI (`frontend/src/pages/AdminDashboard.tsx`)
  - **Description**: Build live order queue with search/filter/pagination, 48h stale order alerts, notification rule management modal, KPI cards, Recharts visualizations, and real-time STOMP integration.
  - **Dependencies**: TASK-010, TASK-016, TASK-017
  - **Acceptance Criteria**: Order updates appear live over WebSocket without browser reload.
  - **Complexity**: L

---

## Phase 3.10 — Integration, Polish & Documentation
- [x] **TASK-019**: Seed Data & OpenAPI Swagger UI
  - **Description**: Verify Flyway migration seed data and configure Springdoc OpenAPI with JWT bearer auth.
  - **Dependencies**: TASK-002, TASK-008
  - **Acceptance Criteria**: Swagger UI accessible at `/swagger-ui.html` with interactive API docs.
  - **Complexity**: S

- [x] **TASK-020**: End-to-End Verification, Dockerfile & Documentation
  - **Description**: Multi-stage Dockerfile for backend, Nginx frontend config, comprehensive `README.md` with Mermaid diagrams, credentials, and full test suite execution.
  - **Dependencies**: TASK-001 through TASK-019
  - **Acceptance Criteria**: All automated tests pass; stack starts cleanly; README covers all setup steps.
  - **Complexity**: M
