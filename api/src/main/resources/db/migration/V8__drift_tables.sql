CREATE TABLE IF NOT EXISTS drift_baseline (
  user_id TEXT PRIMARY KEY,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_device_sig TEXT,
  last_tls_family TEXT,
  last_model_version TEXT,
  conf_count BIGINT NOT NULL DEFAULT 0,
  conf_mean DOUBLE PRECISION NOT NULL DEFAULT 0,
  conf_m2 DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS drift_event (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  user_id TEXT NOT NULL,
  request_id TEXT NOT NULL,
  device_drift DOUBLE PRECISION NOT NULL,
  behavior_drift DOUBLE PRECISION NOT NULL,
  tls_drift DOUBLE PRECISION NOT NULL,
  feature_drift DOUBLE PRECISION NOT NULL,
  model_instability DOUBLE PRECISION NOT NULL,
  max_drift DOUBLE PRECISION NOT NULL,
  warnings JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_drift_event_user_time ON drift_event(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_drift_event_req ON drift_event(request_id);
