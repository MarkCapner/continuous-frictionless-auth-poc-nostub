-- EPIC 11.9: canary rollouts + change events + rollback audit

CREATE TABLE IF NOT EXISTS model_canary_policy (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  kind TEXT NOT NULL DEFAULT 'risk-model',
  scope_type TEXT NOT NULL DEFAULT 'GLOBAL',
  scope_key TEXT NOT NULL DEFAULT '*',
  model_id BIGINT NOT NULL,
  rollout_percent INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_model_canary_policy_scope
ON model_canary_policy(kind, scope_type, scope_key);

CREATE TABLE IF NOT EXISTS model_change_event (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  actor TEXT NOT NULL DEFAULT 'system',
  event_type TEXT NOT NULL,
  kind TEXT NOT NULL DEFAULT 'risk-model',
  scope_type TEXT NOT NULL DEFAULT 'GLOBAL',
  scope_key TEXT NOT NULL DEFAULT '*',
  from_model_id BIGINT,
  to_model_id BIGINT,
  reason TEXT,
  evidence_json JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_model_change_event_scope_time
ON model_change_event(kind, scope_type, scope_key, created_at DESC);
