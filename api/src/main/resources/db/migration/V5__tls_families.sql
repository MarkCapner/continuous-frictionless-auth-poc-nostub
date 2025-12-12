CREATE TABLE IF NOT EXISTS tls_family (
  family_id TEXT PRIMARY KEY,
  family_key TEXT NOT NULL,
  sample_tls_fp TEXT,
  sample_meta TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen  TIMESTAMPTZ NOT NULL DEFAULT now(),
  seen_count BIGINT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tls_family_member (
  raw_tls_fp TEXT PRIMARY KEY,
  family_id TEXT NOT NULL REFERENCES tls_family(family_id) ON DELETE CASCADE,
  first_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen  TIMESTAMPTZ NOT NULL DEFAULT now(),
  seen_count BIGINT NOT NULL DEFAULT 1,
  last_meta TEXT
);

CREATE TABLE IF NOT EXISTS user_tls_family (
  user_id TEXT NOT NULL,
  family_id TEXT NOT NULL REFERENCES tls_family(family_id) ON DELETE CASCADE,
  first_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen  TIMESTAMPTZ NOT NULL DEFAULT now(),
  seen_count BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (user_id, family_id)
);

CREATE INDEX IF NOT EXISTS idx_tls_family_last_seen ON tls_family(last_seen DESC);
CREATE INDEX IF NOT EXISTS idx_user_tls_family_user ON user_tls_family(user_id);
CREATE INDEX IF NOT EXISTS idx_tls_family_member_family ON tls_family_member(family_id);
