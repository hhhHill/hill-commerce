CREATE TABLE user_behavior_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    behavior_type VARCHAR(64) NOT NULL,
    target_id BIGINT,
    created_at TIMESTAMP NOT NULL
);
