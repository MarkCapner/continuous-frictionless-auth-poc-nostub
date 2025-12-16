ALTER TABLE decision_log
ADD COLUMN IF NOT EXISTS feature_contributions_json JSONB,
ADD COLUMN IF NOT EXISTS top_positive_contributors_json JSONB,
ADD COLUMN IF NOT EXISTS top_negative_contributors_json JSONB;
