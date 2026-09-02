# Project Description: EDOTS

EDOTS (Event-Driven Order Tracking System) is a real-time order fulfilment tracking platform for EDOTS. It models an order's lifecycle across eight fulfilment stages using an event-driven, state-machine-enforced architecture.

## Personas Served
- **Customers**: View order tracking via self-service lookup and deep-links, receive real-time stage notifications.
- **Delivery Agents**: Update order status from the field via a mobile-first web interface, log exception reasons, and confirm deliveries via OTP.
- **Administrators**: Manage the fulfilment pipeline, inspect live order queues and 48-hour stale orders, configure notification rules dynamically, and review the immutable audit log.

## Core Capabilities
- **State Machine Enforcement**: Enforces valid transitions across all 8 fulfilment stages and rejects out-of-sequence changes with reason codes.
- **Kafka-Based Event Backbone**: Real-time event publishing and consumption across domain events, notifications, reschedule triggers, carrier webhooks, and audit logs.
- **OTP Proof of Delivery**: 6-digit cryptographically-random tokens with 10-minute validity required for confirming delivery.
- **Third-Party Carrier Webhooks**: Ingests, validates, and maps external carrier events with carrier attribution.
- **Automatic Failed-Delivery Rescheduling**: Auto-reschedules failed attempts to the next business day until reaching max attempts, then transitions to returned.
- **Live Admin Dashboard with WebSocket**: STOMP over WebSocket pushing real-time order lifecycle events and live KPI statistics.
- **Immutable 90-Day Audit Log**: Append-only tamper-proof audit trail for compliance and dispute resolution, backed by a 90-day retention cleanup job.

## Technology Stack
- **Backend**: Java 21, Spring Boot 3.4+, Apache Kafka (KRaft mode), PostgreSQL 16+, Spring Data JPA, Flyway, Springdoc OpenAPI 3.1, Spring Security + JWT, Spring WebSocket + STOMP, Gradle (Kotlin DSL).
- **Frontend**: React 19, TypeScript 5.7+, Vite 6, Tailwind CSS, Zustand, Axios, React Router 7, @stomp/stompjs, Recharts, Sonner, shadcn/ui.
- **Infrastructure**: Docker Compose (KRaft Kafka, PostgreSQL 16, Spring Boot backend, Nginx frontend).
