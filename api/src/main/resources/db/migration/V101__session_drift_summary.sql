
CREATE TABLE IF NOT EXISTS session_drift_summary (
    session_id BIGINT PRIMARY KEY,
    device_drift DOUBLE PRECISION,
    tls_drift DOUBLE PRECISION,
    behaviour_drift DOUBLE PRECISION,
    identity_drift DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT now()
);
