# EDOTS Specification Clarifications & Design Decisions (specs/clarify.md)

This document formalizes architectural clarifications and resolves design ambiguities across the EDOTS specification. All decisions prioritize system integrity, auditability, operational reliability, and ease of demonstration.

---

### Clarification 1: What happens if an OTP expires mid-delivery?
- **Ambiguity**: An agent arrives at the customer doorstep, triggers OTP generation, but the customer takes longer than 10 minutes to locate their phone, causing the OTP token to expire before verification is submitted.
- **Resolution**:
  - OTP tokens are valid for strictly 10 minutes from creation.
  - If an expired OTP is submitted, the API returns HTTP 400 with error code `OTP_EXPIRED`.
  - The delivery agent UI presents an explicit "Regenerate OTP" action button.
  - When regenerated, previous unverified OTP tokens for that order are invalidated, a new 6-digit cryptographic code is created, and an audit log event `OTP_REGENERATED` is emitted.
  - Up to 3 regenerations are allowed per delivery session. If an OTP is attempted with 5 consecutive incorrect entries, it is locked with status `OTP_LOCKED`.

---

### Clarification 2: What is the exact retry window and scheduling logic for auto-rescheduling?
- **Ambiguity**: US006 specifies that when a delivery attempt fails, the system reschedules to the "next available delivery slot per configured rules", but does not specify weekend/holiday handling or time-of-day slots.
- **Resolution**:
  - Rescheduling assigns the next calendar day at 10:00 AM local time. If the next day falls on Sunday, it shifts to Monday at 10:00 AM.
  - The calculated slot is stored in `orders.next_delivery_slot`.
  - The order status transitions from `FAILED_ATTEMPT` back to `OUT_FOR_DELIVERY` upon scheduling, incrementing `attempt_count` by 1.
  - If `attempt_count` reaches `max_attempts` (default 3, configured in `admin_config`), the order immediately transitions to `RETURNED`, setting `reason_code = MAX_ATTEMPTS_EXCEEDED` and preventing further scheduling.

---

### Clarification 3: How does the carrier webhook authenticate (API Key vs HMAC Signature)?
- **Ambiguity**: Carrier webhooks can be authenticated via shared secret API keys or HMAC SHA-256 header signatures.
- **Resolution**:
  - Inbound carrier webhooks authenticate via the HTTP header `X-Api-Key`.
  - In `carrier_config`, the system stores `api_key_hash` (computed via SHA-256 hex) rather than plaintext secrets.
  - Upon receiving `POST /api/webhooks/carrier/{carrierId}`, the service hashes the incoming header value and performs a constant-time comparison (`MessageDigest.isEqual`) against the stored hash.
  - This provides strong security without requiring carrier-side HMAC timestamping synchronization for third-party integrations, while eliminating plaintext key storage in the database.

---

### Clarification 4: What happens to in-flight notifications if an admin updates a rule?
- **Ambiguity**: If an administrator edits or disables a notification rule while an event is in-flight on Kafka, will the event use the old or new rule?
- **Resolution**:
  - Notification rules are evaluated on-demand by `edots-notification` at message consumption time directly from PostgreSQL (not cached statically at service boot).
  - Therefore, rule updates take immediate effect for all events consumed after the update transaction commits in PostgreSQL.
  - There is no rollback or re-triggering for notifications already dispatched and recorded in `notification_log`.

---

### Clarification 5: What "channels" does in-app notification use for the MVP?
- **Ambiguity**: US004 mentions "SMS, email, in-app, or combination", but external gateway accounts (Twilio/SendGrid/Push) are not provisioned for local MVP evaluation.
- **Resolution**:
  - All channels (`EMAIL`, `SMS`, `IN_APP`) are fully supported in the schema and admin rule builder.
  - The dispatch engine simulates delivery by formatting the template with order metadata and persisting a structured record in `notification_log` (`channel`, `customer_email`, `payload`, `status = 'SENT'`, `sent_at`).
  - For `IN_APP`, a simulated delivery payload is broadcast over WebSocket and recorded in `notification_log` so it appears live in the client interface.
  - Real logger outputs format the message identically to an outbound gateway payload, ensuring 100% testability with zero external network dependencies.

---

### Clarification 6: State Machine Concurrency Control
- **Ambiguity**: What prevents race conditions if an agent submits a stage update at the exact moment an automated webhook or admin override arrives?
- **Resolution**:
  - Spring Data JPA uses `@Version` (optimistic locking) on the `Order` entity.
  - If a concurrent modification occurs, Hibernate throws an `OptimisticLockException`, which is caught by a retry template (up to 3 retries with exponential backoff) or returns HTTP 409 Conflict to field agents.
  - This guarantees that two conflicting stage transitions cannot execute simultaneously.
