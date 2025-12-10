package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class DecisionLogRepository {

  private final JdbcTemplate jdbc;

  public DecisionLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(String sessionId, String userId, String tlsFp,
                     double behaviorScore, double deviceScore, double contextScore,
                     double confidence, String decision) {
    String sql = "INSERT INTO decision_log " +
        "(session_id, user_id, tls_fp, behavior_score, device_score, context_score, confidence, decision) " +
        "VALUES (?,?,?,?,?,?,?,?)";
    jdbc.update(sql, sessionId, userId, tlsFp, behaviorScore, deviceScore,
        contextScore, confidence, decision);
  }

  public List<DecisionLogRow> findRecentByUser(String userId, int limit) {
    String sql = "SELECT * FROM decision_log WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
    return jdbc.query(sql, (rs, rowNum) -> {
      DecisionLogRow r = new DecisionLogRow();
      r.id = rs.getLong("id");
      r.sessionId = rs.getString("session_id");
      r.userId = rs.getString("user_id");
      r.tlsFp = rs.getString("tls_fp");
      r.behaviorScore = rs.getDouble("behavior_score");
      r.deviceScore = rs.getDouble("device_score");
      r.contextScore = rs.getDouble("context_score");
      r.confidence = rs.getDouble("confidence");
      r.decision = rs.getString("decision");
      r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
      return r;
    }, userId, limit);
  }

  public List<UserSummaryRow> findUserSummaries(int limit) {
    String sql = """
        SELECT
          COALESCE(user_id, 'anonymous') AS user_id,
          COUNT(*) AS sessions,
          COUNT(DISTINCT tls_fp) AS devices,
          MAX(created_at) AS last_seen,
          AVG(confidence) AS avg_confidence
        FROM decision_log
        GROUP BY COALESCE(user_id, 'anonymous')
        ORDER BY last_seen DESC
        LIMIT ?
        """;

    return jdbc.query(sql, (rs, rowNum) -> {
      UserSummaryRow row = new UserSummaryRow();
      row.userId = rs.getString("user_id");
      row.sessions = rs.getLong("sessions");
      row.devices = rs.getLong("devices");
      row.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      row.avgConfidence = rs.getDouble("avg_confidence");
      return row;
    }, limit);
  }

}