# EDOTS Full Feature Specification (specs/spec.md)

## Feature Overview & Lifecycle
The Order Fulfilment State Machine supports eight distinct stages:
1. `ORDER_PLACED`
2. `PICKING`
3. `PACKED`
4. `DISPATCHED`
5. `OUT_FOR_DELIVERY`
6. `DELIVERED`
7. `FAILED_ATTEMPT`
8. `RETURNED`

Allowed state transitions:
- `ORDER_PLACED` → `PICKING`
- `PICKING` → `PACKED`
- `PACKED` → `DISPATCHED`
- `DISPATCHED` → `OUT_FOR_DELIVERY`
- `OUT_FOR_DELIVERY` → `DELIVERED` (Requires OTP verification)
- `OUT_FOR_DELIVERY` → `FAILED_ATTEMPT` (Requires exception reason code)
- `FAILED_ATTEMPT` → `OUT_FOR_DELIVERY` (Triggered via auto-reschedule when `attempt_count < max_attempts`)
- `FAILED_ATTEMPT` → `RETURNED` (Triggered when `attempt_count >= max_attempts`)

---

## US001: Eight-Stage Event Model with State Machine Enforcement

### Actors
- System Engine, Warehouse Operators, Delivery Agents, External Carriers.

### Preconditions
- An order exists in `orders` table.
- A proposed transition request specifies valid `order_id`, `new_stage`, `actor_id`, and `actor_type`.

### Main Flow
1. Client/Module requests a transition for `order_id` to `new_stage`.
2. State machine validates that `current_stage` can transition to `new_stage`.
3. An immutable `OrderEvent` is created in memory with a current UTC timestamp.
4. Database transaction persists `OrderEvent` to `order_events` and updates `orders.current_stage = new_stage` and `orders.updated_at = NOW()`.
5. An event envelope is published to Kafka topic `edots.order.events`.
6. System returns HTTP 200 / execution success.

### Alternative Flows
- **Invalid Transition Attempt**:
  1. System checks transition against allowed matrix.
  2. Transition is disallowed (e.g., `ORDER_PLACED` → `DELIVERED`).
  3. System throws `InvalidStateTransitionException` with reason code `ILLEGAL_STAGE_TRANSITION`.
  4. Transaction rolls back; no event is written; HTTP 422 Unprocessable Entity returned.

### Acceptance Criteria
- All 8 stages are strictly mapped.
- Out-of-sequence transitions are blocked before write with a typed reason code.
- Every event stores `order_id`, `event_type`, `previous_stage`, `new_stage`, `actor_id`, `actor_type`, `created_at`.

### Data Model Impact
- Inserts row in `order_events`.
- Updates `current_stage` and `updated_at` in `orders`.

### API Contracts
- Internal Service Interface: `OrderStateMachine.transition(Order order, Stage newStage, Actor actor, Optional<String> reasonCode)`

### Event Schema (`edots.order.events`)
```json
{
  "eventId": "uuid-v4",
  "orderId": 101,
  "orderReference": "ORD-2026-9812",
  "eventType": "ORDER_STAGE_CHANGED",
  "previousStage": "DISPATCHED",
  "newStage": "OUT_FOR_DELIVERY",
  "actor": {
    "actorId": "agent1@edots.dev",
    "actorType": "AGENT"
  },
  "reasonCode": null,
  "metadata": {},
  "timestamp": "2026-09-10T10:00:00Z"
}
```

### Edge Cases
- Concurrent transition attempts: Handled via optimistic locking (`version` on order) or row-level `PESSIMISTIC_WRITE` locks on `orders`.

---

## US002: Delivery Agent Interface — Field Status Update, Exception Logging & OTP

### Actors
- Delivery Agent.

### Preconditions
- Delivery Agent is authenticated via JWT (`ROLE_AGENT`).
- Order is assigned to the authenticated agent and is in `DISPATCHED` or `OUT_FOR_DELIVERY` or `FAILED_ATTEMPT`.

### Main Flow (Standard Stage Progression)
1. Agent opens mobile dashboard at `/agent`.
2. Agent selects order assigned to them and clicks "Update Stage".
3. Agent selects new stage (e.g. `OUT_FOR_DELIVERY`).
4. System validates transition, records event, and updates tracking.

