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
public class RetrainJobRepository {

  public record RetrainJobRow(
      long id,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String kind,
      String scopeType,
      String scopeKey,
      String reason,
      String status,
      Long fromModelId,
      Long toModelId,
      String metricsJson,
      String error
  ) {}

  private final JdbcTemplate jdbc;

  public RetrainJobRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final RowMapper<RetrainJobRow> mapper = new RowMapper<>() {
    @Override public RetrainJobRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new RetrainJobRow(
          rs.getLong("id"),
          rs.getObject("created_at", OffsetDateTime.class),
          rs.getObject("updated_at", OffsetDateTime.class),
          rs.getString("kind"),
          rs.getString("scope_type"),
          rs.getString("scope_key"),
          rs.getString("reason"),
          rs.getString("status"),
          (Long) rs.getObject("from_model_id"),
          (Long) rs.getObject("to_model_id"),
          rs.getString("metrics_json"),
          rs.getString("error")
      );
    }
  };

  public long enqueue(String kind, String scopeType, String scopeKey, String reason, Long fromModelId) {
    return jdbc.queryForObject(
        "INSERT INTO retrain_job(kind, scope_type, scope_key, reason, status, from_model_id) " +
            "VALUES (?,?,?,?, 'QUEUED', ?) RETURNING id",
        Long.class,
        kind, scopeType, scopeKey, reason, fromModelId
    );
  }

  public Optional<RetrainJobRow> nextQueued() {
    List<RetrainJobRow> rows = jdbc.query(
        "SELECT * FROM retrain_job WHERE status='QUEUED' ORDER BY created_at ASC LIMIT 1",
        mapper
    );
    return rows.stream().findFirst();
  }

  public void markRunning(long id) {
    jdbc.update("UPDATE retrain_job SET status='RUNNING', updated_at=now() WHERE id=?", id);
  }

  public void markSucceeded(long id, Long toModelId, String metricsJson) {
    jdbc.update(
        "UPDATE retrain_job SET status='SUCCEEDED', to_model_id=?, metrics_json=?::jsonb, updated_at=now() WHERE id=?",
        toModelId, metricsJson == null ? "{}" : metricsJson, id
    );
  }

  public void markFailed(long id, String error) {
    jdbc.update(
        "UPDATE retrain_job SET status='FAILED', error=?, updated_at=now() WHERE id=?",
        error, id
    );
  }

  public List<RetrainJobRow> list(int limit) {
    return jdbc.query("SELECT * FROM retrain_job ORDER BY created_at DESC LIMIT ?", mapper, Math.min(limit, 500));
  }
}
