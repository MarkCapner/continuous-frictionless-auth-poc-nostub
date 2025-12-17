
CREATE TABLE IF NOT EXISTS session_drift_timeseries (
    session_id BIGINT PRIMARY KEY,
    user_id VARCHAR(128),
    total_drift DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ DEFAULT now()
);
