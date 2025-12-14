package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EPIC 10.1
 * Persistence layer for the identity graph (nodes + probabilistic links).
 *
 * Note: EPIC 10.1 deliberately avoids inference logic; EPIC 10.2 will populate these tables.
 */
@Repository
public class IdentityGraphRepository {

  private final JdbcTemplate jdbc;

  public IdentityGraphRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<IdentityNodeRow> findNode(IdentityNodeType type, String naturalKey) {
    String sql = """
        SELECT id, node_type, natural_key, display_label, meta_json, created_at, last_seen
          FROM identity_node
         WHERE node_type = ? AND natural_key = ?
        """;

    var list = jdbc.query(sql, (rs, rowNum) -> {
      IdentityNodeRow r = new IdentityNodeRow();
      r.id = rs.getLong("id");
      r.nodeType = IdentityNodeType.valueOf(rs.getString("node_type"));
      r.naturalKey = rs.getString("natural_key");
      r.displayLabel = rs.getString("display_label");
      r.metaJson = rs.getString("meta_json");
      r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
      r.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      return r;
    }, type.name(), naturalKey);

    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  /**
   * Upserts a node by (type,naturalKey) and bumps last_seen.
   */
  public IdentityNodeRow upsertNode(IdentityNodeType type, String naturalKey, String displayLabel, String metaJson) {
    Optional<IdentityNodeRow> existing = findNode(type, naturalKey);
    OffsetDateTime now = OffsetDateTime.now();
    if (existing.isPresent()) {
      String sql = """
          UPDATE identity_node
             SET display_label = COALESCE(?, display_label),
                 meta_json = COALESCE(?::jsonb, meta_json),
                 last_seen = ?
           WHERE id = ?
          """;
      jdbc.update(sql, displayLabel, metaJson, now, existing.get().id);
      IdentityNodeRow r = existing.get();
      r.displayLabel = (displayLabel != null) ? displayLabel : r.displayLabel;
      r.metaJson = (metaJson != null) ? metaJson : r.metaJson;
      r.lastSeen = now;
      return r;
    }

    String sql = """
        INSERT INTO identity_node (node_type, natural_key, display_label, meta_json, created_at, last_seen)
        VALUES (?,?,?,?::jsonb,?,?)
        """;

    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, type.name());
      ps.setString(2, naturalKey);
      ps.setString(3, displayLabel);
      ps.setString(4, metaJson);
      ps.setObject(5, now);
      ps.setObject(6, now);
      return ps;
    }, kh);

    IdentityNodeRow r = new IdentityNodeRow();
    r.id = extractId(kh);
    r.nodeType = type;
    r.naturalKey = naturalKey;
    r.displayLabel = displayLabel;
    r.metaJson = metaJson;
    r.createdAt = now;
    r.lastSeen = now;
    return r;
  }

  public Optional<IdentityLinkRow> findLink(long fromNodeId, long toNodeId, IdentityLinkType linkType) {
    String sql = """
        SELECT id, from_node_id, to_node_id, link_type, confidence, reason, evidence_json, first_seen, last_seen
          FROM identity_link
         WHERE from_node_id = ? AND to_node_id = ? AND link_type = ?
        """;
    var list = jdbc.query(sql, (rs, rowNum) -> {
      IdentityLinkRow r = new IdentityLinkRow();
      r.id = rs.getLong("id");
      r.fromNodeId = rs.getLong("from_node_id");
      r.toNodeId = rs.getLong("to_node_id");
      r.linkType = IdentityLinkType.valueOf(rs.getString("link_type"));
      r.confidence = rs.getBigDecimal("confidence").doubleValue();
      r.reason = rs.getString("reason");
      r.evidenceJson = rs.getString("evidence_json");
      r.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      r.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      return r;
    }, fromNodeId, toNodeId, linkType.name());
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  /**
   * Upserts a link and bumps last_seen. Confidence is overwritten with the latest value.
   */
  public IdentityLinkRow upsertLink(long fromNodeId, long toNodeId, IdentityLinkType linkType, double confidence, String reason, String evidenceJson) {
    Optional<IdentityLinkRow> existing = findLink(fromNodeId, toNodeId, linkType);
    OffsetDateTime now = OffsetDateTime.now();
    if (existing.isPresent()) {
      String sql = """
          UPDATE identity_link
             SET confidence = ?,
                 reason = COALESCE(?, reason),
                 evidence_json = COALESCE(?::jsonb, evidence_json),
                 last_seen = ?
           WHERE id = ?
          """;
      jdbc.update(sql, confidence, reason, evidenceJson, now, existing.get().id);
      IdentityLinkRow r = existing.get();
      r.confidence = confidence;
      r.reason = (reason != null) ? reason : r.reason;
      r.evidenceJson = (evidenceJson != null) ? evidenceJson : r.evidenceJson;
      r.lastSeen = now;
      return r;
    }

    String sql = """
        INSERT INTO identity_link (from_node_id, to_node_id, link_type, confidence, reason, evidence_json, first_seen, last_seen)
        VALUES (?,?,?,?,?,?::jsonb,?,?)
        """;

    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      ps.setLong(1, fromNodeId);
      ps.setLong(2, toNodeId);
      ps.setString(3, linkType.name());
      ps.setDouble(4, confidence);
      ps.setString(5, reason);
      ps.setString(6, evidenceJson);
      ps.setObject(7, now);
      ps.setObject(8, now);
      return ps;
    }, kh);

    IdentityLinkRow r = new IdentityLinkRow();
    r.id = extractId(kh);
    r.fromNodeId = fromNodeId;
    r.toNodeId = toNodeId;
    r.linkType = linkType;
    r.confidence = confidence;
    r.reason = reason;
    r.evidenceJson = evidenceJson;
    r.firstSeen = now;
    r.lastSeen = now;
    return r;
  }

  public List<IdentityLinkRow> listLinksForNode(long nodeId, int limit) {
    String sql = """
        SELECT id, from_node_id, to_node_id, link_type, confidence, reason, evidence_json, first_seen, last_seen
          FROM identity_link
         WHERE from_node_id = ? OR to_node_id = ?
         ORDER BY last_seen DESC
         LIMIT ?
        """;
    return jdbc.query(sql, (rs, rowNum) -> {
      IdentityLinkRow r = new IdentityLinkRow();
      r.id = rs.getLong("id");
      r.fromNodeId = rs.getLong("from_node_id");
      r.toNodeId = rs.getLong("to_node_id");
      r.linkType = IdentityLinkType.valueOf(rs.getString("link_type"));
      r.confidence = rs.getBigDecimal("confidence").doubleValue();
      r.reason = rs.getString("reason");
      r.evidenceJson = rs.getString("evidence_json");
      r.firstSeen = rs.getObject("first_seen", OffsetDateTime.class);
      r.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
      return r;
    }, nodeId, nodeId, limit);
  }

  private static long extractId(KeyHolder kh) {
    var keys = kh.getKeys();
    if (keys != null) {
      Object idVal = keys.get("id");
      if (idVal instanceof Number n) return n.longValue();
    }
    // Fallback for some drivers
    Number n = kh.getKey();
    return n != null ? n.longValue() : 0L;
  }


