CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,
    role       VARCHAR(20)   NOT NULL,
    manager_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
