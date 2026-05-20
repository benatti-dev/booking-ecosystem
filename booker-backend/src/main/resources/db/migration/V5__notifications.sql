-- Phase 4: In-app notifications
CREATE TABLE notifications (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    reference_id BIGINT,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_created_at  ON notifications (created_at DESC);
