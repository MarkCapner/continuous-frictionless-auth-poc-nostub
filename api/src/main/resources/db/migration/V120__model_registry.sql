-- Ensure base table exists
CREATE TABLE IF NOT EXISTS model_registry (
  id BIGSERIAL PRIMARY KEY
);

-- Add missing columns safely
ALTER TABLE model_registry
  ADD COLUMN IF NOT EXISTS model_key TEXT,
  ADD COLUMN IF NOT EXISTS model_type TEXT,
  ADD COLUMN IF NOT EXISTS training_mode TEXT,
  ADD COLUMN IF NOT EXISTS version INTEGER,
  ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT false,
  ADD COLUMN IF NOT EXISTS confidence_level TEXT,
  ADD COLUMN IF NOT EXISTS confidence_score DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS trained_on_from TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS trained_on_to TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS feature_schema_hash TEXT,
  ADD COLUMN IF NOT EXISTS training_rows INTEGER,
  ADD COLUMN IF NOT EXISTS metrics JSONB,
  ADD COLUMN IF NOT EXISTS model_blob BYTEA,
  ADD COLUMN IF NOT EXISTS activated_at TIMESTAMPTZ;

-- Backfill minimal defaults
UPDATE model_registry
SET
  model_key = COALESCE(model_key, 'risk-default'),
  active = COALESCE(active, false),
  confidence_level = COALESCE(confidence_level, 'LOW'),
  confidence_score = COALESCE(confidence_score, 0.0)
WHERE model_key IS NULL;

-- Create index last (now safe)
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_model
ON model_registry(model_key)
WHERE active = true;
