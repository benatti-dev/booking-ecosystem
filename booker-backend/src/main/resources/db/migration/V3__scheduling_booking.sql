-- ═══════════════════════════════════════════════════════════════════
-- V3 — Scheduling & Booking Schema
-- Tables: employees, resources, service_employees, service_resources,
--         schedule_rules, schedule_overrides, schedule_breaks,
--         bookings, booking_cancellations
-- ═══════════════════════════════════════════════════════════════════

-- Required for EXCLUDE constraint on bookings
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ── Employees ────────────────────────────────────────────────────
CREATE TABLE employees
(
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       REFERENCES users (id),   -- NULL if not a platform user yet
    business_id  BIGINT       NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    branch_id    BIGINT       REFERENCES branches (id),
    display_name VARCHAR(255) NOT NULL,
    bio          TEXT,
    avatar_url   TEXT,
    position     VARCHAR(100),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_business ON employees (business_id);
CREATE INDEX idx_employees_branch   ON employees (branch_id);
CREATE INDEX idx_employees_user     ON employees (user_id);

-- ── Resources (bookable: court, room, car lift, etc.) ───────────
CREATE TABLE resources
(
    id            BIGSERIAL    PRIMARY KEY,
    business_id   BIGINT       NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    branch_id     BIGINT       NOT NULL REFERENCES branches (id),
    name          VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,  -- 'COURT', 'ROOM', 'EQUIPMENT'
    capacity      INT          NOT NULL DEFAULT 1,
    meta          JSONB        NOT NULL DEFAULT '{}',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_resources_business ON resources (business_id);
CREATE INDEX idx_resources_branch   ON resources (branch_id);

-- ── Service ↔ Employee (M2M) ──────────────────────────────────
CREATE TABLE service_employees
(
    service_id  BIGINT NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    employee_id BIGINT NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, employee_id)
);

-- ── Service ↔ Resource (M2M) ─────────────────────────────────
CREATE TABLE service_resources
(
    service_id  BIGINT NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL REFERENCES resources (id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, resource_id)
);

-- ── Schedule Rules (recurring weekly) ──────────────────────────
CREATE TABLE schedule_rules
(
    id             BIGSERIAL PRIMARY KEY,
    employee_id    BIGINT    REFERENCES employees (id) ON DELETE CASCADE,
    resource_id    BIGINT    REFERENCES resources (id) ON DELETE CASCADE,
    branch_id      BIGINT    NOT NULL REFERENCES branches (id),
    -- 0=Sunday, 1=Monday, ... 6=Saturday
    day_of_week    SMALLINT  NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time     TIME      NOT NULL,
    end_time       TIME      NOT NULL,
    is_working_day BOOLEAN   NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_schedule_owner CHECK (
        (employee_id IS NOT NULL AND resource_id IS NULL) OR
        (employee_id IS NULL AND resource_id IS NOT NULL)
    )
);

CREATE INDEX idx_schedule_rules_employee ON schedule_rules (employee_id, day_of_week);
CREATE INDEX idx_schedule_rules_resource ON schedule_rules (resource_id, day_of_week);

-- ── Schedule Overrides (one-off: holidays, vacations) ──────────
CREATE TABLE schedule_overrides
(
    id            BIGSERIAL PRIMARY KEY,
    employee_id   BIGINT    REFERENCES employees (id) ON DELETE CASCADE,
    resource_id   BIGINT    REFERENCES resources (id) ON DELETE CASCADE,
    override_date DATE      NOT NULL,
    -- NULL start/end = full day off
    start_time    TIME,
    end_time      TIME,
    reason        VARCHAR(255),  -- 'holiday', 'vacation', 'special_event'
    CONSTRAINT chk_override_owner CHECK (
        (employee_id IS NOT NULL AND resource_id IS NULL) OR
        (employee_id IS NULL AND resource_id IS NOT NULL)
    )
);

CREATE INDEX idx_schedule_overrides_employee ON schedule_overrides (employee_id, override_date);
CREATE INDEX idx_schedule_overrides_resource ON schedule_overrides (resource_id, override_date);

-- ── Schedule Breaks ───────────────────────────────────────────
CREATE TABLE schedule_breaks
(
    id               BIGSERIAL PRIMARY KEY,
    schedule_rule_id BIGINT    NOT NULL REFERENCES schedule_rules (id) ON DELETE CASCADE,
    start_time       TIME      NOT NULL,
    end_time         TIME      NOT NULL
);

CREATE INDEX idx_schedule_breaks_rule ON schedule_breaks (schedule_rule_id);

-- ── Bookings ─────────────────────────────────────────────────────
CREATE TABLE bookings
(
    id                  BIGSERIAL      PRIMARY KEY,
    client_id           BIGINT         NOT NULL REFERENCES users (id),
    service_id          BIGINT         NOT NULL REFERENCES services (id),
    business_id         BIGINT         NOT NULL REFERENCES businesses (id),
    branch_id           BIGINT         NOT NULL REFERENCES branches (id),
    -- Only one of these is set (depends on category resource_type)
    employee_id         BIGINT         REFERENCES employees (id),
    resource_id         BIGINT         REFERENCES resources (id),
    start_time          TIMESTAMPTZ    NOT NULL,
    end_time            TIMESTAMPTZ    NOT NULL,
    status              VARCHAR(30)    NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    client_note         TEXT,
    business_note       TEXT,
    -- Snapshot at booking time (immutable)
    price_snapshot      NUMERIC(10, 2),
    duration_min        INT            NOT NULL,
    selected_attributes JSONB          NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    -- Prevent overlapping bookings for same employee
    EXCLUDE USING GIST (
        employee_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    ) WHERE (employee_id IS NOT NULL AND status NOT IN ('CANCELLED', 'NO_SHOW'))
);

-- Separate EXCLUDE constraint for resource-based bookings
ALTER TABLE bookings ADD CONSTRAINT bookings_no_resource_overlap
    EXCLUDE USING GIST (
        resource_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    ) WHERE (resource_id IS NOT NULL AND status NOT IN ('CANCELLED', 'NO_SHOW'));

CREATE INDEX idx_bookings_client       ON bookings (client_id, start_time DESC);
CREATE INDEX idx_bookings_business     ON bookings (business_id, start_time DESC);
CREATE INDEX idx_bookings_employee     ON bookings (employee_id, start_time, end_time)
    WHERE status NOT IN ('CANCELLED', 'NO_SHOW');
CREATE INDEX idx_bookings_resource     ON bookings (resource_id, start_time, end_time)
    WHERE status NOT IN ('CANCELLED', 'NO_SHOW');
CREATE INDEX idx_bookings_status       ON bookings (status);

-- ── Booking Cancellations ─────────────────────────────────────
CREATE TABLE booking_cancellations
(
    id           BIGSERIAL   PRIMARY KEY,
    booking_id   BIGINT      NOT NULL REFERENCES bookings (id),
    cancelled_by BIGINT      NOT NULL REFERENCES users (id),
    reason       TEXT,
    cancelled_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cancellations_booking ON booking_cancellations (booking_id);
