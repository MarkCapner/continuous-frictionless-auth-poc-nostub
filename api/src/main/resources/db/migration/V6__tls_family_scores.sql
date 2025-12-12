-- EPIC 9.1.5: TLS family confidence/stability scoring support.
--
-- Add explicit lifecycle fields used by services/UI. Keep existing columns for
-- backwards compatibility with earlier EPIC 9 work.

ALTER TABLE tls_family
  ADD COLUMN IF NOT EXISTS first_seen TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS observation_count BIGINT,
  ADD COLUMN IF NOT EXISTS variant_count INT,
  ADD COLUMN IF NOT EXISTS confidence_score DOUBLE PRECISION,
  ADD COLUMN IF NOT EXISTS stability_score DOUBLE PRECISION;

-- Backfill new fields from existing columns.
UPDATE tls_family
SET
  first_seen = COALESCE(first_seen, created_at, now()),
  observation_count = COALESCE(observation_count, seen_count, 1),
  variant_count = COALESCE(variant_count,
    (SELECT COUNT(*) FROM tls_family_member m WHERE m.family_id = tls_family.family_id),
    1
  )
WHERE first_seen IS NULL
   OR observation_count IS NULL
   OR variant_count IS NULL;

CREATE INDEX IF NOT EXISTS idx_tls_family_confidence ON tls_family(confidence_score DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_tls_family_stability  ON tls_family(stability_score DESC NULLS LAST);