### Alternative Flow 1: Successful Delivery with OTP
1. Agent selects `DELIVERED`.
2. UI opens OTP Modal. Agent requests OTP generation (or asks recipient for the OTP sent via email/SMS).
3. Backend issues OTP (6 digits, 10 min TTL) and persists in `otp_tokens`.
4. Agent submits 6-digit OTP to `POST /api/agent/orders/{id}/otp/verify`.
5. Backend verifies code, marks `verified = true`, and allows the transition to `DELIVERED`.

### Alternative Flow 2: Failed Delivery Attempt
1. Agent selects `FAILED_ATTEMPT`.
2. UI forces selection of reason code: `RECIPIENT_ABSENT`, `WRONG_ADDRESS`, or `ACCESS_DENIED`.
3. Agent submits request with `reason_code`.
4. System updates order stage to `FAILED_ATTEMPT`, increments `attempt_count`, and emits event to `edots.order.events` and `edots.reschedule.trigger`.

### Acceptance Criteria
- Agent transition reflects in customer tracking within 5 seconds.
- `FAILED_ATTEMPT` requires reason code.
- `DELIVERED` requires verified OTP.
- Mobile-first responsive UI.

### Data Model Impact
- Inserts into `otp_tokens` (`order_id`, `agent_id`, `otp_code`, `expires_at`, `verified`).
- Increments `orders.attempt_count` on failed attempts.

### API Contracts
- `GET /api/agent/orders`: Returns list of assigned orders.
- `POST /api/agent/orders/{id}/transition`: Body `{ "newStage": "...", "reasonCode": "..." }`.
- `POST /api/agent/orders/{id}/otp/generate`: Generates OTP token.
- `POST /api/agent/orders/{id}/otp/verify`: Body `{ "otpCode": "123456" }`.

### Edge Cases
- OTP expired: System rejects verification with `OTP_EXPIRED`; agent can re-generate.
- Exceeded OTP attempts: Lock OTP after 5 consecutive incorrect guesses.

---

## US003: Customer Self-Service Tracking Page & Deep-Link Access

### Actors
- Customer (public user, no authentication).

### Preconditions
- Valid `orderReference` (e.g., `ORD-2026-0001`).

### Main Flow
1. Customer navigates to `/track` or clicks deep-link `/track/ORD-2026-0001`.
2. Frontend calls `GET /api/tracking/ORD-2026-0001` and `GET /api/tracking/ORD-2026-0001/events`.
3. Page renders order current stage badge, 8-stage progress tracker, full event timeline with timestamps and actors, and delivery agent name (if assigned and `OUT_FOR_DELIVERY`).

### Alternative Flows
- Order not found: API returns 404 with error details; UI renders friendly "Order Not Found" state with lookup retry form.

### Acceptance Criteria
- Publicly accessible without credentials.
- Shows current stage, history with timestamps, agent name during delivery.
- Page responds in < 3 seconds.

### API Contracts
- `GET /api/tracking/{orderReference}`: Returns order summary, customer name, current stage, assigned agent info.
- `GET /api/tracking/{orderReference}/events`: Returns ordered list of `OrderEventResponse` records.

---

## US004: Event-Triggered Notifications Across All Eight Stages

### Actors
- System Notification Engine, Customer.

### Preconditions
- An order stage event is emitted on `edots.order.events`.

### Main Flow
1. Notification service consumes event from Kafka.
2. Service evaluates active rules in `notification_rules` for the given `event_type`.
3. Determines channels (e.g. `["EMAIL", "SMS", "IN_APP"]`).
4. Generates deep-link `http://<host>/track/{orderReference}`.
5. In MVP mode, simulates sending by formatting message templates and writing structured logs.
6. Persists notification record in `notification_log` with status `SENT`.

### Acceptance Criteria
- Notifications fire for all 8 stages.
- Channels configurable by admin.
- Includes order reference, stage label, timestamp, and deep-link.
- Notification generated within 5 minutes (SLA).

### Data Model Impact
- Inserts into `notification_log` (`order_id`, `customer_email`, `channel`, `event_type`, `payload`, `status`, `sent_at`).

---

## US005: Third-Party Carrier Webhook Ingestion & Native Event Publication

### Actors
- External Carrier Systems (e.g., FastShip, BlueDart).

