package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * EPIC 7: analytics repository backed by Postgres views.
 *
 * All queries here read from v_risk_decision_stats, v_session_daily_stats, v_model_confusion,
 * and related views created in V4__analytics_views.sql.
 */
@Repository
public class AdminAnalyticsRepository {

  private final JdbcTemplate jdbcTemplate;

  public AdminAnalyticsRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record RiskStatsRow(
      String decision,
      long total,
      double avgConfidence,
      long last24h,
      long last7d
  ) {}

  public record SessionDailyStatsRow(
      LocalDate day,
      long sessions,
      long autoLogin,
      long stepUp,
      long deny,
      double avgConfidence
  ) {}

  public record ModelConfusionRow(
      String decision,
      String label,
      long sessions
  ) {}

  public java.util.List<RiskStatsRow> findRiskStats() {
    String sql = "SELECT decision, total, avg_confidence, last_24h, last_7d FROM v_risk_decision_stats ORDER BY decision";
    return jdbcTemplate.query(sql, (rs, rowNum) -> new RiskStatsRow(
        rs.getString("decision"),
        rs.getLong("total"),
        rs.getDouble("avg_confidence"),
        rs.getLong("last_24h"),
        rs.getLong("last_7d")
    ));
  }

  public java.util.List<SessionDailyStatsRow> findSessionDailyStats(int limit) {
    String sql = """
        SELECT day, sessions, auto_login, step_up, deny, avg_confidence
        FROM v_session_daily_stats
        ORDER BY day DESC
        LIMIT ?
        """;
    return jdbcTemplate.query(sql, (rs, rowNum) -> new SessionDailyStatsRow(
        rs.getObject("day", LocalDate.class),
        rs.getLong("sessions"),
        rs.getLong("auto_login"),
        rs.getLong("step_up"),
        rs.getLong("deny"),
        rs.getDouble("avg_confidence")
    ), limit);
  }

  public java.util.List<ModelConfusionRow> findModelConfusion() {
    String sql = """
        SELECT decision, label, sessions
        FROM v_model_confusion
        ORDER BY decision, label
        """;
    return jdbcTemplate.query(sql, (rs, rowNum) -> new ModelConfusionRow(
        rs.getString("decision"),
        rs.getString("label"),
        rs.getLong("sessions")
    ));
  }
}
