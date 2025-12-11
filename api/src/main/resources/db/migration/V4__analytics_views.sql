-- EPIC 7: analytics views for risk, sessions, users, devices, and model performance

-- User summary view, mirroring DecisionLogRepository.findUserSummaries but without LIMIT.
CREATE OR REPLACE VIEW v_user_summary AS
SELECT
  COALESCE(user_id, 'anonymous') AS user_id,
  COUNT(*) AS sessions,
  COUNT(DISTINCT tls_fp) AS devices,
  MAX(created_at) AS last_seen,
  AVG(confidence) AS avg_confidence
FROM decision_log
GROUP BY COALESCE(user_id, 'anonymous');

-- Device / TLS fingerprint summary, mirroring DeviceProfileRepository.findAllTlsStats.
CREATE OR REPLACE VIEW v_device_tls_summary AS
SELECT
  tls_fp,
  COUNT(*) AS profiles,
  COUNT(DISTINCT user_id) AS users,
  MIN(first_seen) AS first_seen,
  MAX(last_seen) AS last_seen
FROM device_profile
GROUP BY tls_fp;

-- Session analytics by day.
CREATE OR REPLACE VIEW v_session_daily_stats AS
SELECT
  date_trunc('day', occurred_at)::date AS day,
  COUNT(*) AS sessions,
  SUM(CASE WHEN decision = 'AUTO_LOGIN' THEN 1 ELSE 0 END) AS auto_login,
  SUM(CASE WHEN decision = 'STEP_UP' THEN 1 ELSE 0 END) AS step_up,
  SUM(CASE WHEN decision = 'DENY' THEN 1 ELSE 0 END) AS deny,
  AVG(confidence) AS avg_confidence
FROM session_feature
GROUP BY date_trunc('day', occurred_at)::date
ORDER BY day DESC;

-- Risk statistics by decision, including last 24h/7d volume.
CREATE OR REPLACE VIEW v_risk_decision_stats AS
SELECT
  decision,
  COUNT(*) AS total,
  AVG(confidence) AS avg_confidence,
  COUNT(*) FILTER (WHERE occurred_at >= now() - interval '24 hours') AS last_24h,
  COUNT(*) FILTER (WHERE occurred_at >= now() - interval '7 days') AS last_7d
FROM session_feature
GROUP BY decision;

-- Model performance: simple confusion matrix over labeled sessions.
CREATE OR REPLACE VIEW v_model_confusion AS
SELECT
  decision,
  label,
  COUNT(*) AS sessions
FROM session_feature
WHERE label IS NOT NULL
GROUP BY decision, label;