### Preconditions
- Carrier has an active entry in `carrier_config` with an API key and stage mappings.

### Main Flow
1. External carrier sends HTTP POST to `/api/webhooks/carrier/{carrierId}` with header `X-Api-Key: secret123` and JSON payload containing external tracking status.
2. System validates API key against SHA-256 hash in `carrier_config`.
3. System validates payload schema.
4. System looks up `stage_mapping` to convert carrier status (e.g., `IN_TRANSIT` → `DISPATCHED`, `OUT_DELIVERY` → `OUT_FOR_DELIVERY`).
5. Order state machine executes transition with `actor_type = CARRIER` and `actor_id = carrierId`.
6. Event is published to Kafka and tracking page updates in real-time.

### Alternative Flows
- Invalid API Key: Returns 401 Unauthorized; logs security audit.
- Unrecognized or malformed payload: Returns 400 Bad Request; logs failure to audit table.

### Acceptance Criteria
- Webhook validated on receipt.
- Valid carrier statuses mapped to EDOTS stages.
- Customer tracking shows carrier events identically to own-fleet events.

---

## US006: Auto-Rescheduling on Failed Attempt & Max Attempt Enforcement

### Actors
- System Rescheduler.

### Preconditions
- An order transitions to `FAILED_ATTEMPT`.
- Message is dispatched to Kafka topic `edots.reschedule.trigger`.

### Main Flow (Attempt < Max Attempts)
1. Reschedule consumer receives event.
2. Checks `order.attempt_count` vs `order.max_attempts` (default 3 from `admin_config`).
3. If `attempt_count < max_attempts`:
   - Calculates next delivery slot (next business day at 10:00 AM).
   - Updates `orders.next_delivery_slot`.
   - Transitions order from `FAILED_ATTEMPT` back to `OUT_FOR_DELIVERY` with `actor_type = SYSTEM`.
   - Publishes notification command with new slot details.

### Alternative Flow (Attempt >= Max Attempts)
1. If `attempt_count >= max_attempts`:
   - Transitions order from `FAILED_ATTEMPT` to `RETURNED` with `actor_type = SYSTEM` and reason code `MAX_ATTEMPTS_EXCEEDED`.
   - Notifies customer of order return to warehouse.

### Acceptance Criteria
- Auto-rescheduling triggered on failed attempt.
- Next delivery slot assigned.
- At max attempts (configurable, default 3), transitions to `RETURNED`.
- All actions attributed to actor `SYSTEM`.

---

## US007: Admin Dashboard — Live Queue, Stale Detection & Notification Config

### Actors
- Administrator (`ROLE_ADMIN`).

### Preconditions
- Authenticated via JWT.

### Main Flow
1. Admin opens `/admin`.
2. Admin views live order queue with search, stage filter, carrier filter, agent filter, and pagination.
3. System highlights stale orders where `updated_at` is older than 48 hours and `current_stage NOT IN ('DELIVERED', 'RETURNED')`.
4. Admin can view and mutate notification rules dynamically via CRUD endpoints (`GET/POST/PUT/DELETE /api/admin/notifications/rules`).
5. Live STOMP WebSocket connection pushes new order events instantaneously to the UI.
6. Recharts visualizations render order count breakdowns by stage and activity timeline.

### Acceptance Criteria
- Queue renders within 3s.
- Stale orders flagged at 48h.
- Rule changes apply immediately without server restart.
- All admin actions logged to audit trail.

---

## US008: Immutable Event Audit Log — 90-Day Retention

### Actors
- Compliance Officer, Administrator, System Purge Worker.

### Preconditions
- Actions occur anywhere in the system (stage changes, OTP verification, carrier webhook, config updates).

### Main Flow
1. An action occurs; event is posted to `edots.audit.log`.
2. Audit consumer writes row to `audit_log` table.
3. Records are immutable: application contains no update/delete APIs, and DB prevents updates.
4. `@Scheduled` job `AuditLogRetentionJob` runs nightly and safely deletes entries where `created_at < NOW() - INTERVAL '90 days'`.

### Acceptance Criteria
- All domain events, OTP verifications, webhooks, reschedule actions, and admin rule changes logged.
- Entries include log ID, event type, actor ID, reference ID, timestamp, and details.
- Read-only table.
- 90-day retention job verified.
