-- ═══════════════════════════════════════════════════════════════════
-- V2 — Business & Catalog Schema
-- Tables: business_categories, businesses, branches,
--         service_attribute_definitions, services,
--         service_employees, service_resources
-- ═══════════════════════════════════════════════════════════════════

-- Enable PostGIS for geo columns (idempotent)
CREATE EXTENSION IF NOT EXISTS postgis;

-- ── Business Categories ──────────────────────────────────────────
CREATE TABLE business_categories
(
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,   -- 'beauty_salon', 'gym'
    label         VARCHAR(100) NOT NULL,           -- Display label
    icon_url      TEXT,
    resource_type VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE'
        CHECK (resource_type IN ('EMPLOYEE', 'RESOURCE', 'NONE'))
);

-- ── Businesses ───────────────────────────────────────────────────
CREATE TABLE businesses
(
    id          BIGSERIAL    PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users (id),
    category_id BIGINT       NOT NULL REFERENCES business_categories (id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url    TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED')),
    meta        JSONB        NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_businesses_owner    ON businesses (owner_id);
CREATE INDEX idx_businesses_category ON businesses (category_id);
CREATE INDEX idx_businesses_status   ON businesses (status);

-- ── Branches ─────────────────────────────────────────────────────
CREATE TABLE branches
(
    id          BIGSERIAL    PRIMARY KEY,
    business_id BIGINT       NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    address     TEXT         NOT NULL,
    city        VARCHAR(100) NOT NULL,
    country     VARCHAR(100) NOT NULL DEFAULT 'UA',
    postal_code VARCHAR(20),
    location    GEOGRAPHY(POINT, 4326),           -- PostGIS geo point
    phone       VARCHAR(30),
    email       VARCHAR(255),
    timezone    VARCHAR(60)  NOT NULL DEFAULT 'Europe/Kiev',
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_branches_business  ON branches (business_id);
CREATE INDEX idx_branches_location  ON branches USING GIST (location);
CREATE INDEX idx_branches_city      ON branches (city);

-- ── Service Attribute Definitions ────────────────────────────────
CREATE TABLE service_attribute_definitions
(
    id          BIGSERIAL    PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES business_categories (id),
    field_key   VARCHAR(100) NOT NULL,
    field_label VARCHAR(100) NOT NULL,
    field_type  VARCHAR(30)  NOT NULL
        CHECK (field_type IN ('TEXT', 'NUMBER', 'SELECT', 'BOOLEAN', 'MULTI_SELECT')),
    options     JSONB,
    is_required BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INT          NOT NULL DEFAULT 0,
    UNIQUE (category_id, field_key)
);

-- ── Services ─────────────────────────────────────────────────────
CREATE TABLE services
(
    id              BIGSERIAL      PRIMARY KEY,
    business_id     BIGINT         NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    name            VARCHAR(255)   NOT NULL,
    description     TEXT,
    category_id     BIGINT         NOT NULL REFERENCES business_categories (id),
    duration_min    INT            NOT NULL,
    price           NUMERIC(10, 2),
    currency        VARCHAR(3)     NOT NULL DEFAULT 'UAH',
    attributes      JSONB          NOT NULL DEFAULT '{}',
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    search_vector   TSVECTOR
);

CREATE INDEX idx_services_business   ON services (business_id);
CREATE INDEX idx_services_category   ON services (category_id);
CREATE INDEX idx_services_active     ON services (is_active);
CREATE INDEX idx_services_search     ON services USING GIN (search_vector);
CREATE INDEX idx_services_attributes ON services USING GIN (attributes);

-- Full-text search trigger (Ukrainian + English)
CREATE OR REPLACE FUNCTION services_search_vector_update() RETURNS TRIGGER AS
$$
BEGIN
    NEW.search_vector :=
        to_tsvector('simple',
            coalesce(NEW.name, '') || ' ' || coalesce(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_services_search_vector
    BEFORE INSERT OR UPDATE
    ON services
    FOR EACH ROW
EXECUTE FUNCTION services_search_vector_update();

-- ── Service ↔ Employee (many-to-many) ────────────────────────────
-- Employees table is created in V3; we defer FK and just reserve the pivot now.
-- (Created in V3 to avoid forward-reference; this block is a placeholder comment.)

-- ── Service ↔ Resource (many-to-many) ────────────────────────────
-- Same as above — created in V3.
