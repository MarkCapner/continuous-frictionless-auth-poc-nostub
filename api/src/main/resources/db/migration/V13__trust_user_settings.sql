-- EPIC 12.6: lightweight consent + trust reset hooks (PoC)
CREATE TABLE IF NOT EXISTS trust_user_settings (
    user_id TEXT PRIMARY KEY,
    consent_granted BOOLEAN NOT NULL DEFAULT TRUE,
    consent_updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    baseline_reset_at TIMESTAMPTZ NULL,
    baseline_reset_reason TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_trust_user_settings_consent_updated
    ON trust_user_settings (consent_updated_at DESC);
