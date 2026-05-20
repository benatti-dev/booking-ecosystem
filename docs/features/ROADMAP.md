# Roadmap — Phase 7 and Beyond

This document outlines planned features after the Phase 6 completion milestone. Each phase is designed as an independently shippable increment.

---

## Phase 7 — Payments

**Goal:** Monetise bookings with a prepayment, deposit, or full-charge option at booking time.

### Planned Capabilities

- **Stripe integration** — payment intents created at booking creation; captured on confirmation
- **Deposit mode** — business configures a percentage deposit; remainder collected at appointment
- **Prepay mode** — full amount captured upfront; released to business on `COMPLETED`
- **Refund flows** — automatic Stripe refund on owner-initiated cancellation; partial refund (minus late-cancel fee) for client cancellations within policy window
- **Payout schedule** — weekly Stripe Connect payouts to business Stripe accounts
- **Webhook handler** — secure Stripe signature verification; handles `payment_intent.succeeded`, `charge.refunded`, `account.updated`
- **Payment status** — `PaymentStatus` enum (`PENDING`, `PAID`, `PARTIALLY_REFUNDED`, `REFUNDED`, `FAILED`) stored on `Booking`
- **Admin override** — platform admin can issue manual refunds

### Technical Considerations

- Stripe SDK added as a Maven dependency
- `PaymentService` interface with `StripePaymentServiceImpl` — mockable in tests
- Idempotency keys on all Stripe API calls (prevents duplicate charges on retry)
- Webhook endpoint excluded from CSRF/JWT filter (`/api/payments/webhook`)

---

## Phase 8 — Reviews & Ratings

**Goal:** Build trust and social proof for service businesses through verified post-booking reviews.

### Planned Capabilities

- **Post-booking prompt** — notification sent 2h after `COMPLETED` status with a review link
- **Star rating** — 1–5 integer stars, required for submission
- **Text review** — optional free-text, max 1000 chars, profanity-filtered before persist
- **Verified badge** — review only creatable by the client who held the booking (`bookingId` validated)
- **Business aggregate** — `averageRating` and `reviewCount` updated asynchronously via `@TransactionalEventListener`
- **Owner response** — business owner can post a single public reply to each review
- **Admin moderation** — flag and hide reviews; appeals workflow
- **Search integration** — minimum rating filter added to geo-search endpoint

### Schema Additions

```sql
CREATE TABLE review (
    id          BIGSERIAL PRIMARY KEY,
    booking_id  BIGINT UNIQUE REFERENCES booking(id),
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    owner_reply TEXT,
    visible     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## Phase 9 — Mobile App

**Goal:** Native-quality mobile experience for clients (discovery + booking) and employees (schedule viewing).

### Technology Options

| Option | Pros | Cons |
|---|---|---|
| **React Native** | Large ecosystem, Expo toolchain, strong community | Bridge overhead for complex animations |
| **Flutter** | Excellent UI fidelity, single codebase | Dart learning curve |

**Recommended:** React Native with Expo — fastest time-to-market given the REST API is already complete.

### Planned Scope (MVP)

- Auth (login, register, biometric unlock via expo-local-authentication)
- Geo-aware business search with map view (react-native-maps)
- Slot selection and booking creation
- Push notifications (Expo Push / FCM / APNs) mirroring WebSocket events
- Employee daily schedule viewer

---

## Phase 10 — Recurring Bookings

**Goal:** Allow clients to create a standing appointment that auto-generates bookings on a weekly or bi-weekly cadence.

### Planned Capabilities

- **Recurrence rule** — RRULE-compatible: `WEEKLY;BYDAY=MO;INTERVAL=1;COUNT=12`
- **Auto-generation** — `@Scheduled` job generates next occurrence 7 days ahead if the previous one is `CONFIRMED`
- **Series management** — cancel one occurrence / cancel all future / reschedule one
- **Conflict handling** — if a future slot is already taken, send notification and skip (rather than fail entire series)

---

## Phase 11 — Waitlist Management

**Goal:** Capture demand for fully-booked slots and automatically offer released capacity.

### Planned Capabilities

- **Join waitlist** — client joins queue for a specific slot+employee combination
- **Auto-offer** — when a booking is cancelled, the next client in queue receives a timed offer (15-minute window to claim the slot)
- **Offer expiry** — if not claimed, offer moves to next in queue
- **Position visibility** — client can see their position in the waitlist

---

## Phase 12 — Public API and Webhooks

**Goal:** Allow third-party systems (POS systems, CRMs, other platforms) to integrate with Booker.

### Planned Capabilities

- **API key management** — business owners generate API keys in the admin panel
- **Scoped permissions** — keys can be restricted to `bookings:read`, `bookings:write`, `schedule:read`
- **Webhook subscriptions** — register an HTTPS endpoint to receive events (`booking.created`, `booking.confirmed`, `booking.cancelled`, `booking.completed`)
- **Delivery guarantees** — at-least-once delivery with exponential backoff and dead-letter logging
- **Webhook signature** — `X-Booker-Signature: sha256=<hmac>` header on every delivery
- **OpenAPI spec export** — `GET /api/api-docs.yaml` for SDK generation

---

## Phase 13 — Loyalty & Rewards

**Goal:** Retain clients through a points-based rewards programme.

### Planned Capabilities

- **Points accrual** — configurable per-business points-per-booking (e.g., 10 pts per £10 spent)
- **Tier system** — Bronze / Silver / Gold tiers with increasing discounts
- **Redemption** — points redeemable as a discount code at checkout (requires Phase 7)
- **Expiry** — points expire after 12 months of inactivity
- **Analytics** — loyalty programme ROI dashboard for business owners

---

## Phase 14 — Microservices Migration Path

**Goal:** Extract high-traffic or independently scalable modules into separate services when monolith bottlenecks are measurable.

### Extraction Candidates (in priority order)

| Module | Trigger | Target |
|---|---|---|
| `search` | Read replica not enough to handle query volume | Elasticsearch or dedicated read service |
| `notification` | Email volume or WebSocket connections exceed monolith capacity | Dedicated service + Kafka event bus |
| `booking` | Booking write throughput requires horizontal scale | CQRS with separate write/read models |
| `analytics` | Complex aggregations affecting OLTP performance | Separate read replica or columnar store |

### Preparation (already done)

- No direct cross-module repository calls (events only) — extraction boundary is clean
- `PagedResponse<T>` and all shared DTOs in a separate `shared` package — easily extracted as a library
- Flyway migrations per module — already conceptually separate

---

## Long-Term Vision

```
Booker v1.0  (Phases 1–6)  — Full-featured marketplace, production-ready
Booker v1.5  (Phases 7–9)  — Monetised, reviewed, mobile-accessible
Booker v2.0  (Phases 10–13) — Platform play: third-party ecosystem + loyalty
Booker v3.0  (Phase 14)    — Microservices at scale
```
