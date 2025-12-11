CREATE TABLE IF NOT EXISTS session_feature (
  id BIGSERIAL PRIMARY KEY,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  user_id TEXT NOT NULL,
  request_id TEXT NOT NULL,
  tls_fp TEXT NOT NULL,
  device_json JSONB NOT NULL,
  behavior_json JSONB NOT NULL,
  context_json JSONB NOT NULL,
  feature_vector JSONB NOT NULL,
  decision TEXT NOT NULL,
  confidence NUMERIC NOT NULL,
  label TEXT
);

CREATE INDEX IF NOT EXISTS idx_session_feature_user ON session_feature(user_id);
CREATE INDEX IF NOT EXISTS idx_session_feature_tls ON session_feature(tls_fp);
CREATE INDEX IF NOT EXISTS idx_session_feature_occurred_at ON session_feature(occurred_at);
