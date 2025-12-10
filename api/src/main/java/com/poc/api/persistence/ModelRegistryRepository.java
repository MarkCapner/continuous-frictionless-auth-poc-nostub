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
      OffsetDateTime createdAt
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
          rs.getObject("created_at", OffsetDateTime.class)
      );
    }
  };

  public ModelRegistryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<ModelRecord> findActive() {
    List<ModelRecord> list = jdbcTemplate.query(
        "SELECT id, name, format, version, bytes, sha256, created_at " +
            "FROM model_registry WHERE active = true ORDER BY id DESC LIMIT 1",
        rowMapper
    );
    return list.stream().findFirst();
  }

  public void deactivateAll() {
    jdbcTemplate.update("UPDATE model_registry SET active = false WHERE active = true");
  }

  public void insert(String name, String format, String version, byte[] bytes, String sha256, boolean active) {
    jdbcTemplate.update(
        "INSERT INTO model_registry(name, format, version, bytes, sha256, active) VALUES (?,?,?,?,?,?)",
        name, format, version, bytes, sha256, active
    );
  }
}
