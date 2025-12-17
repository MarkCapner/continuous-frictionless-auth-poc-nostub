-- V0XX__create_session_domain_contributions.sql
CREATE TABLE IF NOT EXISTS session_domain_contributions (
  session_id BIGINT NOT NULL,
  user_id TEXT NOT NULL,
  domain TEXT NOT NULL,
  session_time TIMESTAMPTZ NOT NULL,
  absolute_score DOUBLE PRECISION NOT NULL,
  deviation_label TEXT NOT NULL
);

CREATE MATERIALIZED VIEW IF NOT EXISTS domain_contribution_daily AS
SELECT
  user_id,
  domain,
  date_trunc('day', session_time) AS day,
  avg(absolute_score) AS avg_score,
  max(absolute_score) AS max_score,
  count(*) FILTER (WHERE deviation_label = 'EXTREME') AS extreme_count,
  count(*) AS session_count
FROM session_domain_contributions
GROUP BY user_id, domain, day;
