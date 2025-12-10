CREATE TABLE IF NOT EXISTS device_profile (
  id BIGSERIAL PRIMARY KEY,
  user_id TEXT NOT NULL,
  tls_fp TEXT NOT NULL,
  ua_family TEXT NOT NULL,
  ua_version TEXT NOT NULL,
  screen_w INT NOT NULL,
  screen_h INT NOT NULL,
  pixel_ratio NUMERIC(4,2) NOT NULL,
  tz_offset SMALLINT NOT NULL,
  canvas_hash TEXT NOT NULL,
  webgl_hash TEXT,
  first_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen  TIMESTAMPTZ NOT NULL DEFAULT now(),
  seen_count BIGINT NOT NULL DEFAULT 1,
  last_country TEXT,
  UNIQUE (user_id, tls_fp, canvas_hash)
);

CREATE TABLE IF NOT EXISTS behavior_profile_stats (
  id BIGSERIAL PRIMARY KEY,
  user_id TEXT NOT NULL,
  feature TEXT NOT NULL,
  mean NUMERIC NOT NULL,
  variance NUMERIC NOT NULL,
  decay NUMERIC NOT NULL DEFAULT 0.9,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, feature)
);

CREATE TABLE IF NOT EXISTS session_feature (
  id BIGSERIAL PRIMARY KEY,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  user_id TEXT,
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

CREATE TABLE IF NOT EXISTS model_registry (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  format TEXT NOT NULL,
  version TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  bytes BYTEA NOT NULL,
  sha256 TEXT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS decision_log (
  id BIGSERIAL PRIMARY KEY,
  session_id TEXT NOT NULL,
  user_id TEXT,
  tls_fp TEXT NOT NULL,
  behavior_score NUMERIC NOT NULL,
  device_score NUMERIC NOT NULL,
  context_score NUMERIC NOT NULL,
  confidence NUMERIC NOT NULL,
  decision TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
