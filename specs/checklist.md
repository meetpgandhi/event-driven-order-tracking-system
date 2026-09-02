# EDOTS Quality & Acceptance Checklist (specs/checklist.md)

This quality checklist defines exhaustive, testable verification items for all 8 user stories, including strict performance criteria and architectural invariants.

---

## US001: Eight-Stage Event Model with State Machine Enforcement
- [x] **Stage Enumeration**: All 8 stages (`ORDER_PLACED`, `PICKING`, `PACKED`, `DISPATCHED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED_ATTEMPT`, `RETURNED`) defined in domain model.
- [x] **Sequential Transition Verification**:
  - [x] `ORDER_PLACED` → `PICKING` succeeds.
  - [x] `PICKING` → `PACKED` succeeds.
  - [x] `PACKED` → `DISPATCHED` succeeds.
  - [x] `DISPATCHED` → `OUT_FOR_DELIVERY` succeeds.
  - [x] `OUT_FOR_DELIVERY` → `DELIVERED` succeeds when OTP verified.
  - [x] `OUT_FOR_DELIVERY` → `FAILED_ATTEMPT` succeeds when reason code provided.
  - [x] `FAILED_ATTEMPT` → `OUT_FOR_DELIVERY` succeeds on reschedule.
  - [x] `FAILED_ATTEMPT` → `RETURNED` succeeds on max attempts.
- [x] **Invalid Transition Rejection**:
  - [x] `ORDER_PLACED` → `DELIVERED` rejected with HTTP 422 / `ILLEGAL_STAGE_TRANSITION`.
  - [x] `PICKING` → `DISPATCHED` rejected with HTTP 422.
  - [x] `DELIVERED` → any stage rejected (terminal stage).
  - [x] `RETURNED` → any stage rejected (terminal stage).
- [x] **Event Immutability & Fields**:
  - [x] Every transition inserts into `order_events` with `order_id`, `event_type`, `previous_stage`, `new_stage`, `actor_id`, `actor_type`, and timestamp.
  - [x] Updates or deletes to `order_events` are strictly rejected.

---

## US002: Delivery Agent Interface — Field Status Update, Exception Logging & OTP
- [x] **Agent Authentication**:
  - [x] Agent logs in via `/api/auth/login` and receives JWT with `ROLE_AGENT`.
  - [x] Unauthenticated requests to `/api/agent/**` return 401 Unauthorized.
- [x] **Order Assignment Isolation**: Agent can only view orders assigned to their `agent_id`.
- [x] **Performance SLA**:
  - [x] Field update submitted by agent reflects in backend and customer tracking page within **5 seconds**.
- [x] **Exception Logging**:
  - [x] Transitioning to `FAILED_ATTEMPT` without `reason_code` is rejected with 400 Bad Request.
  - [x] Valid reason codes (`RECIPIENT_ABSENT`, `WRONG_ADDRESS`, `ACCESS_DENIED`) accepted and saved in `order_events.reason_code`.
- [x] **OTP Delivery Verification**:
  - [x] Transitioning directly to `DELIVERED` without prior OTP verification is rejected.
  - [x] OTP generation returns 6-digit numeric string with 10-minute expiry timestamp.
  - [x] Correct OTP verification marks token verified and enables delivery completion.
  - [x] Incorrect OTP returns error message; expired OTP returns `OTP_EXPIRED`.
- [x] **Mobile Usability**: Mobile-first responsive UI rendered without horizontal scrolling on mobile viewports.

---

## US003: Customer Self-Service Tracking Page & Deep-Link Access
- [x] **Zero Authentication**: Accessing `/api/tracking/{orderReference}` and `/track/{orderReference}` requires no authorization header or session cookie.
- [x] **Performance SLA**:
  - [x] Tracking page loads and displays order status in **< 3.0 seconds**.
- [x] **Timeline Data Accuracy**:
  - [x] Displays current stage banner with color-coded badge.
  - [x] Visual progress bar shows all 8 stages with completed, active, and upcoming steps.
  - [x] Event log lists chronological history with human-readable timestamps and actor attribution.
  - [x] Shows assigned delivery agent name and contact only when order is `OUT_FOR_DELIVERY`.
