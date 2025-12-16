package com.poc.api.risk.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.poc.api.risk.persistence.UserSummaryRow;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class DecisionLogRepository {

  private final JdbcTemplate jdbcTemplate;

  public DecisionLogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Insert a decision log row (PoC schema is lightweight). */
  public void insert(String sessionId,
                     String userId,
                     String tlsFp,
                     double behaviorScore,
                     double deviceScore,
                     double contextScore,
                     double confidence,
                     String decision) {
    jdbcTemplate.update(
        "INSERT INTO decision_log(session_id, user_id, tls_fp, behavior_score, device_score, tls_score, context_score, confidence, decision) " +
            "VALUES (?,?,?,?,?,?,?,?,?)",
        sessionId,
        userId,
        tlsFp,
        behaviorScore,
        deviceScore,
        0.0,
        contextScore,
        confidence,
        decision
    );
  }

  public List<DecisionLogRow> findRecentByUser(String userId, int limit) {
    return jdbcTemplate.query(
        "SELECT id, created_at, session_id, user_id, tls_fp, behavior_score, device_score, tls_score, context_score, confidence, decision " +
            "FROM decision_log WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
        (rs, rowNum) -> {
          DecisionLogRow r = new DecisionLogRow();
          r.id = rs.getLong("id");
          r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
          r.sessionId = rs.getString("session_id");
          r.userId = rs.getString("user_id");
          r.tlsFp = rs.getString("tls_fp");
          r.behaviorScore = rs.getDouble("behavior_score");
          r.deviceScore = rs.getDouble("device_score");
          r.tlsScore = rs.getDouble("tls_score");
          r.contextScore = rs.getDouble("context_score");
          r.confidence = rs.getDouble("confidence");
          r.decision = rs.getString("decision");
          return r;
        },
        userId,
        Math.min(limit, 5000)
    );
  }

  public List<DecisionLogRow> findLastNBefore(OffsetDateTime before, int n) {
    return jdbcTemplate.query(
        "SELECT id, created_at, session_id, user_id, tls_fp, behavior_score, device_score, tls_score, context_score, confidence, decision " +
            "FROM decision_log WHERE created_at < ? ORDER BY created_at DESC LIMIT ?",
        (rs, rowNum) -> {
          DecisionLogRow r = new DecisionLogRow();
          r.id = rs.getLong("id");
          r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
          r.sessionId = rs.getString("session_id");
          r.userId = rs.getString("user_id");
          r.tlsFp = rs.getString("tls_fp");
          r.behaviorScore = rs.getDouble("behavior_score");
          r.deviceScore = rs.getDouble("device_score");
          r.tlsScore = rs.getDouble("tls_score");
          r.contextScore = rs.getDouble("context_score");
          r.confidence = rs.getDouble("confidence");
          r.decision = rs.getString("decision");
          return r;
        },
        before,
        Math.min(n, 5000)
    );
  }

  public List<DecisionLogRow> findFirstNAfter(OffsetDateTime after, int n) {
    return jdbcTemplate.query(
        "SELECT id, created_at, session_id, user_id, tls_fp, behavior_score, device_score, tls_score, context_score, confidence, decision " +
            "FROM decision_log WHERE created_at >= ? ORDER BY created_at ASC LIMIT ?",
        (rs, rowNum) -> {
          DecisionLogRow r = new DecisionLogRow();
          r.id = rs.getLong("id");
          r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
          r.sessionId = rs.getString("session_id");
          r.userId = rs.getString("user_id");
          r.tlsFp = rs.getString("tls_fp");
          r.behaviorScore = rs.getDouble("behavior_score");
          r.deviceScore = rs.getDouble("device_score");
          r.tlsScore = rs.getDouble("tls_score");
          r.contextScore = rs.getDouble("context_score");
          r.confidence = rs.getDouble("confidence");
          r.decision = rs.getString("decision");
          return r;
        },
        after,
        Math.min(n, 5000)
    );
  }

  public List<UserSummaryRow> findUserSummaries(int limit) {
    String sql =
        "SELECT COALESCE(user_id, 'anonymous') AS user_id, " +
            "COUNT(DISTINCT session_id) AS sessions, " +
            "COUNT(DISTINCT tls_fp) AS devices, " +
            "MAX(created_at) AS last_seen, " +
            "AVG(confidence) AS avg_confidence " +
            "FROM decision_log " +
            "GROUP BY COALESCE(user_id, 'anonymous') " +
            "ORDER BY last_seen DESC " +
            "LIMIT ?";

    return jdbcTemplate.query(sql, (rs, rowNum) -> {
      UserSummaryRow row = new UserSummaryRow();
      row.userId = rs.getString("user_id");
      row.sessions = rs.getLong("sessions");
      row.devices = rs.getLong("devices");
      row.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      row.avgConfidence = rs.getDouble("avg_confidence");
      return row;
    }, Math.min(limit, 500));
  }
}
