# Phase 6 — Hardening & Testing

## Overview

Production readiness: comprehensive test suite, security audit, caching layer, Docker packaging, and Angular production build configuration.

## Status: ✅ Complete

---

## TASK 6.1 — Unit Tests

Pure unit tests using `@ExtendWith(MockitoExtension.class)` — no Spring context, no database, no Docker. All external dependencies are mocked.

### SlotGeneratorServiceUnitTest

`com.booker.booking.SlotGeneratorServiceUnitTest`

| Test | Scenario |
|------|----------|
| `allSlotsAvailable` | No conflicts → all slots in working window returned |
| `noRuleForDay` | No schedule rule for this day of week → empty list |
| `ruleIsNonWorkingDay` | Rule exists but `isWorkingDay=false` → empty list |
| `fullDayOff` | Override with null start/end → empty list |
| `overrideShiftsHours` | Override narrows working window |
| `breakBlocksSlots` | Break at 10:00–10:30 → overlapping slots excluded |
| `breakCoversAll` | Break covers entire window → empty list |
| `existingBookingBlocksSlots` | Existing booking at 09:00 → overlapping slots excluded |
| `twoBookingsLeaveSingleGap` | Back-to-back bookings leave exactly one available slot |

**Coverage target:** 100% branch coverage of `computeSlots()`, `overlapsBreak()`, `overlapsBooking()`.

### ServiceAttributeValidatorUnitTest

`com.booker.catalog.ServiceAttributeValidatorUnitTest`

| Test | Scenario |
|------|----------|
| `noDefinitions_passes` | No definitions for category → always passes |
| `requiredTextField_absent_throws` | Required TEXT field absent → 400 |
| `requiredTextField_blank_throws` | Required TEXT field blank string → 400 |
| `optionalField_absent_passes` | Optional field absent → passes |
| `nullAttributeMap_requiredFieldThrows` | Null attributes map + required field → 400 |
| `text_anyString_passes` | TEXT type accepts any string |
| `number_integerValue_passes` | NUMBER type accepts Integer |
| `number_numericString_passes` | NUMBER type accepts numeric string |
| `number_nonNumericString_throws` | NUMBER type rejects non-numeric string → 400 |
| `boolean_true_passes` | BOOLEAN type accepts Boolean `true` |
| `boolean_string_throws` | BOOLEAN type rejects string "yes" → 400 |
| `select_validOption_passes` | SELECT with valid option → passes |
| `select_invalidOption_throws` | SELECT with invalid option → 400 |
| `multiSelect_allValid_passes` | MULTI_SELECT with all valid options → passes |
| `multiSelect_invalidOption_throws` | MULTI_SELECT with one invalid option → 400 |
| `multiSelect_nonList_throws` | MULTI_SELECT with non-list value → 400 |

### BookingServiceUnitTest

`com.booker.booking.BookingServiceUnitTest`

Focuses on status-transition methods (advisory lock path tested at integration level).

| Test | Scenario |
|------|----------|
| `confirm_pendingByOwner_success` | PENDING → CONFIRMED, `BookingConfirmedEvent` published |
| `confirm_alreadyConfirmed_throws` | Already CONFIRMED → 400 |
| `confirm_byClient_throws` | CLIENT tries to confirm → 403 |
| `complete_confirmedByOwner_success` | CONFIRMED → COMPLETED, event published |
| `complete_pending_throws` | PENDING (not confirmed) → 400 |
| `cancel_byOwner_success` | Owner cancels PENDING → CANCELLED, event published |
| `cancel_byClientWithEnoughNotice_success` | Client cancels own booking (>24h notice) → CANCELLED |
| `cancel_byClientTooLate_throws` | Client cancels with <24h notice → 400 |
| `cancel_wrongClient_throws` | Client cancels another client's booking → 403 |
| `cancel_completedBooking_throws` | Cancel COMPLETED booking → 400 |

### AuthServiceUnitTest

`com.booker.auth.AuthServiceUnitTest`

| Test | Scenario |
|------|----------|
| `register_success` | New email → user saved, tokens issued, audit log written |
| `register_duplicateEmail_throws` | Duplicate email → 409, `userRepository.save` never called |
| `login_success` | Correct credentials → tokens issued, audit log written |
| `login_badCredentials_throws` | Wrong password → `BadCredentialsException` propagated |
| `refresh_success` | Valid refresh token → new token pair issued |
| `refresh_expiredToken_throws` | Expired token → `BookerException` from `RefreshTokenService` |
| `logout_success` | Logout → token revoked, audit log written |

---

## TASK 6.2 — Integration Tests

All integration tests use `@SpringBootTest + @AutoConfigureMockMvc + @Testcontainers(disabledWithoutDocker=true)` with a real PostgreSQL instance via the Testcontainers JDBC URL.

### BookingConcurrencyIntegrationTest

`com.booker.booking.BookingConcurrencyIntegrationTest`