- [x] **Deep-Link Handling**:
  - [x] Navigating directly to `/track/{orderReference}` retrieves and displays the correct order without requiring navigation from a home screen.

---

## US004: Event-Triggered Notifications Across All Eight Stages
- [x] **Full Stage Coverage**: Notifications fire for all 8 stages; no stage is silent.
- [x] **Dynamic Channel Routing**: Channels (`EMAIL`, `SMS`, `IN_APP`) configured in `notification_rules` are resolved per event type.
- [x] **Payload Content**:
  - [x] Notification payload includes: `orderReference`, `currentStage`, `timestamp`, and direct deep-link `/track/{orderReference}`.
- [x] **Performance SLA**:
  - [x] Notifications processed and recorded in `notification_log` within **5 minutes** of triggering event in >= 95% of test cases.
- [x] **Audit Trail**: Every notification attempt logged in `notification_log` with status, channel, and recipient.

---

## US005: Third-Party Carrier Webhook Ingestion & Native Event Publication
- [x] **Webhook Authentication**:
  - [x] Inbound POST `/api/webhooks/carrier/{carrierId}` without valid `X-Api-Key` rejected with 401 Unauthorized.
  - [x] Valid key accepted and carrier validated from `carrier_config`.
- [x] **Payload Validation & Mapping**:
  - [x] Malformed JSON or missing status yields 400 Bad Request and logs failure.
  - [x] Valid external carrier status (e.g. `FASTSHIP_DELIVERED`) maps to EDOTS stage (e.g. `DELIVERED`).
- [x] **Attribution**: Published event records `actor_id = carrierId` and `actor_type = CARRIER`.
- [x] **Tracking Reflection**: Customer tracking page updates seamlessly with carrier status.

---

## US006: Auto-Rescheduling on Failed Attempt & Max Attempt Enforcement
- [x] **Automatic Trigger**: `FAILED_ATTEMPT` event publishes to `edots.reschedule.trigger`.
- [x] **Reschedule Progression**:
  - [x] When `attempt_count < max_attempts`: next delivery slot assigned (next business day 10:00 AM), order transitions to `OUT_FOR_DELIVERY`, customer notified.
- [x] **Max Attempt Return**:
  - [x] When `attempt_count >= max_attempts` (default 3): order transitions to `RETURNED` with reason code `MAX_ATTEMPTS_EXCEEDED`.
- [x] **Configurability**: Changing `max_delivery_attempts` in `admin_config` dynamically adjusts threshold without server restart.
- [x] **System Attribution**: Reschedule events attributed to actor `SYSTEM`.

---

## US007: Admin Dashboard — Live Queue, Stale Detection & Notification Config
- [x] **Performance SLA**: Admin order queue renders in **< 3.0 seconds**.
- [x] **Filters & Pagination**: Table supports filtering by stage, carrier, agent, and date ranges.
- [x] **48-Hour Stale Detection**:
  - [x] Orders inactive for > 48 hours in active stages are visually flagged with stale alert badge.
  - [x] `StaleOrderDetectionJob` runs and surfaces stale count in dashboard stats.
- [x] **Dynamic Notification Rules**:
  - [x] Admin can Create, Read, Update, and Deactivate notification rules via UI.
  - [x] Modified rules take effect on subsequent events immediately without server restart.
- [x] **Real-Time WebSockets**:
  - [x] New events received via STOMP WebSocket automatically refresh table and metrics without browser refresh.
- [x] **Charts**: Recharts render orders by stage (bar chart) and timeline activity (line chart).

---

## US008: Immutable Event Audit Log — 90-Day Retention
- [x] **Comprehensive Capture**: Logs all transitions, OTP events, carrier webhooks, reschedule actions, and admin config edits.
- [x] **Immutability**: No REST endpoints exist for deleting/editing audit logs; database constraints disallow modifications.
- [x] **Retention Worker**:
  - [x] Scheduled job `AuditLogRetentionJob` purges records older than 90 days.
  - [x] Retention threshold is configurable via `application.yml`.
