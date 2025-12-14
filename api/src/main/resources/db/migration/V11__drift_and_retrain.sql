-- EPIC 11.5–11.7 foundations: drift tables + retrain_job + model_registry scoping extensions

CREATE TABLE IF NOT EXISTS drift_baseline (
  user_id TEXT PRIMARY KEY,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_device_fp TEXT,
  last_tls_fp TEXT,
  last_model_version TEXT,
  beh_count BIGINT NOT NULL DEFAULT 0,
  beh_mean DOUBLE PRECISION NOT NULL DEFAULT 0,
  beh_m2   DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS drift_event (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  user_id TEXT,
  session_id TEXT,
  device_drift DOUBLE PRECISION NOT NULL DEFAULT 0,
  behavior_drift DOUBLE PRECISION NOT NULL DEFAULT 0,
  tls_drift DOUBLE PRECISION NOT NULL DEFAULT 0,
  model_instability DOUBLE PRECISION NOT NULL DEFAULT 0,
  max_drift DOUBLE PRECISION NOT NULL DEFAULT 0,
  warnings JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_drift_event_user_time ON drift_event(user_id, created_at DESC);

ALTER TABLE model_registry
  ADD COLUMN IF NOT EXISTS kind TEXT,
  ADD COLUMN IF NOT EXISTS scope_type TEXT,
  ADD COLUMN IF NOT EXISTS scope_key TEXT,
  ADD COLUMN IF NOT EXISTS metrics_json JSONB;

UPDATE model_registry SET kind = COALESCE(kind, 'risk-model') WHERE kind IS NULL;
UPDATE model_registry SET scope_type = COALESCE(scope_type, 'GLOBAL') WHERE scope_type IS NULL;
UPDATE model_registry SET scope_key = COALESCE(scope_key, '*') WHERE scope_key IS NULL;

CREATE INDEX IF NOT EXISTS idx_model_registry_scope ON model_registry(kind, scope_type, scope_key, created_at DESC);

CREATE TABLE IF NOT EXISTS retrain_job (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  kind TEXT NOT NULL DEFAULT 'risk-model',
  scope_type TEXT NOT NULL DEFAULT 'GLOBAL',
  scope_key TEXT NOT NULL DEFAULT '*',
  reason TEXT,
  status TEXT NOT NULL DEFAULT 'QUEUED',
  from_model_id BIGINT,
  to_model_id BIGINT,
  metrics_json JSONB,
  error TEXT
);

CREATE INDEX IF NOT EXISTS idx_retrain_job_status ON retrain_job(status, created_at ASC);