**Race condition test:** 5 concurrent clients simultaneously POST to the same employee slot. Uses `CountDownLatch` to synchronize thread start, then `ExecutorService` to fire all requests in parallel.

```
Expected:
  - 1 request → HTTP 201 Created
  - 4 requests → HTTP 409 Conflict
  - DB contains exactly 1 booking
```

This verifies the PostgreSQL advisory lock (`pg_try_advisory_xact_lock`) correctly serializes concurrent writes.

### Existing Integration Tests

| Test Class | Coverage |
|-----------|----------|
| `AuthIntegrationTest` | Register, login, refresh, logout, duplicate email, bad credentials |
| `BookingIntegrationTest` | Create, confirm, complete, cancel (owner/client/late/wrong-client), list |
| `SlotGeneratorIntegrationTest` | Slot generation with real schedule rules and existing bookings |
| `ScheduleIntegrationTest` | Schedule rule CRUD and override management |
| `BusinessIntegrationTest` | Business onboarding, branch management, category management |

---

## TASK 6.3 — OpenAPI Documentation

- All controllers annotated with `@Tag(name = "...", description = "...")`
- All endpoints annotated with `@Operation(summary = "...")`
- Bearer auth scheme configured globally in `OpenApiConfig` — authentication requirement is shown on every protected endpoint in Swagger UI
- `SpringDoc` path: `/api/api-docs`, UI: `/api/swagger-ui.html`

---

## TASK 6.4 — Security Audit

### Findings and Fixes

| Finding | Action |
|---------|--------|
| `POST /bookings` (create) was missing `@PreAuthorize("hasRole('CLIENT')")` | **Fixed** — BUSINESS_OWNER and ADMIN were incorrectly able to create bookings |
| CORS config uses `app.cors.allowed-origins` (explicit list, no wildcards) | ✅ Verified clean |
| JWT secret read from `@Value("${app.jwt.secret}")`, never logged | ✅ Verified clean |
| All admin endpoints protected by class-level `@PreAuthorize("hasRole('ADMIN')")` | ✅ Verified clean |
| `NotificationController` — all methods access `auth.getPrincipal()`; protected by global `.anyRequest().authenticated()` | ✅ Acceptable (no role restriction needed — users access only their own data) |

### Security Layers Summary

```
Layer 1: JwtAuthenticationFilter — validates Bearer token on every request
Layer 2: SecurityConfig — .anyRequest().authenticated() for non-public paths
Layer 3: @PreAuthorize — role-based method-level authorization
Layer 4: Service-layer checks — ownership verification (booking.client == caller, etc.)
Layer 5: Booking Engine — advisory lock + EXCLUDE constraint
```

---

## TASK 6.5 — Caching

| Cache | Method | Eviction |
|-------|--------|----------|
| `attributeDefs` (keyed by `categoryId`) | `AttributeDefinitionService.getDefinitions()` | On create/delete of definition |
| `businessCategories` | `BusinessCategoryService.listAll()` | On create of new category |

Both use Spring's `simple` cache provider (in-memory `ConcurrentHashMap`). Upgrading to Redis requires only a `spring.cache.type=redis` config change — no code changes.

### HikariCP Connection Pool

Already configured in `application.yml`:

```yaml
hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 30000    # 30s
  idle-timeout: 600000         # 10 min
  max-lifetime: 1800000        # 30 min
```

---

## TASK 6.6 — Docker Packaging

### Multi-Stage Dockerfile (`booker-backend/Dockerfile`)

```
Stage 1: maven:3.9-eclipse-temurin-21
  → mvn dependency:go-offline (cached layer)
  → mvn package -DskipTests
  → produces target/*.jar

Stage 2: eclipse-temurin:21-jre-jammy
  → Non-root user: booker:booker
  → JAVA_OPTS: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
  → EXPOSE 8080
```

### Updated `docker-compose.yml`

```yaml
services:
  postgres:   postgis/postgis:16-3.4  (with healthcheck)
  redis:      redis:7-alpine          (with healthcheck)
  app:        ./booker-backend        (depends_on both, with healthcheck)
```

Environment variables passed via `.env` file. `JWT_SECRET` is the only required variable.

---

## TASK 6.7 — Angular Production Build

### Environment File Swapping

`angular.json` production configuration now includes:

```json
"fileReplacements": [
  {
    "replace": "src/environments/environment.ts",
    "with": "src/environments/environment.prod.ts"
  }
]
```

`environment.prod.ts` uses a relative `/api` URL (works behind any reverse proxy) and `wss://` for WebSocket.

### Bundle Budget

| Bundle type | Warning | Error |
|---|---|---|
| Initial | 500kB | 1MB |
| Any component style | 4kB | 8kB |

All feature routes use `loadChildren` — code splitting is active for every feature module.

### Build Command

```bash
cd booker-frontend
npm run build                    # production build
npx ng build --stats-json        # inspect bundle composition
```
