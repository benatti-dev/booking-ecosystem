-- ═══════════════════════════════════════════════════════════════════
-- V1 — Initial Auth Schema
-- Tables: users, refresh_tokens, password_reset_tokens, audit_logs
-- ═══════════════════════════════════════════════════════════════════

-- ── Users ────────────────────────────────────────────────────────
CREATE TABLE users
(
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(30)  NOT NULL
        CHECK (role IN ('ADMIN', 'BUSINESS_OWNER', 'EMPLOYEE', 'CLIENT')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING')),
    avatar_url    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- ── Refresh Tokens ───────────────────────────────────────────────
CREATE TABLE refresh_tokens
(
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 hex of raw UUID token
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- ── Password Reset Tokens ────────────────────────────────────────
CREATE TABLE password_reset_tokens
(
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 hex of raw UUID token
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);

-- ── Audit Logs ───────────────────────────────────────────────────
CREATE TABLE audit_logs
(
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    action     VARCHAR(50)  NOT NULL,   -- LOGIN | LOGOUT | FAILED_LOGIN | REGISTER | ...
    ip_address VARCHAR(45),            -- supports IPv6
    details    TEXT,                   -- JSON string with extra context
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id    ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action     ON audit_logs (action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
