
CREATE TABLE IF NOT EXISTS risk_policy_event (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT,
    action VARCHAR(64),
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);
