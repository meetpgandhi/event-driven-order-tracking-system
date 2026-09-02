# EDOTS User Stories

## US001: Eight-Stage Event Model with State Machine Enforcement
**Module:** Event Engine & Fulfilment Stage Model  
**User Story:** As a System, I want every order transition across all eight fulfilment stages — Order Placed, Picking, Packed, Dispatched, Out for Delivery, Delivered, Failed Attempt, Returned — to be published as a timestamped, actor-attributed event with out-of-sequence transitions rejected, so that no status change goes untracked, every event is attributable, and the fulfilment record is always in a consistent and auditable state.

### Acceptance Criteria:
1. All eight stages are represented in the event model; an order cannot skip a stage without a logged event.
2. Out-of-sequence stage transitions are rejected with a reason code before the event is written.
3. Every event record contains: order ID, event type, actor ID, and timestamp.
4. **UAT:** Full stage walkthrough logged and traceable; out-of-sequence rejection verified.

---

## US002: Delivery Agent Interface — Field Status Update, Exception Logging & OTP
**Module:** Delivery Agent Interface  
**User Story:** As a Delivery Agent, I want to update order status, log delivery exceptions with a reason code, and confirm delivery via OTP at the Delivered stage from a mobile web interface, so that every field action is recorded as a real-time event with my ID attributed and customers are notified immediately without me contacting the operations centre.

### Acceptance Criteria:
1. Agent selects order, selects new stage, and submits; event is published and customer tracking page reflects the change within 5 seconds.
2. Failed Attempt requires a reason code selection (recipient absent, wrong address, access denied) before submission.
3. Delivered stage presents an OTP entry field; submission is blocked until a valid OTP is entered.
4. Interface is accessible via mobile browser; no native app or offline capability required.
5. **UAT:** Timed update test ≤ 5 seconds; OTP flow verified; exception reason code logged.

---

## US003: Customer Self-Service Tracking Page & Deep-Link Access
**Module:** Customer Order Tracking  
**User Story:** As a Customer, I want to view my order's live status and full stage history via both an order ID lookup page and a deep-link in my notification, so that I can check where my order is at any point without contacting support, and each notification takes me directly to my order's current status.

### Acceptance Criteria:
1. Tracking page accessible without authentication via order ID lookup.
2. Page displays current stage, full stage history with timestamps, and assigned agent name when Out for Delivery.
3. Every customer notification includes a deep-link that opens the tracking page for that order directly.
4. Page loads and displays current status within 3 seconds.
5. **UAT:** Page load timing; history accuracy; deep-link from each notification channel verified.

---

## US004: Event-Triggered Notifications Across All Eight Stages
**Module:** Customer Notifications  
**User Story:** As a Customer, I want to receive a notification at every fulfilment stage event via the channels configured for that event, with each notification containing my order reference, current stage, timestamp, and a direct link to my tracking page, so that I am informed at every milestone without polling the platform or contacting support.

### Acceptance Criteria:
1. Notifications fire at all eight stage events; no stage is silent.
2. Channel (SMS, email, in-app, or combination) is configurable per event type by the Administrator.
3. Each notification contains: order reference, current stage label, stage timestamp, and deep-link to tracking page.
4. Notification delivered within 5 minutes of triggering event in >= 95% of UAT test cases.
5. Every delivery logged with customer ID, channel, delivery status, and timestamp.

---

## US005: Third-Party Carrier Webhook Ingestion & Native Event Publication
**Module:** Third-Party Carrier Webhook Integration  
**User Story:** As an Administrator, I want carrier status events received via inbound webhooks to be validated, mapped to the EDOTS event model, and published as native tracking events with the carrier identified as the actor, so that orders fulfilled by external couriers appear in the same tracking interface and audit log as own-fleet deliveries — without any manual data entry.

### Acceptance Criteria:
1. Inbound webhook payloads are validated on receipt; invalid payloads are rejected and logged with a failure reason.
2. Valid carrier events are mapped to the EDOTS stage model and published with carrier ID as actor.
3. Customer tracking page reflects carrier events in the same format as own-fleet events.
4. **UAT:** Carrier test webhook processed; tracking page updated; carrier ID attributed in audit log.

---

## US006: Auto-Rescheduling on Failed Attempt & Max Attempt Enforcement
**Module:** Automatic Failed Delivery Rescheduling  
**User Story:** As an Administrator, I want a Failed Attempt event to automatically trigger rescheduling to the next available delivery slot per configured rules, notify the customer of the new slot, and initiate return-to-warehouse when the maximum attempt count is reached, so that no failed delivery stalls without a follow-up action and the return process is triggered consistently without manual intervention.

### Acceptance Criteria:
1. Failed Attempt event triggers automatic rescheduling; next delivery slot assigned per configured rules.
2. Customer notification fires with the new delivery slot details via the configured channel.
3. When maximum attempt count is reached, a Returned event is triggered automatically.
4. Maximum attempt count is configurable by the Administrator without developer intervention.
5. All rescheduling and return-trigger events are logged with actor ("system") and timestamp.

---

## US007: Admin Dashboard — Live Queue, Stale Detection & Notification Config
**Module:** Administrator Dashboard  
**User Story:** As an Administrator, I want a live fulfilment queue covering all active orders, stale order flags at 48 hours of inactivity, exception management, and the ability to configure notification rules per event type without developer intervention, so that I have real-time pipeline visibility, at-risk orders are surfaced before they escalate, and notification behaviour can be adjusted on the same day it is needed.

### Acceptance Criteria:
1. Live queue renders within 3 seconds, filterable by stage, carrier, agent, and date range.
2. Orders with no status update for 48 hours are flagged as stale and surfaced on the dashboard; administrator is notified.
3. Administrator can add, update, or remove notification rules per event type; changes take effect on the next triggered event without a system restart.
4. All configuration changes are logged with administrator ID, change detail, and timestamp.
5. **UAT:** Stale flag at 48 hours verified; config change reflected on next event; exception resolved and traced.

---

## US008: Immutable Event Audit Log — 90-Day Retention
**Module:** Audit Log  
**User Story:** As an Administrator, I want every system event — stage transitions, OTP verifications, carrier webhook ingestions, rescheduling actions, exception resolutions, and configuration changes — to be logged immutably with order ID, event type, actor ID, and timestamp, and retained for 90 days, so that the organisation has a complete, tamper-proof record for customer dispute resolution and compliance review.

### Acceptance Criteria:
1. All in-scope event types are logged: stage transitions, OTP verifications, carrier events, reschedule triggers, exception actions, and config changes.
2. Each entry contains: log ID, event type, actor ID, order or config reference, and timestamp.
3. Log entries are read-only; no role may edit or delete them.
4. All records retained for a minimum of 90 days; retention configuration verified.
