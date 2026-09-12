# EDOTS Cross-Artifact Consistency & Traceability Analysis (specs/analysis.md)

This analysis verifies end-to-end alignment and traceability across all Spec-Driven Development (SDD) artifacts: User Stories, Feature Specifications (`specs/spec.md`), Technical Implementation Plan (`specs/plan.md`), Acceptance Checklist (`specs/checklist.md`), and Implementation Tasks (`specs/tasks.md`).

---

## 1. Traceability Matrix

| User Story | Feature Spec Section | Plan Component | Checklist Section | Implementation Tasks | Status |
|---|---|---|---|---|---|
| **US001: 8-Stage State Machine** | Spec: US001 (8 Stages, Validation, Immutability) | Plan: §1 Architecture, §2 Kafka, §3 DB Schema (`orders`, `order_events`) | Checklist: US001 (Enumeration, Transitions, Rejection) | TASK-001, TASK-002, TASK-004, TASK-005, TASK-006 | **ALIGNED** |
| **US002: Agent Interface & OTP** | Spec: US002 (Agent Dashboard, OTP verify, Reason code) | Plan: §3 DB (`otp_tokens`), §4 Agent API, §5 Security | Checklist: US002 (Auth, SLA ≤5s, Exception, OTP modal) | TASK-008, TASK-009, TASK-010 | **ALIGNED** |
| **US003: Customer Tracking** | Spec: US003 (Public tracking, timeline, agent info, deep-link) | Plan: §4 Public REST API, §5 Public Security, Frontend Routing | Checklist: US003 (No auth, SLA <3s, Deep-links) | TASK-011, TASK-013 | **ALIGNED** |
| **US004: Event Notifications** | Spec: US004 (8 stages, channel routing, deep-link in payload) | Plan: §2 Topic `notification.trigger`, §3 DB `notification_log` & `notification_rules` | Checklist: US004 (All 8 stages, SLA ≤5m, Channel config) | TASK-006, TASK-012 | **ALIGNED** |
| **US005: Carrier Webhooks** | Spec: US005 (Webhook auth, mapping, carrier attribution) | Plan: §2 Topic `webhook.inbound`, §3 DB `carrier_config`, §4 Webhook API | Checklist: US005 (API key auth, payload validation, mapping) | TASK-014 | **ALIGNED** |
| **US006: Auto-Rescheduling** | Spec: US006 (Reschedule on fail, next slot, max attempts) | Plan: §2 Topic `reschedule.trigger`, §3 DB `admin_config`, §4 State Machine | Checklist: US006 (Attempt count, slot assignment, return trigger) | TASK-015 | **ALIGNED** |
| **US007: Admin Dashboard** | Spec: US007 (Live queue, 48h stale flag, notification CRUD, WebSocket) | Plan: §1 WebSocket hub, §4 Admin API, §5 Security | Checklist: US007 (SLA <3s, 48h stale flag, WebSocket live) | TASK-016, TASK-017, TASK-018 | **ALIGNED** |
| **US008: Immutable Audit Log** | Spec: US008 (Append-only audit, 90-day retention) | Plan: §2 Topic `audit.log`, §3 DB `audit_log`, §4 Scheduler | Checklist: US008 (All events captured, immutable, retention cron) | TASK-007, TASK-019 | **ALIGNED** |

---

## 2. Gap Identification & Resolution Report

### Item 1: Real-time Live Updates for Field Agent Dashboard
- **Check**: US002 requires customer tracking to reflect agent updates within 5 seconds. Does the customer view need polling or WebSockets?
- **Analysis**: Both the public customer tracking and admin dashboard benefit from immediate updates. The public tracking page features lightweight, responsive polling (every 3 seconds) and instant optimistic state updates, while the Admin dashboard uses STOMP WebSockets for event push.
- **Resolution**: Fully covered in TASK-013 and TASK-018.

### Item 2: Database Indexes for Query SLA Performance
- **Check**: Acceptance criteria specify < 3s page loads for Customer Tracking and Admin Queue, and 48h stale order detection.
- **Analysis**: Unindexed queries on large order datasets would violate the SLA.
- **Resolution**: Plan §3 explicitly defines B-tree indexes on `orders(order_reference)`, `orders(current_stage)`, `orders(assigned_agent_id)`, and `orders(updated_at)` for stale detection. Covered in TASK-002.

### Item 3: Deep-Link URL Resolution Across Environments
- **Check**: US003 and US004 require every notification to include a working deep-link to `/track/{orderReference}`.
- **Analysis**: The base URL must adapt dynamically depending on whether running in local development (`localhost:5173` / `localhost:8080`) or Docker container (`localhost:3000`).
- **Resolution**: Notification service reads `app.frontend.base-url` from `application.yml` (default `http://localhost:5173`) with Docker environment override. Handled in TASK-003 and TASK-012.

### Item 4: Seed Data Credential Invariants
- **Check**: The specifications require default users:
  - Admin: `admin@edots.dev` / `admin123`
  - Agents: `agent1@edots.dev` / `agent123`, `agent2@edots.dev` / `agent123`
- **Analysis**: Password hashes must be pre-generated BCrypt strings matching password `admin123` and `agent123`.
- **Resolution**: `V2__seed_data.sql` and `SeedDataLoader` will insert valid BCrypt hashes for these exact credentials. Handled in TASK-002 and TASK-019.

---

## 3. Consistency Conclusion
All 8 user stories, technical architecture components, and quality gates are 100% mapped to actionable tasks with zero gaps. The implementation phase (Phase 3) can now proceed systematically.
