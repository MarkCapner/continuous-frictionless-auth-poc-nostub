package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public class DeviceProfileRepository {

  private final JdbcTemplate jdbc;

  public DeviceProfileRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<DeviceProfile> findByUserAndTlsAndCanvas(String userId, String tlsFp, String canvasHash) {
    String sql = "SELECT * FROM device_profile WHERE user_id = ? AND tls_fp = ? AND canvas_hash = ?";
    var list = jdbc.query(sql, (rs, rowNum) -> {
      DeviceProfile d = new DeviceProfile();
      d.id = rs.getLong("id");
      d.userId = rs.getString("user_id");
      d.tlsFp = rs.getString("tls_fp");
      d.uaFamily = rs.getString("ua_family");
      d.uaVersion = rs.getString("ua_version");
      d.screenW = rs.getInt("screen_w");
      d.screenH = rs.getInt("screen_h");
      d.pixelRatio = rs.getDouble("pixel_ratio");
      d.tzOffset = rs.getShort("tz_offset");
      d.canvasHash = rs.getString("canvas_hash");
      d.webglHash = rs.getString("webgl_hash");
      d.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      d.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      d.seenCount = rs.getLong("seen_count");
      d.lastCountry = rs.getString("last_country");
      return d;
    }, userId, tlsFp, canvasHash);
    if (list.isEmpty()) return Optional.empty();
    return Optional.of(list.get(0));
  }

  public DeviceProfile upsert(DeviceProfile p) {
    Optional<DeviceProfile> existingOpt = findByUserAndTlsAndCanvas(p.userId, p.tlsFp, p.canvasHash);
    if (existingOpt.isPresent()) {
      DeviceProfile existing = existingOpt.get();
      existing.lastSeen = OffsetDateTime.now();
      existing.seenCount = existing.seenCount + 1;
      existing.lastCountry = p.lastCountry != null ? p.lastCountry : existing.lastCountry;
      String sql = "UPDATE device_profile SET last_seen = ?, seen_count = ?, last_country = ? WHERE id = ?";
      jdbc.update(sql, existing.lastSeen, existing.seenCount, existing.lastCountry, existing.id);
      return existing;
    } else {
      String sql = "INSERT INTO device_profile " +
          "(user_id, tls_fp, ua_family, ua_version, screen_w, screen_h, pixel_ratio, tz_offset, canvas_hash, webgl_hash, first_seen, last_seen, seen_count, last_country) " +
          "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      KeyHolder kh = new GeneratedKeyHolder();
      OffsetDateTime now = OffsetDateTime.now();
      jdbc.update(con -> {
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, p.userId);
        ps.setString(2, p.tlsFp);
        ps.setString(3, p.uaFamily);
        ps.setString(4, p.uaVersion);
        ps.setInt(5, p.screenW);
        ps.setInt(6, p.screenH);
        ps.setDouble(7, p.pixelRatio);
        ps.setShort(8, p.tzOffset);
        ps.setString(9, p.canvasHash);
        ps.setString(10, p.webglHash);
        ps.setObject(11, now);
        ps.setObject(12, now);
        ps.setLong(13, 1L);
        ps.setString(14, p.lastCountry);
        return ps;
      }, kh);
      java.util.Map<String,Object> keys = kh.getKeys();
      if (keys != null) {
        Object idVal = keys.get("id");
        if (idVal instanceof Number n) {
          p.id = n.longValue();
        }
      }
      p.firstSeen = now;
      p.lastSeen = now;
      p.seenCount = 1L;
      return p;
    }
  }

  public java.util.List<DeviceProfile> findByUser(String userId) {
    String sql = """
        SELECT id, user_id, tls_fp, ua_family, ua_version, screen_w, screen_h,
               pixel_ratio, tz_offset, canvas_hash, webgl_hash,
               first_seen, last_seen, seen_count, last_country
          FROM device_profile
         WHERE user_id = ?
         ORDER BY last_seen DESC
        """;

    return jdbc.query(sql, (rs, rowNum) -> {
      DeviceProfile p = new DeviceProfile();
      p.id = rs.getLong("id");
      p.userId = rs.getString("user_id");
      p.tlsFp = rs.getString("tls_fp");
      p.uaFamily = rs.getString("ua_family");
      p.uaVersion = rs.getString("ua_version");
      p.screenW = rs.getInt("screen_w");
      p.screenH = rs.getInt("screen_h");
      p.pixelRatio = rs.getDouble("pixel_ratio");
      p.tzOffset = rs.getShort("tz_offset");
      p.canvasHash = rs.getString("canvas_hash");
      p.webglHash = rs.getString("webgl_hash");
      p.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      p.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      p.seenCount = rs.getLong("seen_count");
      p.lastCountry = rs.getString("last_country");
      return p;
    }, userId);
  }


  public Optional<TlsFingerprintStatsRow> findTlsStats(String tlsFp) {
    String sql = """
        SELECT
          tls_fp,
          COUNT(*) AS profiles,
          COUNT(DISTINCT user_id) AS users,
          MIN(first_seen) AS first_seen,
          MAX(last_seen) AS last_seen
        FROM device_profile
        WHERE tls_fp = ?
        GROUP BY tls_fp
        """;

    var list = jdbc.query(sql, (rs, rowNum) -> {
      TlsFingerprintStatsRow row = new TlsFingerprintStatsRow();
      row.tlsFp = rs.getString("tls_fp");
      row.profiles = rs.getLong("profiles");
      row.users = rs.getLong("users");
      row.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      row.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      return row;
    }, tlsFp);

    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }


  public java.util.List<TlsFingerprintDeviceRow> findDevicesByTlsFp(String tlsFp) {
    String sql = """
        SELECT
          user_id,
          ua_family,
          ua_version,
          screen_w,
          screen_h,
          pixel_ratio,
          last_country,
          seen_count,
          first_seen,
          last_seen
        FROM device_profile
        WHERE tls_fp = ?
        ORDER BY last_seen DESC
        """;

    return jdbc.query(sql, (rs, rowNum) -> {
      TlsFingerprintDeviceRow row = new TlsFingerprintDeviceRow();
      row.userId = rs.getString("user_id");
      row.uaFamily = rs.getString("ua_family");
      row.uaVersion = rs.getString("ua_version");
      row.screenW = rs.getInt("screen_w");
      row.screenH = rs.getInt("screen_h");
      row.pixelRatio = rs.getDouble("pixel_ratio");
      row.lastCountry = rs.getString("last_country");
      row.seenCount = rs.getLong("seen_count");
      row.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      row.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      return row;
    }, tlsFp);
  }

  public java.util.List<TlsFingerprintStatsRow> findAllTlsStats(int limit) {
    String sql = """
        SELECT
          tls_fp,
          COUNT(*) AS profiles,
          COUNT(DISTINCT user_id) AS users,
          MIN(first_seen) AS first_seen,
          MAX(last_seen) AS last_seen
        FROM device_profile
        GROUP BY tls_fp
        ORDER BY last_seen DESC
        LIMIT ?
        """;

    return jdbc.query(sql, (rs, rowNum) -> {
      TlsFingerprintStatsRow row = new TlsFingerprintStatsRow();
      row.tlsFp = rs.getString("tls_fp");
      row.profiles = rs.getLong("profiles");
      row.users = rs.getLong("users");
      row.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      row.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      return row;
    }, limit);
  }

}