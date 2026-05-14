CREATE TABLE vacation_requests (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT      REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_dates CHECK (end_date >= start_date)
);
