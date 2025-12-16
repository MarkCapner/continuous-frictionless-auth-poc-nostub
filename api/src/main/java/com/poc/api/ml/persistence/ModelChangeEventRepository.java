package com.poc.api.ml.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ModelChangeEventRepository {

  public record ChangeEvent(
      long id,
      OffsetDateTime createdAt,
      String actor,
      String eventType,
      String kind,
      String scopeType,
      String scopeKey,
      Long fromModelId,
      Long toModelId,
      String reason,
      String evidenceJson
  ) {}

  private final JdbcTemplate jdbc;

  public ModelChangeEventRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final RowMapper<ChangeEvent> mapper = new RowMapper<>() {
    @Override public ChangeEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ChangeEvent(
          rs.getLong("id"),
          rs.getObject("created_at", OffsetDateTime.class),
          rs.getString("actor"),
          rs.getString("event_type"),
          rs.getString("kind"),
          rs.getString("scope_type"),
          rs.getString("scope_key"),
          (Long) rs.getObject("from_model_id"),
          (Long) rs.getObject("to_model_id"),
          rs.getString("reason"),
          rs.getString("evidence_json")
      );
    }
  };

  public long insert(ChangeEvent e) {
    return jdbc.queryForObject(
        "INSERT INTO model_change_event(actor, event_type, kind, scope_type, scope_key, from_model_id, to_model_id, reason, evidence_json) " +
            "VALUES (?,?,?,?,?,?,?,?, ?::jsonb) RETURNING id",
        Long.class,
        e.actor(), e.eventType(), e.kind(), e.scopeType(), e.scopeKey(), e.fromModelId(), e.toModelId(), e.reason(),
        e.evidenceJson() == null ? "{}" : e.evidenceJson()
    );
  }

  public List<ChangeEvent> list(String kind, String scopeType, String scopeKey, int limit) {
    return jdbc.query(
        "SELECT * FROM model_change_event WHERE kind=? AND scope_type=? AND scope_key=? ORDER BY created_at DESC LIMIT ?",
        mapper, kind, scopeType, scopeKey, Math.min(limit, 300)
    );
  }

  public Optional<ChangeEvent> lastActivateOrRollback(String kind, String scopeType, String scopeKey) {
    List<ChangeEvent> rows = jdbc.query(
        "SELECT * FROM model_change_event WHERE kind=? AND scope_type=? AND scope_key=? AND event_type IN ('ACTIVATE','ROLLBACK') " +
            "ORDER BY created_at DESC LIMIT 1",
        mapper, kind, scopeType, scopeKey
    );
    return rows.stream().findFirst();
  }

  public boolean hasRollbackForScorecard(long scorecardId) {
    Integer c = jdbc.queryForObject(
        "SELECT count(*) FROM model_change_event WHERE event_type='ROLLBACK' AND evidence_json->>'scorecardId' = ?",
        Integer.class,
        String.valueOf(scorecardId)
    );
    return c != null && c > 0;
  }
}
