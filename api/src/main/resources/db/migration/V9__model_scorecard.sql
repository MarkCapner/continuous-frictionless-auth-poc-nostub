-- EPIC 11.8: KPI scorecards

CREATE TABLE IF NOT EXISTS model_scorecard (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  kind TEXT NOT NULL DEFAULT 'risk-model',
  scope_type TEXT NOT NULL DEFAULT 'GLOBAL',
  scope_key TEXT NOT NULL DEFAULT '*',
  model_id BIGINT,
  model_version TEXT,
  trigger_type TEXT NOT NULL,
  baseline_n INT NOT NULL DEFAULT 0,
  recovery_n INT NOT NULL DEFAULT 0,
  baseline_metrics_json JSONB NOT NULL,
  recovery_metrics_json JSONB NOT NULL,
  delta_metrics_json JSONB NOT NULL,
  status TEXT NOT NULL,
  notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_scorecard_scope_time ON model_scorecard(kind, scope_type, scope_key, created_at DESC);
