# Booker Platform — Architectural Roadmap

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architectural Approach](#2-architectural-approach)
3. [Technology Stack](#3-technology-stack)
4. [Security Layer — Auth Libraries](#4-security-layer--auth-libraries)
5. [Database Design](#5-database-design)
6. [System Modules](#6-system-modules)
7. [API Specification](#7-api-specification)
8. [Development Phases (Roadmap)](#8-development-phases-roadmap)
9. [Non-Functional Requirements](#9-non-functional-requirements)

---

## 1. Project Overview

### What Is It?

A **multi-tenant SaaS marketplace** that aggregates service-based businesses (beauty salons, auto shops, medical clinics, gyms, tennis courts, etc.) into a single platform.

### User Personas

| Role              | Key Capabilities                                                                          |
|-------------------|-------------------------------------------------------------------------------------------|
| **Client**        | Search businesses, filter by location/category, book time slots, view booking history    |
| **Employee**      | View own schedule, confirm/decline bookings, manage personal availability                 |
| **Business Owner**| Manage branches, staff, services, schedules, view analytics dashboard                    |
| **Admin**         | Moderate businesses, manage users, view platform-wide analytics, configure categories     |

### Core Design Principles

1. **Dynamic-first** — No code change needed to add a new business type or service attribute.
2. **Conflict-safe** — Booking engine prevents double-booking via atomic DB locking.
3. **Modular** — Each domain is isolated; can be extracted to microservice later.
4. **Mobile-ready** — REST API designed for future React Native / Flutter client.

---

## 2. Architectural Approach

### Decision: Modular Monolith

**Rationale:** A Modular Monolith is chosen over microservices for the initial phases because:
- **Speed to market** — single deployable unit, no distributed system overhead.
- **ACID transactions** — critical for the Booking Engine (no saga orchestration needed at MVP).
- **Easy refactoring** — well-defined module boundaries allow extraction to microservices when load demands it.

### Module Boundary Rules (enforced by package structure)

```
No cross-module direct class dependency.
Modules communicate via Spring Application Events or shared DTOs only.
Each module owns its own DB tables (no cross-module JOINs in repositories).
```

### High-Level Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                  │
│   Angular 17 SPA        Mobile App (future)     External API     │
└────────────┬─────────────────────┬────────────────────┬─────────┘
             │ HTTPS/REST          │ HTTPS/REST         │
             ▼                     ▼                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                    API GATEWAY / NGINX                           │
│            (Rate limiting, TLS termination, routing)             │
└───────────────────────────┬──────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────────┐
│                SPRING BOOT 3 — MODULAR MONOLITH                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐  │
│  │   Auth   │ │ Business │ │ Catalog  │ │ Booking  │ │Search │  │
│  │  Module  │ │  Module  │ │  Module  │ │  Engine  │ │Module │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └───────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                          │
│  │Scheduler │ │Notif.    │ │Analytics │                          │
│  │  Module  │ │  Module  │ │  Module  │                          │
│  └──────────┘ └──────────┘ └──────────┘                          │
└──────────┬──────────────────────────────┬────────────────────────┘
           │                              │
           ▼                              ▼
  ┌─────────────────┐            ┌─────────────────┐
  │   PostgreSQL    │            │      Redis       │
  │  (Primary DB)   │            │  (Cache + Lock)  │
  └─────────────────┘            └─────────────────┘
           │
           ▼
  ┌─────────────────┐
  │ Email Provider  │
  │(SES / SendGrid) │
  └─────────────────┘
```

---

## 3. Technology Stack

### Backend

| Layer              | Technology                                    | Reason                                              |
|--------------------|-----------------------------------------------|-----------------------------------------------------|
| Language           | Java 21 (LTS)                                 | Virtual threads (Project Loom), mature ecosystem    |
| Framework          | Spring Boot 3.3.x                             | Auto-configuration, Spring Security 6               |
| Security           | `benatti-auth-starter` 1.0.1 + Spring Security| JWT auth, production-ready starter                  |
| ORM                | Spring Data JPA + Hibernate 6                 | JPQL, native queries for JSONB                      |
| DB Migration       | Flyway                                        | Versioned, repeatable migrations                    |
| Validation         | Jakarta Validation (Hibernate Validator)      | Declarative constraints                             |
| Mapping            | MapStruct                                     | Compile-time, type-safe DTO mapping                 |
| Async / Scheduling | Spring `@Async` + `@Scheduled`                | In-process async (upgradeable to Kafka)             |
| WebSocket          | Spring WebSocket + STOMP                      | Real-time booking notifications                     |
| API Docs           | SpringDoc OpenAPI 3 (Swagger UI)              | Auto-generated from annotations                     |
| Build              | Maven 3.9+                                    | Matches library ecosystem                           |

### Frontend

| Layer              | Technology                                  | Reason                                         |
|--------------------|---------------------------------------------|------------------------------------------------|
| Framework          | Angular 17+ (standalone components)         | Strong typing, DI, reactive forms              |
| Security           | `@benatti/ng-auth-lib` (latest npm)         | JWT token management, auth guards, interceptors|
| State Management   | NgRx (Signals-based)                        | Predictable state for complex booking flows    |
| UI Components      | Angular Material + TailwindCSS              | Rapid prototyping, responsive                  |
| Maps               | Google Maps JS SDK / Leaflet               | Geo-search visualization                       |
| HTTP               | Angular `HttpClient` + interceptors         | Centralized token injection                    |
| Real-time          | `@stomp/ng2-stompjs`                        | WebSocket / STOMP client                       |
| Build              | Angular CLI + esbuild                       | Fast builds, tree shaking                      |

### Infrastructure

| Component          | Technology            | Notes                                           |
|--------------------|-----------------------|-------------------------------------------------|
| Primary Database   | PostgreSQL 16+        | JSONB, PostGIS extension for geo queries        |
| Cache + Locks      | Redis 7+              | Distributed locks for Booking Engine            |
| File Storage       | AWS S3 / MinIO        | Business logos, photos                          |
| Email              | AWS SES / SendGrid    | Transactional emails (confirmations, reminders) |
| Containerization   | Docker + Compose      | Dev environment parity                          |
| CI/CD              | GitHub Actions        | Lint → Test → Build → Deploy pipeline          |

---

## 4. Security Layer — Auth Libraries

### 4.1 Backend: `benatti-auth-starter`

**Artifact:** `io.github.benatti-dev:benatti-auth-starter:1.0.1`  
**Internals:** Spring Security 6 + JJWT 0.12.3 + Spring Boot 3.3.x auto-configuration

#### Maven Dependency

```xml
<dependency>
    <groupId>io.github.benatti-dev</groupId>
    <artifactId>benatti-auth-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```

#### Required `application.yml` Configuration

```yaml
benatti:
  auth:
    secret: ${JWT_SECRET}          # min 256-bit base64-encoded key (from env var)
    expiration-ms: 3600000         # 1 hour access token
    refresh-expiration-ms: 604800000  # 7 days refresh token
    token-prefix: "Bearer "
    header-name: "Authorization"
```

#### Integration Pattern

```java
// The starter auto-configures:
// 1. JwtAuthenticationFilter — validates Bearer token on each request
// 2. JwtService — generate / validate / extract claims from tokens
// 3. SecurityFilterChain — with sensible defaults

// Your task: implement UserDetailsService to load user from DB
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}

// Expose auth endpoints — the starter provides the filter,
// you write the AuthController with login / register / refresh endpoints
```

#### RBAC — Custom Claims in JWT

```java
// When building the token, embed role as a claim:
// { "sub": "user@email.com", "role": "BUSINESS_OWNER", "businessId": 42 }
// The JwtAuthenticationFilter from the starter populates SecurityContext
// Use @PreAuthorize("hasRole('BUSINESS_OWNER')") on controller methods
```

---

### 4.2 Frontend: `@benatti/ng-auth-lib`

**Install:** `npm install @benatti/ng-auth-lib`

#### Module Setup (standalone)

```typescript
// app.config.ts
import { provideAuth } from '@benatti/ng-auth-lib';

export const appConfig: ApplicationConfig = {
  providers: [
    provideAuth({
      apiUrl: environment.apiUrl,
      loginEndpoint: '/api/auth/login',
      refreshEndpoint: '/api/auth/refresh',
      tokenKey: 'booker_token',
      refreshTokenKey: 'booker_refresh',
    }),
    // ... other providers
  ],
};
```

#### Usage Pattern

```typescript
// The library provides:
// - AuthService       — login(), logout(), isAuthenticated(), currentUser$
// - AuthGuard         — route protection
// - AuthInterceptor   — auto-injects Bearer token into every HttpClient request
// - hasRole() helper  — template / code role checks

// Route guard usage:
{
  path: 'business',
  canActivate: [AuthGuard],
  data: { roles: ['BUSINESS_OWNER', 'ADMIN'] },
  loadComponent: () => import('./business/dashboard/dashboard.component'),
}
```

---

### 4.3 Security Architecture Rules

```
1. Passwords: BCrypt (strength 12) — handled by Spring Security PasswordEncoder bean.
2. JWT Secret: injected from environment variable JWT_SECRET only — never hard-coded.
3. Refresh Tokens: stored in HttpOnly cookies OR in DB (choose: DB for revocation support).
4. CORS: whitelist exact frontend origins — no wildcard (*) in production.
5. Rate Limiting: Nginx limits /api/auth/login to 10 req/min per IP.
6. HTTPS: enforce TLS; HSTS header set on Nginx.
7. Input validation: all DTOs annotated with Jakarta Validation constraints.
8. SQL Injection: never use string concatenation in queries — JPA + named params only.
9. Audit Log: log all auth events (login, failed login, role change) to separate table.
```

---

## 5. Database Design

### 5.1 Core Design Decisions

| Requirement                         | Solution                                    |
|-------------------------------------|---------------------------------------------|
| Dynamic service attributes          | `JSONB` column `attributes` on `services`   |
| Dynamic working hours / schedule    | Separate `schedule_rules` table (EAV-light) |
| Full-text search                    | PostgreSQL `tsvector` + GIN index           |
| Geo queries (nearby businesses)     | PostGIS `GEOGRAPHY(POINT)` + GIST index     |
| Multi-tenancy isolation             | `business_id` foreign key on all tenant rows|

---

### 5.2 Entity-Relationship Schema

```sql
-- ═══════════════════════════════════════
-- USERS & AUTH
-- ═══════════════════════════════════════

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(30)  NOT NULL CHECK (role IN ('ADMIN','BUSINESS_OWNER','EMPLOYEE','CLIENT')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','PENDING')),
    avatar_url    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,     -- SHA-256 of raw token
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    ip_address  INET,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════
-- BUSINESSES & BRANCHES
-- ═══════════════════════════════════════

CREATE TABLE business_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,      -- 'beauty_salon', 'gym', 'medical'
    label       VARCHAR(100) NOT NULL,             -- Display label
    icon_url    TEXT,
    -- Defines which booking resource type is used:
    -- 'EMPLOYEE' | 'RESOURCE' | 'NONE'
    resource_type VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE'
);

CREATE TABLE businesses (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT       NOT NULL REFERENCES users(id),
    category_id     BIGINT       NOT NULL REFERENCES business_categories(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    logo_url        TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','REJECTED')),
    -- Custom fields per business type stored as JSONB:
    -- beauty salon: {"instagram":"@x", "brands":["L'oreal"]}
    -- gym: {"facilities":["pool","sauna"], "membership_types":["monthly","annual"]}
    meta            JSONB        NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE branches (
    id              BIGSERIAL PRIMARY KEY,
    business_id     BIGINT       NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    address         TEXT         NOT NULL,
    city            VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL DEFAULT 'UA',
    postal_code     VARCHAR(20),
    location        GEOGRAPHY(POINT, 4326),    -- PostGIS for geo queries
    phone           VARCHAR(30),
    email           VARCHAR(255),
    timezone        VARCHAR(60)  NOT NULL DEFAULT 'Europe/Kiev',
    is_primary      BOOLEAN      NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════
-- EMPLOYEES & RESOURCES
-- ═══════════════════════════════════════

CREATE TABLE employees (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      REFERENCES users(id),      -- NULL if not a platform user yet
    business_id   BIGINT      NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    branch_id     BIGINT      REFERENCES branches(id),
    display_name  VARCHAR(255) NOT NULL,
    bio           TEXT,
    avatar_url    TEXT,
    position      VARCHAR(100),
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Generic bookable resources (court, room, car lift, etc.)
CREATE TABLE resources (
    id            BIGSERIAL PRIMARY KEY,
    business_id   BIGINT      NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    branch_id     BIGINT      NOT NULL REFERENCES branches(id),
    name          VARCHAR(255) NOT NULL,           -- "Court #1", "Bay #3"
    resource_type VARCHAR(100) NOT NULL,           -- 'COURT', 'ROOM', 'EQUIPMENT'
    capacity      INT          NOT NULL DEFAULT 1,
    meta          JSONB        NOT NULL DEFAULT '{}',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ═══════════════════════════════════════
-- SERVICES CATALOG (Dynamic Attributes)
-- ═══════════════════════════════════════

-- Defines the "shape" of dynamic attributes for a business category
CREATE TABLE service_attribute_definitions (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT       NOT NULL REFERENCES business_categories(id),
    field_key       VARCHAR(100) NOT NULL,      -- 'hair_length', 'court_surface'
    field_label     VARCHAR(100) NOT NULL,
    field_type      VARCHAR(30)  NOT NULL       -- 'TEXT','NUMBER','SELECT','BOOLEAN','MULTI_SELECT'
                        CHECK (field_type IN ('TEXT','NUMBER','SELECT','BOOLEAN','MULTI_SELECT')),
    options         JSONB,                      -- For SELECT: ["short","medium","long"]
    is_required     BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order      INT          NOT NULL DEFAULT 0,
    UNIQUE (category_id, field_key)
);

CREATE TABLE services (
    id              BIGSERIAL PRIMARY KEY,
    business_id     BIGINT       NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    category_id     BIGINT       NOT NULL REFERENCES business_categories(id),
    duration_min    INT          NOT NULL,          -- Duration in minutes
    price           NUMERIC(10,2),                 -- NULL = price on request
    currency        VARCHAR(3)   NOT NULL DEFAULT 'UAH',
    -- Dynamic attributes: {"hair_length":"long","coloring":true}
    -- Validated against service_attribute_definitions at application layer
    attributes      JSONB        NOT NULL DEFAULT '{}',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- Full-text search vector (auto-updated via trigger)
    search_vector   TSVECTOR
);

-- Many-to-many: service ↔ employee (who can perform this service)
CREATE TABLE service_employees (
    service_id    BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    employee_id   BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, employee_id)
);

-- Many-to-many: service ↔ resource (which resources are used)
CREATE TABLE service_resources (
    service_id    BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    resource_id   BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, resource_id)
);

-- ═══════════════════════════════════════
-- SCHEDULING (Dynamic Working Hours)
-- ═══════════════════════════════════════

-- Recurring weekly schedule rules
CREATE TABLE schedule_rules (
    id              BIGSERIAL PRIMARY KEY,
    -- Applies to employee OR resource (one must be non-null)
    employee_id     BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    resource_id     BIGINT REFERENCES resources(id) ON DELETE CASCADE,
    branch_id       BIGINT NOT NULL REFERENCES branches(id),
    -- 0=Sunday, 1=Monday, ... 6=Saturday
    day_of_week     SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    is_working_day  BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_owner CHECK (
        (employee_id IS NOT NULL AND resource_id IS NULL) OR
        (employee_id IS NULL AND resource_id IS NOT NULL)
    )
);

-- One-off overrides: holidays, vacations, special hours
CREATE TABLE schedule_overrides (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT REFERENCES employees(id) ON DELETE CASCADE,
    resource_id     BIGINT REFERENCES resources(id) ON DELETE CASCADE,
    override_date   DATE NOT NULL,
    -- NULL start/end = full day off
    start_time      TIME,
    end_time        TIME,
    reason          VARCHAR(255),        -- 'holiday', 'vacation', 'special_event'
    CONSTRAINT chk_owner CHECK (
        (employee_id IS NOT NULL AND resource_id IS NULL) OR
        (employee_id IS NULL AND resource_id IS NOT NULL)
    )
);

-- Break times during a working day
CREATE TABLE schedule_breaks (
    id              BIGSERIAL PRIMARY KEY,
    schedule_rule_id BIGINT NOT NULL REFERENCES schedule_rules(id) ON DELETE CASCADE,
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL
);

-- ═══════════════════════════════════════
-- BOOKING ENGINE
-- ═══════════════════════════════════════

CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT       NOT NULL REFERENCES users(id),
    service_id      BIGINT       NOT NULL REFERENCES services(id),
    business_id     BIGINT       NOT NULL REFERENCES businesses(id),
    branch_id       BIGINT       NOT NULL REFERENCES branches(id),
    -- Only one of these is set (depends on category resource_type)
    employee_id     BIGINT       REFERENCES employees(id),
    resource_id     BIGINT       REFERENCES resources(id),
    start_time      TIMESTAMPTZ  NOT NULL,
    end_time        TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW')),
    client_note     TEXT,
    business_note   TEXT,
    -- Snapshot of price/duration at booking time (immutable after creation)
    price_snapshot  NUMERIC(10,2),
    duration_min    INT          NOT NULL,
    -- Dynamic service attributes selected by client at booking time
    selected_attributes JSONB   NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT no_overlap EXCLUDE USING GIST (
        employee_id WITH =,
        tstzrange(start_time, end_time) WITH &&
    ) WHERE (employee_id IS NOT NULL AND status NOT IN ('CANCELLED'))
);

-- Separate EXCLUDE constraint for resource-based bookings
-- (Added via Flyway migration after initial setup)

CREATE TABLE booking_cancellations (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT       NOT NULL REFERENCES bookings(id),
    cancelled_by    BIGINT       NOT NULL REFERENCES users(id),
    reason          TEXT,
    cancelled_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════
-- NOTIFICATIONS
-- ═══════════════════════════════════════

CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(50) NOT NULL,   -- 'BOOKING_CONFIRMED','BOOKING_REMINDER', etc.
    title           VARCHAR(255) NOT NULL,
    body            TEXT        NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    reference_id    BIGINT,               -- e.g. booking_id
    reference_type  VARCHAR(50),          -- 'BOOKING'
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════
-- INDEXES
-- ═══════════════════════════════════════

-- Geo index on branches
CREATE INDEX idx_branches_location ON branches USING GIST(location);

-- Full-text search on services
CREATE INDEX idx_services_search ON services USING GIN(search_vector);

-- Full-text search trigger
CREATE FUNCTION services_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('ukrainian', coalesce(NEW.name,'') || ' ' || coalesce(NEW.description,''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_services_search_vector
    BEFORE INSERT OR UPDATE ON services
    FOR EACH ROW EXECUTE FUNCTION services_search_vector_update();

-- Booking lookup indexes
CREATE INDEX idx_bookings_employee_time ON bookings(employee_id, start_time, end_time)
    WHERE status != 'CANCELLED';
CREATE INDEX idx_bookings_resource_time ON bookings(resource_id, start_time, end_time)
    WHERE status != 'CANCELLED';
CREATE INDEX idx_bookings_client ON bookings(client_id, start_time DESC);
CREATE INDEX idx_bookings_business ON bookings(business_id, start_time DESC);

-- JSONB index on services attributes for filtering
CREATE INDEX idx_services_attributes ON services USING GIN(attributes);

-- Business status index (admin moderation queries)
CREATE INDEX idx_businesses_status ON businesses(status);
```

---

## 6. System Modules

### Module 6.1 — Authentication & RBAC

**Package:** `com.booker.auth`

#### Responsibilities
- Register, login, logout, token refresh
- Password reset via email
- Role-Based Access Control: `ADMIN`, `BUSINESS_OWNER`, `EMPLOYEE`, `CLIENT`
- Audit logging of all auth events

#### Key Classes (AI instruction — generate these)

```
AuthController          POST /api/auth/register, /login, /refresh, /logout, /forgot-password, /reset-password
AuthService             Business logic: hash password, call JwtService (from benatti-auth-starter)
RefreshTokenService     Create, validate, revoke refresh tokens in DB
AuditLogService         Write to audit_logs table on auth events
AppUserDetailsService   Implements UserDetailsService — loads user from DB by email
SecurityConfig          @Configuration — SecurityFilterChain, CORS config, BCryptPasswordEncoder bean
```

#### DTO Contracts

```java
// RegisterRequest
record RegisterRequest(
    @Email String email,
    @Size(min = 8) String password,
    @NotBlank String fullName,
    String phone
) {}

// LoginRequest
record LoginRequest(@Email String email, @NotBlank String password) {}

// AuthResponse (returned on login and refresh)
record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
// Refresh token delivered via HttpOnly Set-Cookie header
```

#### RBAC Method Security

```java
// Enable in main config:
@EnableMethodSecurity(prePostEnabled = true)

// Usage on controllers:
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('BUSINESS_OWNER','ADMIN')")
@PreAuthorize("hasRole('CLIENT')")

// Custom expression to check ownership:
@PreAuthorize("@securityHelper.isBusinessOwner(authentication, #businessId)")
```

---

### Module 6.2 — Business Module

**Package:** `com.booker.business`

#### Responsibilities
- CRUD for businesses and branches
- Business registration workflow (PENDING → moderation → ACTIVE)
- Manage employees and resources per branch
- Upload and serve business media (avatar, photos via S3)

#### Key Classes

```
BusinessController      REST endpoints for business CRUD
BranchController        REST endpoints for branches
EmployeeController      REST endpoints for employees
ResourceController      REST endpoints for bookable resources
BusinessService         Core business logic, status transitions
BusinessRepository      JPA repository with custom queries
BusinessMapper          MapStruct mapper: Entity ↔ DTO
MediaUploadService      Upload to S3/MinIO, return CDN URL
```

#### Business Status Machine

```
PENDING ──(admin approve)──► ACTIVE
PENDING ──(admin reject)───► REJECTED
ACTIVE  ──(admin suspend)──► SUSPENDED
SUSPENDED ─(admin restore)─► ACTIVE
```

#### AI Instructions for this module:
1. `Business` entity has a `meta JSONB` field — map to `Map<String, Object>` in Java.
2. `Branch.location` is a PostGIS `GEOGRAPHY` — use `hibernate-spatial` dependency and `Point` type.
3. `BusinessController` must check: only the `BUSINESS_OWNER` whose `id` matches `business.ownerId` can update (or ADMIN).
4. When business status changes to `ACTIVE`, publish a `BusinessActivatedEvent` (Spring ApplicationEvent).

---

### Module 6.3 — Catalog Module (Dynamic Services)

**Package:** `com.booker.catalog`

#### Responsibilities
- Define service attribute schemas per business category (admin-controlled)
- CRUD for services within a business
- Validate service `attributes JSONB` against `service_attribute_definitions`
- Assign employees/resources to services

#### Key Classes

```
ServiceController           REST endpoints for service CRUD
AttributeDefinitionController  Admin endpoints for attribute schemas
ServiceAttributeValidator   Validates JSONB against definitions at runtime
ServiceRepository           JPA + native queries for JSONB filtering
```

#### Dynamic Attribute Validation Flow

```
Client sends POST /api/businesses/{id}/services
  → body: { name, duration, attributes: { "hair_length": "long", "coloring": true } }
    → ServiceAttributeValidator.validate(categoryId, attributes)
       → Loads definitions for category from DB
       → Checks required fields present
       → Checks type matches (string, boolean, valid option)
       → Throws ValidationException with field errors if invalid
    → Service saved with JSONB attributes
```

#### AI Instructions for this module:
1. `ServiceAttributeValidator` must be a Spring `@Component` injected into `ServiceService`.
2. Cache `service_attribute_definitions` in Spring Cache (`@Cacheable`) — these change rarely.
3. When a definition is updated by admin, evict the cache with `@CacheEvict`.
4. Use `@JsonRawValue` / `@JsonDeserialize` on `attributes` field for proper JSONB handling.

---

### Module 6.4 — Booking Engine

**Package:** `com.booker.booking`

#### Responsibilities
- Generate available time slots for a service/employee/resource on a given date
- Validate slot availability before booking (race condition prevention)
- Create, confirm, cancel bookings
- Emit booking events → notifications module

#### Race Condition Prevention Strategy

```
Strategy: PostgreSQL Advisory Lock + EXCLUDE constraint

1. Client requests slot → SlotGeneratorService computes available slots (no lock yet).
2. Client submits booking → BookingService:
   a. Acquires PostgreSQL advisory lock: pg_try_advisory_xact_lock(employee_id)
   b. Re-checks slot availability within the lock (SELECT FOR UPDATE)
   c. If available: INSERT booking
   d. Lock released automatically on transaction commit
   e. EXCLUDE constraint acts as final safety net (raises exception on concurrent insert)
3. If lock not acquired: return HTTP 409 Conflict with retry guidance.
```

> **Redis alternative:** Use `SET NX PX` (set-if-not-exists with TTL) on key  
> `lock:booking:{employee_id}:{date}:{slot}` for distributed lock if scaling horizontally.

#### Slot Generation Algorithm

```java
// AI: implement this algorithm
// Input: employeeId (or resourceId), serviceId, date
// Output: List<LocalTime> availableStartTimes

1. Load schedule_rules for employee + dayOfWeek(date)
   → If no rule or is_working_day=false → return empty list
2. Load schedule_overrides for employee + date
   → If full-day off → return empty list
   → If partial override: adjust working window
3. Load schedule_breaks for the schedule_rule
4. Load existing bookings for employee on that date (status != CANCELLED)
5. Build occupied ranges from bookings
6. Starting from workStart, step through by slotInterval (e.g. 15 min):
   → Slot is available if:
      - slotStart >= workStart AND slotEnd <= workEnd
      - Does not overlap any break
      - Does not overlap any existing booking
      - slotEnd = slotStart + service.durationMin
7. Return list of available LocalTime start times
```

#### Key Classes

```
BookingController       POST /api/bookings, GET, PATCH /cancel
SlotController          GET /api/slots?serviceId=&employeeId=&date=
BookingService          Booking creation with advisory lock
SlotGeneratorService    Algorithm above — pure computation, no side effects
BookingValidator        Pre-checks: business active, service active, employee assigned to service
BookingRepository       Custom queries for overlap detection
```

---

### Module 6.5 — Search Module

**Package:** `com.booker.search`

#### Responsibilities
- Find businesses by location, category, name (full-text), rating
- Find available services with filters
- Pagination + cursor-based for large result sets

#### Search Query Examples (native SQL)

```sql
-- Find businesses near a point, within radius (km), filtered by category
SELECT b.id, b.name, b.meta,
       ST_Distance(br.location, ST_MakePoint(:lng, :lat)::geography) AS distance_m
FROM businesses b
JOIN branches br ON br.business_id = b.id AND br.status = 'ACTIVE'
WHERE b.status = 'ACTIVE'
  AND (:categoryId IS NULL OR b.category_id = :categoryId)
  AND ST_DWithin(br.location, ST_MakePoint(:lng, :lat)::geography, :radiusMeters)
ORDER BY distance_m ASC
LIMIT :pageSize OFFSET :offset;

-- Full-text search on services
SELECT s.* FROM services s
WHERE s.is_active = true
  AND s.business_id = :businessId
  AND s.search_vector @@ plainto_tsquery('ukrainian', :query)
ORDER BY ts_rank(s.search_vector, plainto_tsquery('ukrainian', :query)) DESC;
```

#### Key Classes

```
SearchController        GET /api/search/businesses, /api/search/services
SearchService           Orchestrates geo + text + filter queries
SearchRequest           DTO: lat, lng, radiusKm, categoryId, query, priceMin, priceMax, page, size
SearchResultDTO         Business summary with distance, rating, next available slot hint
```

---

### Module 6.6 — Notification Module

**Package:** `com.booker.notification`

#### Responsibilities
- Send email notifications (booking confirmation, reminders, cancellations)
- Store in-app notifications in DB
- Real-time push via WebSocket / STOMP

#### Event Listener Pattern

```java
// AI: implement event listeners — no direct coupling to booking module
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final EmailService emailService;
    private final InAppNotificationService inAppService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    @Async
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        // 1. Send email to client
        emailService.sendBookingConfirmation(event.booking());
        // 2. Store in-app notification
        inAppService.create(event.booking().clientId(), "BOOKING_CONFIRMED", ...);
        // 3. Push to WebSocket topic
        messagingTemplate.convertAndSendToUser(
            event.booking().clientId().toString(),
            "/queue/notifications",
            NotificationDTO.from(event.booking())
        );
    }
}
```

#### Email Templates (Thymeleaf)
- `booking-confirmation.html`
- `booking-reminder.html` (sent 24h before)
- `booking-cancellation.html`
- `password-reset.html`
- `business-approved.html`

---

### Module 6.7 — Analytics Module

**Package:** `com.booker.analytics`

#### Responsibilities (read-only, no writes)
- Business owner dashboard: bookings per day, revenue, top services, employee performance
- Admin dashboard: total businesses, users, bookings, revenue (platform fee)

#### Key Queries

```sql
-- Business: bookings per day for last 30 days
SELECT DATE(start_time AT TIME ZONE b.timezone) AS day,
       COUNT(*) AS total_bookings,
       SUM(price_snapshot) AS revenue
FROM bookings bk
JOIN branches br ON bk.branch_id = br.id
JOIN businesses b ON bk.business_id = b.id
WHERE bk.business_id = :businessId
  AND bk.status = 'COMPLETED'
  AND bk.start_time >= NOW() - INTERVAL '30 days'
GROUP BY day
ORDER BY day;
```

---

## 7. API Specification

### Base URL: `/api/v1`

### 7.1 Authentication

| Method | Endpoint                    | Role   | Description                   |
|--------|-----------------------------|--------|-------------------------------|
| POST   | `/auth/register`            | Public | Register new client account   |
| POST   | `/auth/login`               | Public | Login, returns JWT            |
| POST   | `/auth/refresh`             | Public | Refresh access token          |
| POST   | `/auth/logout`              | Auth   | Revoke refresh token          |
| POST   | `/auth/forgot-password`     | Public | Send password reset email     |
| POST   | `/auth/reset-password`      | Public | Reset with token from email   |

### 7.2 Business Management

| Method | Endpoint                              | Role              | Description                    |
|--------|---------------------------------------|-------------------|--------------------------------|
| POST   | `/businesses`                         | BUSINESS_OWNER    | Register new business          |
| GET    | `/businesses/{id}`                    | Public            | Get business details           |
| PUT    | `/businesses/{id}`                    | OWNER/ADMIN       | Update business                |
| PATCH  | `/businesses/{id}/status`             | ADMIN             | Approve/reject/suspend         |
| GET    | `/businesses/{id}/branches`           | Public            | List branches                  |
| POST   | `/businesses/{id}/branches`           | OWNER             | Add branch                     |
| GET    | `/businesses/{id}/employees`          | OWNER/EMPLOYEE    | List employees                 |
| POST   | `/businesses/{id}/employees`          | OWNER             | Add employee                   |
| GET    | `/businesses/{id}/resources`          | OWNER             | List bookable resources        |
| POST   | `/businesses/{id}/resources`          | OWNER             | Add resource                   |
| GET    | `/businesses/{id}/analytics`          | OWNER             | Analytics dashboard data       |

### 7.3 Catalog (Services)

| Method | Endpoint                                   | Role   | Description                        |
|--------|--------------------------------------------|--------|------------------------------------|
| POST   | `/businesses/{id}/services`                | OWNER  | Create service                     |
| GET    | `/businesses/{id}/services`                | Public | List services (with filters)       |
| GET    | `/services/{id}`                           | Public | Service detail                     |
| PUT    | `/services/{id}`                           | OWNER  | Update service                     |
| DELETE | `/services/{id}`                           | OWNER  | Deactivate service                 |
| GET    | `/categories`                              | Public | List business categories           |
| GET    | `/categories/{id}/attribute-definitions`   | Public | Attribute schema for category      |
| POST   | `/admin/categories`                        | ADMIN  | Create category                    |
| POST   | `/admin/categories/{id}/attribute-definitions` | ADMIN | Add attribute definition      |

### 7.4 Booking Engine

| Method | Endpoint                      | Role        | Description                              |
|--------|-------------------------------|-------------|------------------------------------------|
| GET    | `/slots`                      | Public      | Get available slots for service/employee |
| POST   | `/bookings`                   | CLIENT      | Create booking                           |
| GET    | `/bookings/{id}`              | Auth        | Get booking detail                       |
| GET    | `/bookings/my`                | CLIENT      | Client's booking history                 |
| GET    | `/businesses/{id}/bookings`   | OWNER/EMPL  | Business bookings (with date filter)     |
| PATCH  | `/bookings/{id}/confirm`      | OWNER/EMPL  | Confirm pending booking                  |
| PATCH  | `/bookings/{id}/cancel`       | Auth        | Cancel booking (client or business)      |
| PATCH  | `/bookings/{id}/complete`     | OWNER/EMPL  | Mark booking as completed                |

**GET `/slots` Query Parameters:**
```
serviceId  REQUIRED  Long   
employeeId OPTIONAL  Long   (required if category resource_type = EMPLOYEE)
resourceId OPTIONAL  Long   (required if category resource_type = RESOURCE)
date       REQUIRED  String (ISO 8601: 2026-06-01)
```

**POST `/bookings` Request Body:**
```json
{
  "serviceId": 42,
  "employeeId": 7,
  "branchId": 3,
  "startTime": "2026-06-01T10:00:00+03:00",
  "clientNote": "Please use allergen-free products",
  "selectedAttributes": {
    "hair_length": "long",
    "coloring": true
  }
}
```

### 7.5 Search

| Method | Endpoint              | Role   | Description                              |
|--------|-----------------------|--------|------------------------------------------|
| GET    | `/search/businesses`  | Public | Geo + category + text search             |
| GET    | `/search/services`    | Public | Service search within a business         |

**GET `/search/businesses` Query Parameters:**
```
lat        OPTIONAL  Double  Latitude
lng        OPTIONAL  Double  Longitude
radiusKm   OPTIONAL  Int     Default: 10
categoryId OPTIONAL  Long
query      OPTIONAL  String  Full-text search term
page       OPTIONAL  Int     Default: 0
size       OPTIONAL  Int     Default: 20, Max: 50
```

### 7.6 Notifications

| Method | Endpoint                          | Role  | Description                    |
|--------|-----------------------------------|-------|--------------------------------|
| GET    | `/notifications`                  | Auth  | Get user notifications         |
| PATCH  | `/notifications/{id}/read`        | Auth  | Mark notification as read      |
| PATCH  | `/notifications/read-all`         | Auth  | Mark all as read               |

**WebSocket endpoint:** `ws://.../ws` with STOMP  
**Subscribe:** `/user/queue/notifications`

### 7.7 Admin

| Method | Endpoint                          | Role  | Description                       |
|--------|-----------------------------------|-------|-----------------------------------|
| GET    | `/admin/businesses`               | ADMIN | List all businesses + filter       |
| GET    | `/admin/users`                    | ADMIN | List all users                    |
| PATCH  | `/admin/users/{id}/status`        | ADMIN | Suspend/restore user              |
| GET    | `/admin/analytics/overview`       | ADMIN | Platform-wide statistics          |
| GET    | `/admin/audit-logs`               | ADMIN | Auth audit log                    |

---

## 8. Development Phases (Roadmap)

> Each phase is a self-contained coding task for the AI. Complete and test one phase before starting the next.

---

### Phase 1 — Project Skeleton & Auth (Week 1–2)

**Goal:** Working backend with JWT auth, role management, and Angular shell with login/register.

#### Backend Tasks
```
[TASK 1.1] Initialize Spring Boot 3.3.x Maven project
  - Modules: auth, shared
  - Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa,
    spring-boot-starter-validation, spring-boot-starter-websocket,
    benatti-auth-starter:1.0.1, postgresql, flyway-core, lombok, mapstruct,
    springdoc-openapi-starter-webmvc-ui, hibernate-spatial, jts-core
  - Configure application.yml (dev profile) with PostgreSQL and JWT settings

[TASK 1.2] Flyway V1 migration: create users, refresh_tokens, audit_logs tables

[TASK 1.3] Implement User entity + UserRepository

[TASK 1.4] Implement AppUserDetailsService (UserDetailsService)

[TASK 1.5] Configure SecurityConfig:
  - SecurityFilterChain: permit /api/auth/**, /api/search/**, /api/categories/**,
    /api/businesses/{id}/services (GET), swagger-ui
  - All other endpoints require authentication
  - CORS: allow http://localhost:4200 in dev
  - Disable CSRF (stateless JWT API)
  - BCryptPasswordEncoder bean (strength 12)

[TASK 1.6] Implement AuthController + AuthService + RefreshTokenService:
  - POST /api/auth/register: validate, hash password, save user, return AuthResponse
  - POST /api/auth/login: authenticate, generate JWT (with role claim), set refresh token cookie
  - POST /api/auth/refresh: validate refresh token, issue new access token
  - POST /api/auth/logout: revoke refresh token from DB
  - POST /api/auth/forgot-password: generate reset token, send email (stub email in phase 1)
  - POST /api/auth/reset-password: validate token, update password

[TASK 1.7] AuditLogService: log LOGIN, LOGOUT, FAILED_LOGIN, REGISTER, PASSWORD_RESET

[TASK 1.8] Write integration tests: register → login → access protected endpoint → refresh → logout
```

#### Frontend Tasks
```
[TASK 1.9] Initialize Angular 17 project (standalone, routing, SSR=false)
  - Install: @benatti/ng-auth-lib, @angular/material, tailwindcss, @stomp/ng2-stompjs

[TASK 1.10] Configure provideAuth() in app.config.ts pointing to localhost:8080

[TASK 1.11] Create AuthModule with:
  - LoginComponent (reactive form: email, password)
  - RegisterComponent (reactive form: email, password, fullName, phone)
  - Integrate @benatti/ng-auth-lib AuthService for login/register calls
  - Store token, redirect to /dashboard on success

[TASK 1.12] Create AppShellComponent with:
  - Navbar (shows login/register when unauthenticated, user menu when authenticated)
  - Route guard using AuthGuard from @benatti/ng-auth-lib
  - Placeholder routes: /login, /register, /dashboard, /admin, /business

[TASK 1.13] HTTP interceptor from @benatti/ng-auth-lib auto-injects Bearer token
```

---

### Phase 2 — Business & Catalog (Week 3–4)

**Goal:** Business registration, branch management, service catalog with dynamic attributes.

#### Backend Tasks
```
[TASK 2.1] Flyway V2 migration: business_categories, businesses, branches,
  service_attribute_definitions, services, service_employees, service_resources tables

[TASK 2.2] Implement Business module:
  - BusinessEntity, BranchEntity with PostGIS Point type (hibernate-spatial)
  - BusinessController, BusinessService, BusinessRepository, BusinessMapper
  - Endpoints: POST /businesses, GET /businesses/{id}, PUT, PATCH /status

[TASK 2.3] Business status transition logic in BusinessService:
  - Only PENDING → ACTIVE/REJECTED by ADMIN
  - Publish BusinessActivatedEvent on ACTIVE

[TASK 2.4] Implement Branch CRUD under /businesses/{id}/branches

[TASK 2.5] Implement admin category management:
  - BusinessCategoryController (ADMIN only)
  - AttributeDefinitionController: GET /categories/{id}/attribute-definitions (Public),
    POST (ADMIN)

[TASK 2.6] Implement ServiceAttributeValidator:
  - Load definitions from DB (cached with @Cacheable("attributeDefs"))
  - Validate JSONB attributes map against definitions

[TASK 2.7] Implement Service CRUD:
  - ServiceController, ServiceService, ServiceRepository, ServiceMapper
  - On create/update: call ServiceAttributeValidator
  - Search vector updated via DB trigger (just INSERT, trigger handles it)
```

#### Frontend Tasks
```
[TASK 2.8] Business Owner dashboard layout with sidenav

[TASK 2.9] BusinessRegistrationComponent: multi-step form
  - Step 1: Basic info (name, category, description)
  - Step 2: Branch info (address, city, select location on map)
  - Step 3: Review + submit

[TASK 2.10] ServiceListComponent + ServiceFormComponent:
  - Load attribute definitions for business category
  - Render dynamic form fields based on field_type (text, select, boolean, multi-select)
  - Submit with JSONB attributes

[TASK 2.11] Admin panel: pending businesses list with approve/reject buttons
```

---

### Phase 3 — Scheduling & Booking Engine (Week 5–6)

**Goal:** Slot generation, conflict-free booking creation, booking management.

#### Backend Tasks
```
[TASK 3.1] Flyway V3 migration: employees, resources, schedule_rules, schedule_overrides,
  schedule_breaks, bookings, booking_cancellations tables
  - Add btree_gist extension (required for EXCLUDE constraint)
  - Add EXCLUDE constraint on bookings

[TASK 3.2] Implement Employee and Resource CRUD under businesses/{id}/employees, /resources

[TASK 3.3] Implement Schedule management:
  - ScheduleController: GET/PUT /employees/{id}/schedule (returns weekly rules + overrides)
  - ScheduleService: save rules, overrides, breaks

[TASK 3.4] Implement SlotGeneratorService:
  - Method: List<LocalTime> getAvailableSlots(Long entityId, EntityType, Long serviceId, LocalDate)
  - Implements the algorithm described in Module 6.4
  - Pure computation, no DB writes — unit-testable

[TASK 3.5] Implement SlotController: GET /slots (calls SlotGeneratorService)

[TASK 3.6] Implement BookingService:
  - createBooking(): advisory lock → re-check → insert
  - cancelBooking(): status → CANCELLED, check cancellation policy (24h rule)
  - confirmBooking(), completeBooking()
  - Publish BookingConfirmedEvent, BookingCancelledEvent

[TASK 3.7] Implement BookingController with all endpoints from 7.4
  - Role checks: client can only cancel own bookings,
    owner can cancel only bookings in their business

[TASK 3.8] Write concurrent test: 20 simultaneous booking requests for the same slot
  → only 1 succeeds, 19 receive HTTP 409
```

#### Frontend Tasks
```
[TASK 3.9] Public booking flow (4 screens):
  1. BusinessDetailPage: info, services list, employee/resource selector
  2. SlotPickerComponent: date picker → call GET /slots → display time grid
  3. BookingConfirmComponent: review details, dynamic attribute form, submit
  4. BookingSuccessComponent: confirmation number, add to calendar link

[TASK 3.10] Client booking history: GET /bookings/my → list with status badges, cancel button

[TASK 3.11] Business owner booking calendar: week view using FullCalendar library
  - Color-coded by status
  - Click booking → detail modal with confirm/complete/cancel actions

[TASK 3.12] Schedule management UI:
  - Weekly grid to set working hours per employee
  - Override calendar for holidays / vacations
```

---

### Phase 4 — Search, Notifications & Real-Time (Week 7–8)

**Goal:** Full geo-search, email notifications, in-app real-time notifications.

#### Backend Tasks
```
[TASK 4.1] Enable PostGIS extension in Flyway (V4 migration: CREATE EXTENSION IF NOT EXISTS postgis)

[TASK 4.2] Implement SearchService with native queries:
  - businessSearch(): geo + category + full-text combined
  - serviceSearch(): full-text on search_vector

[TASK 4.3] Implement SearchController: GET /search/businesses, /search/services
  - Validate: if lat/lng provided, require both
  - Default page size 20, max 50

[TASK 4.4] Configure JavaMailSender (SES or SendGrid via SMTP):
  - Add Thymeleaf templates for 5 email types
  - EmailService: send HTML emails with TemplateEngine

[TASK 4.5] Configure Spring WebSocket + STOMP:
  - WebSocketConfig: endpoint /ws, broker /topic, /queue
  - JWT authentication for WebSocket (configure HandshakeInterceptor)

[TASK 4.6] Implement NotificationModule:
  - BookingNotificationListener: @EventListener for BookingConfirmedEvent etc.
  - InAppNotificationService: persist to notifications table
  - NotificationController: GET /notifications, PATCH /read
  - SimpMessagingTemplate push to /user/queue/notifications

[TASK 4.7] Implement 24h booking reminder:
  - @Scheduled(cron = "0 0 8 * * *"): find bookings 24h from now, send reminders
```

#### Frontend Tasks
```
[TASK 4.8] HomeSearchComponent:
  - Search bar + category grid + "Use my location" button
  - Map view with business markers (Leaflet / Google Maps)
  - Distance displayed on business cards

[TASK 4.9] SearchResultsComponent:
  - Filter panel: category, radius slider, price range
  - Infinite scroll OR pagination
  - Toggle list/map view

[TASK 4.10] WebSocket setup with @stomp/ng2-stompjs:
  - Connect on app init after authentication
  - Subscribe /user/queue/notifications
  - Show notification badge count in navbar

[TASK 4.11] NotificationCenterComponent:
  - Dropdown list of in-app notifications
  - Click → navigate to relevant booking
  - Mark as read on open
```

---

### Phase 5 — Analytics & Admin Panel (Week 9–10)

**Goal:** Complete admin panel and business analytics dashboard.

#### Backend Tasks
```
[TASK 5.1] Implement AnalyticsController:
  - GET /businesses/{id}/analytics?from=&to= → aggregate queries
  - GET /admin/analytics/overview → platform stats

[TASK 5.2] Implement AdminController:
  - GET /admin/businesses?status=&page=&size= → paginated list
  - GET /admin/users?role=&status=&page= → user management
  - PATCH /admin/users/{id}/status
  - GET /admin/audit-logs?userId=&from=&to=
```

#### Frontend Tasks
```
[TASK 5.3] Business Owner Analytics Dashboard:
  - Line chart: bookings per day (Chart.js / ngx-charts)
  - Bar chart: revenue per service
  - KPI cards: total bookings, total revenue, cancellation rate
  - Employee performance table

[TASK 5.4] Admin Panel:
  - Businesses table with status filter + approve/reject actions
  - Users table with suspend/restore
  - Platform overview KPIs
  - Audit log viewer with filters
```

---

### Phase 6 — Hardening, Testing & Documentation (Week 11–12)

**Goal:** Production-ready quality gate.

```
[TASK 6.1] Write unit tests: SlotGeneratorService (100% coverage), ServiceAttributeValidator,
  BookingService, AuthService

[TASK 6.2] Write integration tests (Testcontainers + PostgreSQL):
  - Full booking flow: register → search → book → confirm → complete
  - Concurrent booking race condition test
  - Auth: register, login, refresh, logout

[TASK 6.3] OpenAPI documentation: verify all endpoints documented, add examples

[TASK 6.4] Security audit:
  - Ensure no endpoint missing @PreAuthorize where needed
  - Verify CORS config (no wildcards)
  - Check all user inputs validated with Jakarta Validation
  - Verify JWT secret not logged

[TASK 6.5] Performance:
  - Verify all DB indexes created (run EXPLAIN ANALYZE on key queries)
  - Add Spring Cache on: attribute definitions, business categories
  - Configure HikariCP pool: maxPoolSize=20, connectionTimeout=30s

[TASK 6.6] Docker setup:
  - Dockerfile for Spring Boot (multi-stage: maven build → JRE 21 slim)
  - docker-compose.yml: app, postgresql (with PostGIS image), redis
  - Environment variables: JWT_SECRET, DB_URL, DB_USER, DB_PASS, SES_KEY

[TASK 6.7] Finalize Angular production build:
  - Environment configs for dev / prod
  - Lazy-load all feature routes (auth, business, admin, booking)
  - Run ng build --configuration production → verify bundle size < 500KB initial
```

---

### Phase 7 — Scalability Preparation (Future)

```
When load requires horizontal scaling:

[TASK 7.1] Extract Booking Engine to standalone microservice:
  - Own DB schema (bookings + schedules tables migrated)
  - Communicate via REST with other modules

[TASK 7.2] Replace advisory locks with Redis distributed locks (Redisson)
  - Required when Booking Engine runs on multiple instances

[TASK 7.3] Add Kafka for async events:
  - Replace Spring ApplicationEvents with Kafka topics
  - Topics: booking.confirmed, booking.cancelled, business.activated
  - Notification module becomes Kafka consumer

[TASK 7.4] Add Elasticsearch for search:
  - Sync services and businesses to ES index
  - Replace PostgreSQL full-text search with ES queries

[TASK 7.5] Mobile API: verify all endpoints return mobile-friendly payloads
  - Add React Native / Flutter client (new project)
```

---

## 9. Non-Functional Requirements

### Performance Targets

| Operation                 | Target Latency (p95) |
|---------------------------|----------------------|
| Search businesses         | < 200ms              |
| Get available slots       | < 100ms              |
| Create booking            | < 500ms (with lock)  |
| Login                     | < 300ms              |
| Page load (Angular SPA)   | < 2s first load      |

### Security Checklist

- [ ] JWT secret: min 256-bit, from environment variable only
- [ ] Passwords: BCrypt strength 12
- [ ] Refresh tokens: stored as SHA-256 hash in DB
- [ ] All auth events: logged in `audit_logs`
- [ ] Rate limiting: 10 req/min on `/auth/login` per IP
- [ ] HTTPS enforced; HSTS header enabled
- [ ] CORS: exact origin whitelist
- [ ] No sensitive data in JWT payload (no passwords, no PII beyond email+role)
- [ ] Input validation on all DTO fields
- [ ] SQL: parameterized queries only (JPA + named params)
- [ ] File uploads: validate MIME type + size limit (5MB) on S3 upload

### Project Structure (Backend)

```
booker-backend/
├── src/main/java/com/booker/
│   ├── shared/                    # Cross-module DTOs, exceptions, config
│   │   ├── config/                # SecurityConfig, WebSocketConfig, CacheConfig
│   │   ├── exception/             # GlobalExceptionHandler, ApiError
│   │   └── event/                 # Base event classes
│   ├── auth/                      # Module 6.1
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   ├── business/                  # Module 6.2
│   ├── catalog/                   # Module 6.3
│   ├── booking/                   # Module 6.4
│   ├── search/                    # Module 6.5
│   ├── notification/              # Module 6.6
│   └── analytics/                 # Module 6.7
├── src/main/resources/
│   ├── db/migration/              # Flyway scripts V1__*.sql, V2__*.sql, ...
│   ├── templates/email/           # Thymeleaf email templates
│   └── application.yml
└── pom.xml
```

### Project Structure (Frontend)

```
booker-frontend/
├── src/app/
│   ├── core/                      # Guards, interceptors, services (singleton)
│   ├── shared/                    # Reusable UI components
│   ├── features/
│   │   ├── auth/                  # Login, Register, Password Reset
│   │   ├── home/                  # Search, home page
│   │   ├── business/              # Business detail, booking flow
│   │   ├── dashboard/             # Client booking history
│   │   ├── business-admin/        # Owner: services, schedule, analytics
│   │   └── admin/                 # Platform admin panel
│   ├── app.routes.ts
│   └── app.config.ts              # provideAuth(), provideRouter(), provideHttpClient()
└── angular.json
```

---
