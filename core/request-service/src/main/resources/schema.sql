-- Таблица заявок на участие
CREATE TABLE IF NOT EXISTS participation_requests (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    requester_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL
);
