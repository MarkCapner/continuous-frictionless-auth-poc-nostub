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
public class ModelCanaryPolicyRepository {

  public record CanaryPolicy(
      long id,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      String kind,
      String scopeType,
      String scopeKey,
      long modelId,
      int rolloutPercent,
      boolean enabled
  ) {}

  private final JdbcTemplate jdbc;

  public ModelCanaryPolicyRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private final RowMapper<CanaryPolicy> mapper = new RowMapper<>() {
    @Override public CanaryPolicy mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new CanaryPolicy(
          rs.getLong("id"),
          rs.getObject("created_at", OffsetDateTime.class),
          rs.getObject("updated_at", OffsetDateTime.class),
          rs.getString("kind"),
          rs.getString("scope_type"),
          rs.getString("scope_key"),
          rs.getLong("model_id"),
          rs.getInt("rollout_percent"),
          rs.getBoolean("enabled")
      );
    }
  };

  public Optional<CanaryPolicy> get(String kind, String scopeType, String scopeKey) {
    List<CanaryPolicy> rows = jdbc.query(
        "SELECT * FROM model_canary_policy WHERE kind=? AND scope_type=? AND scope_key=?",
        mapper, kind, scopeType, scopeKey
    );
    return rows.stream().findFirst();
  }

  public void upsert(String kind, String scopeType, String scopeKey, long modelId, int rolloutPercent, boolean enabled) {
    jdbc.update(
        "INSERT INTO model_canary_policy(kind, scope_type, scope_key, model_id, rollout_percent, enabled) " +
            "VALUES (?,?,?,?,?,?) " +
            "ON CONFLICT (kind, scope_type, scope_key) DO UPDATE SET " +
            "model_id=EXCLUDED.model_id, rollout_percent=EXCLUDED.rollout_percent, enabled=EXCLUDED.enabled, updated_at=now()",
        kind, scopeType, scopeKey, modelId, rolloutPercent, enabled
    );
  }

  public void disable(String kind, String scopeType, String scopeKey) {
    jdbc.update(
        "UPDATE model_canary_policy SET enabled=false, rollout_percent=0, updated_at=now() WHERE kind=? AND scope_type=? AND scope_key=?",
        kind, scopeType, scopeKey
    );
  }
}
