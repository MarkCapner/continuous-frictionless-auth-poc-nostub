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
public class ModelRegistryRepository {

  public record ModelRecord(
      long id,
      String name,
      String format,
      String version,
      byte[] bytes,
      String sha256,
      boolean active,
      OffsetDateTime createdAt,
      String kind,
      String scopeType,
      String scopeKey,
      String metricsJson
  ) {}

  private final JdbcTemplate jdbcTemplate;

  private final RowMapper<ModelRecord> rowMapper = new RowMapper<>() {
    @Override
    public ModelRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ModelRecord(
          rs.getLong("id"),
          rs.getString("name"),
          rs.getString("format"),
          rs.getString("version"),
          rs.getBytes("bytes"),
          rs.getString("sha256"),
          rs.getBoolean("active"),
          rs.getObject("created_at", OffsetDateTime.class),
          rs.getString("kind"),
          rs.getString("scope_type"),
          rs.getString("scope_key"),
          rs.getString("metrics_json")
      );
    }
  };

  private static final String BASE_SELECT =
      "SELECT id, name, format, version, bytes, sha256, active, created_at, " +
          "COALESCE(kind,'risk-model') as kind, COALESCE(scope_type,'GLOBAL') as scope_type, COALESCE(scope_key,'*') as scope_key, " +
          "COALESCE(metrics_json,'{}'::jsonb)::text as metrics_json " +
          "FROM model_registry ";

  public ModelRegistryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<ModelRecord> findActive() {
    List<ModelRecord> rows = jdbcTemplate.query(
        BASE_SELECT + "WHERE active=true ORDER BY id DESC LIMIT 1",
        rowMapper
    );
    return rows.stream().findFirst();
  }

  public Optional<ModelRecord> findById(long id) {
    List<ModelRecord> rows = jdbcTemplate.query(
        BASE_SELECT + "WHERE id=?",
        rowMapper, id
    );
    return rows.stream().findFirst();
  }

  public List<ModelRecord> listRecent(String kind, String scopeType, String scopeKey, int limit) {
    return jdbcTemplate.query(
        BASE_SELECT +
            "WHERE COALESCE(kind,'risk-model')=? AND COALESCE(scope_type,'GLOBAL')=? AND COALESCE(scope_key,'*')=? " +
            "ORDER BY id DESC LIMIT ?",
        rowMapper,
        kind, scopeType, scopeKey, Math.min(limit, 500)
    );
  }

  public Optional<ModelRecord> findActiveScoped(String kind, String scopeType, String scopeKey) {
    List<ModelRecord> rows = jdbcTemplate.query(
        BASE_SELECT +
            "WHERE active=true AND COALESCE(kind,'risk-model')=? AND COALESCE(scope_type,'GLOBAL')=? AND COALESCE(scope_key,'*')=? " +
            "ORDER BY id DESC LIMIT 1",
        rowMapper,
        kind, scopeType, scopeKey
    );
    return rows.stream().findFirst();
  }

  public void deactivateAll() {
    jdbcTemplate.update("UPDATE model_registry SET active=false WHERE active=true");
  }

  public void deactivateAllScoped(String kind, String scopeType, String scopeKey) {
    jdbcTemplate.update(
        "UPDATE model_registry SET active=false WHERE active=true AND COALESCE(kind,'risk-model')=? AND COALESCE(scope_type,'GLOBAL')=? AND COALESCE(scope_key,'*')=?",
        kind, scopeType, scopeKey
    );
  }

  public void activateById(long id) {
    deactivateAll();
    jdbcTemplate.update("UPDATE model_registry SET active=true WHERE id=?", id);
  }

  public long insertReturningId(String name, String format, String version, byte[] bytes, String sha256, boolean active,
                                String kind, String scopeType, String scopeKey, String metricsJson) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO model_registry(name, format, version, bytes, sha256, active, kind, scope_type, scope_key, metrics_json) " +
            "VALUES (?,?,?,?,?,?,?,?,?, ?::jsonb) RETURNING id",
        Long.class,
        name, format, version, bytes, sha256, active, kind, scopeType, scopeKey, metricsJson == null ? "{}" : metricsJson
    );
  }
}
