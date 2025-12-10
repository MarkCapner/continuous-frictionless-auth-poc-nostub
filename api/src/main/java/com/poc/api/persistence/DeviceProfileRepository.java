package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.Optional;

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
      Number key = kh.getKey();
      p.id = key != null ? key.longValue() : null;
      p.firstSeen = now;
      p.lastSeen = now;
      p.seenCount = 1L;
      return p;
    }
  }
}
