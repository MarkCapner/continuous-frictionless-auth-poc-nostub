CREATE TABLE IF NOT EXISTS policy_rule (
  id BIGSERIAL PRIMARY KEY,
  scope VARCHAR(32) NOT NULL,          -- GLOBAL | TENANT | USER
  scope_ref VARCHAR(128),              -- tenant_id / user_id / null for GLOBAL
  priority INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  condition_json JSONB NOT NULL,
  action_json JSONB NOT NULL,
  description TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_policy_rule_scope ON policy_rule(scope, scope_ref);
CREATE INDEX IF NOT EXISTS idx_policy_rule_enabled_priority ON policy_rule(enabled, priority DESC, id DESC);
