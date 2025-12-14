-- EPIC 11: Decision log now stores per-signal core scores.
-- Some existing DBs were created before tls_score existed.

ALTER TABLE decision_log
  ADD COLUMN IF NOT EXISTS tls_score DOUBLE PRECISION;

-- Ensure existing rows are safe to read (no NULL surprises)
UPDATE decision_log
   SET tls_score = COALESCE(tls_score, 0.0)
 WHERE tls_score IS NULL;
