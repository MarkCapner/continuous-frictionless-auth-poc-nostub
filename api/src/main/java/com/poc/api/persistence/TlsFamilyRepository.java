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
        INSERT INTO tls_family (
          family_id, family_key, sample_tls_fp, sample_meta,
          created_at, first_seen, last_seen,
          seen_count, observation_count,
          variant_count, confidence_score, stability_score
        )
        VALUES (?, ?, ?, ?, now(), now(), now(), 1, 1, 1, NULL, NULL)
        ON CONFLICT (family_id) DO UPDATE SET
          last_seen = now(),
          seen_count = tls_family.seen_count + 1,
          observation_count = COALESCE(tls_family.observation_count, tls_family.seen_count, 0) + 1,
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
        SELECT f.family_id, f.family_key, f.sample_tls_fp, f.sample_meta,
               f.created_at, f.first_seen, f.last_seen,
               f.seen_count, f.observation_count, f.variant_count,
               f.confidence_score, f.stability_score
        FROM tls_family_member m
        JOIN tls_family f ON f.family_id = m.family_id
        WHERE m.raw_tls_fp = ?
        """;
    List<FamilyLookup> rows = jdbc.query(sql, (rs, rowNum) -> mapFamilyLookup(rs), rawTlsFp);
    return rows.stream().findFirst();
  }

  public List<TlsFamilySummaryRow> listFamilies(int limit) {
    String sql = """
        SELECT family_id, family_key, sample_tls_fp, created_at, first_seen, last_seen,
               seen_count,
               COALESCE(observation_count, seen_count) AS observation_count,
               COALESCE(variant_count, (SELECT COUNT(*) FROM tls_family_member m WHERE m.family_id = f.family_id)) AS variant_count,
               confidence_score,
               stability_score
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
      r.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      r.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      r.seenCount = rs.getLong("seen_count");
      r.observationCount = rs.getLong("observation_count");
      r.variantCount = rs.getLong("variant_count");
      r.confidence = (Double) rs.getObject("confidence_score");
      r.stability = (Double) rs.getObject("stability_score");
      return r;
    }, limit);
  }

  /**
   * EPIC 9.1.5: Recomputes and persists derived family stats (variant_count + scores).
   *
   * This is safe to call frequently and is idempotent.
   */
  public void recomputeFamilyStats(String familyId, double confidenceScore, double stabilityScore) {
    String sql = """
        UPDATE tls_family f
        SET
          first_seen = COALESCE(f.first_seen, f.created_at, now()),
          observation_count = COALESCE(f.observation_count, f.seen_count, 1),
          variant_count = (
            SELECT COUNT(*) FROM tls_family_member m WHERE m.family_id = f.family_id
          ),
          confidence_score = ?,
          stability_score = ?
        WHERE f.family_id = ?
        """;
    jdbc.update(sql, confidenceScore, stabilityScore, familyId);
  }

  public Optional<FamilyStats> getFamilyStats(String familyId) {
    String sql = """
        SELECT family_id, first_seen, last_seen,
               COALESCE(observation_count, seen_count, 1) AS observation_count,
               COALESCE(variant_count, (SELECT COUNT(*) FROM tls_family_member m WHERE m.family_id = f.family_id), 1) AS variant_count
        FROM tls_family f
        WHERE family_id = ?
        """;
    List<FamilyStats> rows = jdbc.query(sql, (rs, rowNum) -> {
      FamilyStats s = new FamilyStats();
      s.familyId = rs.getString("family_id");
      s.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      s.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      s.observationCount = rs.getLong("observation_count");
      s.variantCount = rs.getInt("variant_count");
      return s;
    }, familyId);
    return rows.stream().findFirst();
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
    f.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
    f.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
    f.seenCount = rs.getLong("seen_count");
    f.observationCount = rs.getLong("observation_count");
    f.variantCount = rs.getInt("variant_count");
    f.confidence = (Double) rs.getObject("confidence_score");
    f.stability = (Double) rs.getObject("stability_score");
    return f;
  }

  public static class FamilyLookup {
    public String familyId;
    public String familyKey;
    public String sampleTlsFp;
    public String sampleMeta;
    public OffsetDateTime createdAt;
    public OffsetDateTime firstSeen;
    public OffsetDateTime lastSeen;
    public long seenCount;
    public long observationCount;
    public int variantCount;
    public Double confidence;
    public Double stability;
  }

  public static class TlsFamilySummaryRow {
    public String familyId;
    public String familyKey;
    public String sampleTlsFp;
    public OffsetDateTime createdAt;
    public OffsetDateTime firstSeen;
    public OffsetDateTime lastSeen;
    public long seenCount;
    public long observationCount;
    public long variantCount;
    public Double confidence;
    public Double stability;
  }

  public static class FamilyStats {
    public String familyId;
    public OffsetDateTime firstSeen;
    public OffsetDateTime lastSeen;
    public long observationCount;
    public int variantCount;
  }
}
