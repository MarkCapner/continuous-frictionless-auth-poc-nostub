
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
