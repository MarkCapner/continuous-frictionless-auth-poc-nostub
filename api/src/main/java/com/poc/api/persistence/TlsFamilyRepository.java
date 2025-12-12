package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TlsFamilyRepository {

  private final JdbcTemplate jdbc;

  public TlsFamilyRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void upsertFamily(String familyId, String familyKey, String sampleFp, String sampleMeta) {
    String sql = """
        INSERT INTO tls_family (family_id, family_key, sample_tls_fp, sample_meta, created_at, last_seen, seen_count)
        VALUES (?, ?, ?, ?, now(), now(), 1)
        ON CONFLICT (family_id) DO UPDATE SET
          last_seen = now(),
          seen_count = tls_family.seen_count + 1,
          sample_tls_fp = COALESCE(tls_family.sample_tls_fp, EXCLUDED.sample_tls_fp),
          sample_meta = COALESCE(tls_family.sample_meta, EXCLUDED.sample_meta)
        """;
    jdbc.update(sql, familyId, familyKey, sampleFp, sampleMeta);
  }

  public void upsertMember(String rawTlsFp, String familyId, String lastMeta) {
    String sql = """
        INSERT INTO tls_family_member (raw_tls_fp, family_id, first_seen, last_seen, seen_count, last_meta)
        VALUES (?, ?, now(), now(), 1, ?)
        ON CONFLICT (raw_tls_fp) DO UPDATE SET
          family_id = EXCLUDED.family_id,
          last_seen = now(),
          seen_count = tls_family_member.seen_count + 1,
          last_meta = COALESCE(EXCLUDED.last_meta, tls_family_member.last_meta)
        """;
    jdbc.update(sql, rawTlsFp, familyId, lastMeta);
  }

  /** @return true if it was newly created for this user. */
  public boolean upsertUserFamily(String userId, String familyId) {
    String sql = """
        INSERT INTO user_tls_family (user_id, family_id, first_seen, last_seen, seen_count)
        VALUES (?, ?, now(), now(), 1)
        ON CONFLICT (user_id, family_id) DO UPDATE SET
          last_seen = now(),
          seen_count = user_tls_family.seen_count + 1
        """;
    // JdbcTemplate doesn't expose insert-vs-update easily; do a lightweight existence check.
    boolean existed = jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM user_tls_family WHERE user_id=? AND family_id=?)",
        Boolean.class,
        userId, familyId
    );
    jdbc.update(sql, userId, familyId);
    return !existed;
  }

  public Optional<FamilyLookup> findFamilyByRawFp(String rawTlsFp) {
    String sql = """
        SELECT f.family_id, f.family_key, f.sample_tls_fp, f.sample_meta, f.created_at, f.last_seen, f.seen_count
        FROM tls_family_member m
        JOIN tls_family f ON f.family_id = m.family_id
        WHERE m.raw_tls_fp = ?
        """;
    List<FamilyLookup> rows = jdbc.query(sql, (rs, rowNum) -> mapFamilyLookup(rs), rawTlsFp);
    return rows.stream().findFirst();
  }

  public List<TlsFamilySummaryRow> listFamilies(int limit) {
    String sql = """
        SELECT family_id, family_key, sample_tls_fp, created_at, last_seen, seen_count,
               (SELECT COUNT(*) FROM tls_family_member m WHERE m.family_id = f.family_id) AS variants
        FROM tls_family f
        ORDER BY last_seen DESC
        LIMIT ?
        """;
    return jdbc.query(sql, (rs, rowNum) -> {
      TlsFamilySummaryRow r = new TlsFamilySummaryRow();
      r.familyId = rs.getString("family_id");
      r.familyKey = rs.getString("family_key");
      r.sampleTlsFp = rs.getString("sample_tls_fp");
      r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
      r.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      r.seenCount = rs.getLong("seen_count");
      r.variants = rs.getLong("variants");
      return r;
    }, limit);
  }

  public List<String> listVariants(String familyId, int limit) {
    String sql = """
        SELECT raw_tls_fp
        FROM tls_family_member
        WHERE family_id=?
        ORDER BY last_seen DESC
        LIMIT ?
        """;
    return jdbc.query(sql, (rs, rowNum) -> rs.getString("raw_tls_fp"), familyId, limit);
  }

  public long countUsersForFamily(String familyId) {
    return jdbc.queryForObject(
        "SELECT COUNT(DISTINCT user_id) FROM user_tls_family WHERE family_id=?",
        Long.class,
        familyId
    );
  }

  /**
   * EPIC 9.1.4: Lists raw TLS fingerprints observed in historical tables which
   * are not yet present in {@code tls_family_member}.
   *
   * Paging is lexicographic on tls_fp using {@code afterFp} as an exclusive cursor.
   */
  public List<String> listUnclassifiedObservedTlsFps(String afterFp, int limit) {
    String cursor = (afterFp == null) ? "" : afterFp;
    String sql = """
        WITH observed AS (
          SELECT tls_fp FROM session_feature
          UNION
          SELECT tls_fp FROM device_profile
          UNION
          SELECT tls_fp FROM decision_log
        )
        SELECT o.tls_fp
        FROM observed o
        WHERE o.tls_fp IS NOT NULL
          AND o.tls_fp <> ''
          AND o.tls_fp > ?
          AND NOT EXISTS (SELECT 1 FROM tls_family_member m WHERE m.raw_tls_fp = o.tls_fp)
        ORDER BY o.tls_fp
        LIMIT ?
        """;
    return jdbc.query(sql, (rs, rowNum) -> rs.getString("tls_fp"), cursor, limit);
  }

  /**
   * EPIC 9.1.4: Fetches the most recent tls_meta string we have observed for a TLS FP.
   * This is best-effort (may be null), and is used to improve family normalisation.
   */
  public String findLatestTlsMetaForFp(String tlsFp) {
    String sql = """
        SELECT context_json->>'tls_meta' AS tls_meta
        FROM session_feature
        WHERE tls_fp = ?
          AND context_json ? 'tls_meta'
          AND (context_json->>'tls_meta') IS NOT NULL
          AND (context_json->>'tls_meta') <> ''
        ORDER BY occurred_at DESC
        LIMIT 1
        """;
    List<String> rows = jdbc.query(sql, (rs, rowNum) -> rs.getString("tls_meta"), tlsFp);
    return rows.isEmpty() ? null : rows.get(0);
  }

  private static FamilyLookup mapFamilyLookup(ResultSet rs) throws SQLException {
    FamilyLookup f = new FamilyLookup();
    f.familyId = rs.getString("family_id");
    f.familyKey = rs.getString("family_key");
    f.sampleTlsFp = rs.getString("sample_tls_fp");
    f.sampleMeta = rs.getString("sample_meta");
    f.createdAt = rs.getObject("created_at", OffsetDateTime.class);
    f.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
    f.seenCount = rs.getLong("seen_count");
    return f;
  }

  public static class FamilyLookup {
    public String familyId;
    public String familyKey;
    public String sampleTlsFp;
    public String sampleMeta;
    public OffsetDateTime createdAt;
    public OffsetDateTime lastSeen;
    public long seenCount;
  }

  public static class TlsFamilySummaryRow {
    public String familyId;
    public String familyKey;
    public String sampleTlsFp;
    public OffsetDateTime createdAt;
    public OffsetDateTime lastSeen;
    public long seenCount;
    public long variants;
  }
}
