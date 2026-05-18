# Booking Engine — Deep Dive

## Problem

Multiple clients attempt to book the same slot with the same employee simultaneously.
The system must guarantee that only one request succeeds.

## Solution: PostgreSQL Advisory Lock + EXCLUDE Constraint

### Protection Layers

```
Layer 1: Advisory Lock (optimistic)
  pg_try_advisory_xact_lock(employee_id) — per-transaction, released automatically
  → Serializes requests per employee at application level

Layer 2: EXCLUDE Constraint (final guarantee)
  EXCLUDE USING GIST (employee_id WITH =, tstzrange(start_time, end_time) WITH &&)
  → Database-level guarantee against overlapping bookings
```

### Single Booking Sequence

```
Client POST /bookings
  │
  ├── BookingValidator.validate()
  │     ├── business active?
  │     ├── service active?
  │     └── employee assigned to this service?
  │
  ├── BEGIN TRANSACTION
  │
  ├── pg_try_advisory_xact_lock(employee_id)
  │     └── if false → HTTP 409 "Slot temporarily unavailable, try again"
  │
  ├── SELECT FOR UPDATE — re-check slot availability
  │     └── if taken → HTTP 409 "Slot no longer available"
  │
  ├── INSERT INTO bookings (...)
  │     └── EXCLUDE constraint — final protection
  │
  ├── COMMIT → advisory lock released automatically
  │
  └── Publish BookingConfirmedEvent (async)
```

### Concurrency Test

```
20 parallel requests → one slot → one employee
Expected result:
  - 1 request: HTTP 201 Created
  - 19 requests: HTTP 409 Conflict
```

## Booking Cancellation

### Rules

- **CLIENT**: may cancel own booking up to 24h before start time
- **BUSINESS_OWNER/EMPLOYEE**: may cancel any booking within their business
- Cancellation is recorded in `booking_cancellations` with a reason

### Status Machine

```
PENDING ──(confirm)──► CONFIRMED ──(complete)──► COMPLETED
PENDING ──(cancel)───► CANCELLED
CONFIRMED ──(cancel)─► CANCELLED
CONFIRMED ──(no_show)─► NO_SHOW
```

## Snapshot Data

On booking creation, snapshots are stored:
- `price_snapshot` — service price at the time of booking
- `duration_min` — service duration
- `selected_attributes` — chosen attributes (JSONB)

This ensures immutability of booking data even if the service is later modified.

## Redis as Alternative (for Horizontal Scaling)

```
SET NX PX lock:booking:{employee_id}:{date}:{slot} TTL=30s
```

Used when the Booking Engine runs on multiple instances.
PostgreSQL advisory locks are tied to a single connection and are unsuitable for distributed systems.
