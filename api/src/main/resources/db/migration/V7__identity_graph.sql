-- EPIC 10.1: Identity Graph data model (nodes + probabilistic links)

CREATE TABLE IF NOT EXISTS identity_node (
  id BIGSERIAL PRIMARY KEY,
  node_type TEXT NOT NULL,
  natural_key TEXT NOT NULL,
  display_label TEXT,
  meta_json JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (node_type, natural_key)
);

CREATE INDEX IF NOT EXISTS idx_identity_node_type_key
  ON identity_node(node_type, natural_key);

CREATE TABLE IF NOT EXISTS identity_link (
  id BIGSERIAL PRIMARY KEY,
  from_node_id BIGINT NOT NULL REFERENCES identity_node(id) ON DELETE CASCADE,
  to_node_id   BIGINT NOT NULL REFERENCES identity_node(id) ON DELETE CASCADE,
  link_type TEXT NOT NULL,
  confidence NUMERIC NOT NULL,
  reason TEXT,
  evidence_json JSONB,
  first_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (from_node_id, to_node_id, link_type)
);

CREATE INDEX IF NOT EXISTS idx_identity_link_from
  ON identity_link(from_node_id);
CREATE INDEX IF NOT EXISTS idx_identity_link_to
  ON identity_link(to_node_id);
CREATE INDEX IF NOT EXISTS idx_identity_link_type
  ON identity_link(link_type);
