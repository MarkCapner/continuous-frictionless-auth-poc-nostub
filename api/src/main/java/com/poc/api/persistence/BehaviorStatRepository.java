package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class BehaviorStatRepository {

  private final JdbcTemplate jdbc;

  public BehaviorStatRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<BehaviorStat> findByUserAndFeature(String userId, String feature) {
    String sql = "SELECT * FROM behavior_profile_stats WHERE user_id = ? AND feature = ?";
    var list = jdbc.query(sql, (rs, rowNum) -> {
      BehaviorStat s = new BehaviorStat();
      s.id = rs.getLong("id");
      s.userId = rs.getString("user_id");
      s.feature = rs.getString("feature");
      s.mean = rs.getDouble("mean");
      s.variance = rs.getDouble("variance");
      s.decay = rs.getDouble("decay");
      s.updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
      return s;
    }, userId, feature);
    if (list.isEmpty()) return Optional.empty();
    return Optional.of(list.get(0));
  }


  public java.util.List<BehaviorStat> findRecent(int limit) {
    String sql = """
        SELECT id, user_id, feature, mean, variance, decay, updated_at
        FROM behavior_profile_stats
        ORDER BY updated_at DESC
        LIMIT ?
        """;
    return jdbc.query(sql, (rs, rowNum) -> {
      BehaviorStat s = new BehaviorStat();
      s.id = rs.getLong("id");
      s.userId = rs.getString("user_id");
      s.feature = rs.getString("feature");
      s.mean = rs.getDouble("mean");
      s.variance = rs.getDouble("variance");
      s.decay = rs.getDouble("decay");
      s.updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
      return s;
    }, limit);
  }

  public BehaviorStat save(BehaviorStat s) {
    if (s.id == null) {
      String sql = "INSERT INTO behavior_profile_stats (user_id, feature, mean, variance, decay, updated_at) VALUES (?,?,?,?,?,?)";
      jdbc.update(sql, s.userId, s.feature, s.mean, s.variance, s.decay, OffsetDateTime.now());
    } else {
      String sql = "UPDATE behavior_profile_stats SET mean = ?, variance = ?, decay = ?, updated_at = ? WHERE id = ?";
      jdbc.update(sql, s.mean, s.variance, s.decay, OffsetDateTime.now(), s.id);
    }
    return s;
  }
}
