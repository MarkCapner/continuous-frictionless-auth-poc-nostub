
CREATE TABLE IF NOT EXISTS policy_feedback (
    session_id BIGINT,
    success BOOLEAN,
    recorded_at TIMESTAMPTZ DEFAULT now()
);