public Optional<IdentityNodeRow> findNodeById(long id) {
  String sql = "SELECT id, node_type, natural_key, display_label, meta_json, created_at, last_seen FROM identity_node WHERE id = ?";
  var list = jdbc.query(sql, (rs, rowNum) -> {
    IdentityNodeRow r = new IdentityNodeRow();
    r.id = rs.getLong("id");
    r.nodeType = IdentityNodeType.valueOf(rs.getString("node_type"));
    r.naturalKey = rs.getString("natural_key");
    r.displayLabel = rs.getString("display_label");
    r.metaJson = rs.getString("meta_json");
    r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
    r.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
    return r;
  }, id);
  return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
}


public List<IdentityNodeRow> listNodesByIds(List<Long> ids) {
  if (ids == null || ids.isEmpty()) return List.of();
  String in = String.join(",", ids.stream().map(x -> "?").toList());
  String sql = "SELECT id, node_type, natural_key, display_label, meta_json, created_at, last_seen FROM identity_node WHERE id IN (" + in + ")";
  Object[] args = ids.toArray();
  return jdbc.query(sql, (rs, rowNum) -> {
    IdentityNodeRow r = new IdentityNodeRow();
    r.id = rs.getLong("id");
    r.nodeType = IdentityNodeType.valueOf(rs.getString("node_type"));
    r.naturalKey = rs.getString("natural_key");
    r.displayLabel = rs.getString("display_label");
    r.metaJson = rs.getString("meta_json");
    r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
    r.lastSeen = rs.getObject("last_seen", OffsetDateTime.class);
    return r;
  }, args);
}
}
