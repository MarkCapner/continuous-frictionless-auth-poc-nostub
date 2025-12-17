
CREATE TABLE IF NOT EXISTS feature_baseline (
    user_id VARCHAR(128) NOT NULL,
    feature_key VARCHAR(128) NOT NULL,
    signal_type VARCHAR(32) NOT NULL,
    mean DOUBLE PRECISION,
    stddev DOUBLE PRECISION,
    sample_count INT,
    model_version VARCHAR(64),
    last_updated TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, feature_key, signal_type)
);
