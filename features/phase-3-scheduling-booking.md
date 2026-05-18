# Phase 3 — Scheduling & Booking Engine

## Overview

Employee/resource schedule management, available slot generation and conflict-safe booking.

## Architecture

```
Client → GET /slots?serviceId=&employeeId=&date=
           → SlotGeneratorService (pure logic, no side-effects)
              ← ScheduleRules, ScheduleOverrides, ScheduleBreaks
              ← Existing Bookings (occupied slots)
           → List<LocalTime> available slots

Client → POST /bookings
           → BookingService.createBooking()
              1. BookingValidator: business active? service active? employee assigned?
              2. Acquire advisory lock: pg_try_advisory_xact_lock(employee_id)
              3. Re-check slot availability (SELECT FOR UPDATE)
              4. INSERT booking
              5. Release lock (auto on commit)
              6. Publish BookingConfirmedEvent
```

## Race Condition Prevention

**Strategy**: PostgreSQL Advisory Lock + EXCLUDE constraint

1. `SlotGeneratorService` generates available slots (no locking).
2. `BookingService.createBooking()`:
   - Acquires advisory lock: `pg_try_advisory_xact_lock(employee_id)`
   - Re-checks slot availability within the lock
   - INSERT booking
   - Lock released automatically on transaction commit
   - EXCLUDE constraint — final database-level guarantee
3. If lock not acquired → HTTP 409 Conflict

## Slot Generation Algorithm

```
Input: employeeId (or resourceId), serviceId, date
Output: List<LocalTime> available start times

1. Load schedule_rules for dayOfWeek(date)
   → No rule or is_working_day=false → empty list
2. Load schedule_overrides for date
   → Full day off → empty list
   → Partial override → adjust working window
3. Load schedule_breaks for the rule
4. Load existing bookings for the date (status != CANCELLED)
5. Build occupied time ranges
6. Iterate slots (15-min step) from workStart to workEnd:
   → Slot is available if:
      - slotStart >= workStart AND slotEnd <= workEnd
      - Does not overlap any break
      - Does not overlap any existing booking
      - slotEnd = slotStart + service.durationMin
7. Return list of available LocalTime values
```

## Backend Implementation

### New Tables (V3 migration)

```sql
employees              -- business employees
resources              -- bookable resources (courts, rooms, etc.)
service_employees      -- M2M: service ↔ employee
service_resources      -- M2M: service ↔ resource
schedule_rules         -- recurring weekly schedule
schedule_overrides     -- one-off overrides (holidays, vacations)
schedule_breaks        -- breaks within the working day
bookings               -- bookings
booking_cancellations  -- booking cancellations
```

### Components

| Class | Responsibility |
|-------|---------------|
| `EmployeeController` | CRUD `/businesses/{id}/employees` |
| `ResourceController` | CRUD `/businesses/{id}/resources` |
| `ScheduleController` | GET/PUT employee/resource schedule |
| `SlotController` | `GET /slots` |
| `BookingController` | CRUD bookings |
| `SlotGeneratorService` | Slot generation algorithm (pure) |
| `BookingService` | Booking with advisory lock |
| `BookingValidator` | Pre-booking validation |
| `ScheduleService` | Schedule CRUD |

### Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `GET/POST` | `/businesses/{id}/employees` | OWNER/EMPL | List/add employees |
| `GET/POST` | `/businesses/{id}/resources` | OWNER | List/add resources |
| `GET/PUT` | `/employees/{id}/schedule` | OWNER/EMPL | Read/save schedule |
| `GET` | `/slots` | Public | Available slots |
| `POST` | `/bookings` | CLIENT | Create booking |
| `GET` | `/bookings/my` | CLIENT | Own booking history |
| `GET` | `/bookings/business/{id}` | OWNER/EMPL | Business bookings |
| `PATCH` | `/bookings/{id}/confirm` | OWNER/EMPL | Confirm |
| `PATCH` | `/bookings/{id}/cancel` | Auth | Cancel |
| `PATCH` | `/bookings/{id}/complete` | OWNER/EMPL | Complete |

### Events

| Event | When |
|-------|------|
| `BookingConfirmedEvent` | After booking is confirmed |
| `BookingCancelledEvent` | After booking is cancelled |

## Frontend Implementation

### Components

| Component | Description |
|-----------|-------------|
| `BusinessDetailComponent` | Business info, services, employee picker |
| `SlotPickerComponent` | Date navigation → available slot grid |
| `BookingConfirmComponent` | Booking confirmation with note |
| `BookingSuccessComponent` | Success screen |
| `DashboardComponent` | Client booking history |
| `BookingCalendarComponent` | Business owner booking list (confirm/complete/cancel) |
| `ScheduleManagementComponent` | Employee weekly schedule management |

### NgRx Booking Store

```typescript
// Actions
loadSlots, slotsLoaded
createBooking, bookingCreated, bookingFailed
loadMyBookings, myBookingsLoaded
cancelBooking, bookingCancelled
confirmBooking, completeBooking
loadBusinessBookings, businessBookingsLoaded
```

## Tests

- Concurrent test: 20 requests to one slot → 1 success, 19 HTTP 409
- Unit: `SlotGeneratorService` — 100% coverage
- Integration: full booking flow
