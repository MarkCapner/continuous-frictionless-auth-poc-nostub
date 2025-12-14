package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ModelScorecardRepository {

  public record ScorecardRow(
      long id,
      OffsetDateTime createdAt,
      String kind,
      String scopeType,
      String scopeKey,
      Long modelId,
      String modelVersion,
      String triggerType,
      int baselineN,
      int recoveryN,
      String baselineMetricsJson,
      String recoveryMetricsJson,
      String deltaMetricsJson,
      String status,
      String notes
  ) {}

  private final JdbcTemplate jdbc;

  public ModelScorecardRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final RowMapper<ScorecardRow> mapper = new RowMapper<>() {
    @Override public ScorecardRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ScorecardRow(
          rs.getLong("id"),
          rs.getObject("created_at", OffsetDateTime.class),
          rs.getString("kind"),
          rs.getString("scope_type"),
          rs.getString("scope_key"),
          (Long) rs.getObject("model_id"),
          rs.getString("model_version"),
          rs.getString("trigger_type"),
          rs.getInt("baseline_n"),
          rs.getInt("recovery_n"),
          rs.getString("baseline_metrics_json"),
          rs.getString("recovery_metrics_json"),
          rs.getString("delta_metrics_json"),
          rs.getString("status"),
          rs.getString("notes")
      );
    }
  };

  public long insert(ScorecardRow s) {
    return jdbc.queryForObject(
        "INSERT INTO model_scorecard(kind, scope_type, scope_key, model_id, model_version, trigger_type," +
            " baseline_n, recovery_n, baseline_metrics_json, recovery_metrics_json, delta_metrics_json, status, notes) " +
            "VALUES (?,?,?,?,?,?,?,?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?) RETURNING id",
        Long.class,
        s.kind(), s.scopeType(), s.scopeKey(), s.modelId(), s.modelVersion(), s.triggerType(),
        s.baselineN(), s.recoveryN(), s.baselineMetricsJson(), s.recoveryMetricsJson(), s.deltaMetricsJson(), s.status(), s.notes()
    );
  }

  public List<ScorecardRow> list(String scopeType, String scopeKey, int limit) {
    return jdbc.query(
        "SELECT * FROM model_scorecard WHERE scope_type=? AND scope_key=? ORDER BY created_at DESC LIMIT ?",
        mapper, scopeType, scopeKey, Math.min(limit, 200)
    );
  }

  public Optional<ScorecardRow> get(long id) {
    List<ScorecardRow> rows = jdbc.query("SELECT * FROM model_scorecard WHERE id=?", mapper, id);
    return rows.stream().findFirst();
  }
}
