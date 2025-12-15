package com.poc.api.persistence;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SessionFeatureRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<SessionFeatureRow> mapper = (rs, rowNum) -> {
        SessionFeatureRow row = new SessionFeatureRow();
        row.id = rs.getLong("id");
        row.occurredAt = rs.getObject("occurred_at", java.time.OffsetDateTime.class);
        row.userId = rs.getString("user_id");
        row.requestId = rs.getString("request_id");
        row.tlsFp = rs.getString("tls_fp");
        row.deviceJson = rs.getString("device_json");
        row.behaviorJson = rs.getString("behavior_json");
        row.contextJson = rs.getString("context_json");
        row.featureVector = rs.getString("feature_vector");
        row.decision = rs.getString("decision");
        row.confidence = rs.getDouble("confidence");
        row.label = rs.getString("label");
        return row;
    };

    public SessionFeatureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String userId,
                       String requestId,
                       String tlsFp,
                       String deviceJson,
                       String behaviorJson,
                       String contextJson,
                       String featureVectorJson,
                       String decision,
                       double confidence,
                       String label) {

        jdbcTemplate.update(
                """
                INSERT INTO session_feature
                (user_id, request_id, tls_fp, device_json, behavior_json, context_json, feature_vector, decision, confidence, label)
                VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?)
                """,
                userId,
                requestId,
                tlsFp,
                deviceJson,
                behaviorJson,
                contextJson,
                featureVectorJson,
                decision,
                confidence,
                label
        );
    }

    public java.util.List<SessionFeatureRow> findRecentWithLabel(int limit) {
        String sql = """
            SELECT id,
                   occurred_at,
                   user_id,
                   request_id,
                   tls_fp,
                   device_json::text AS device_json,
                   behavior_json::text AS behavior_json,
                   context_json::text AS context_json,
                   feature_vector::text AS feature_vector,
                   decision,
                   confidence,
                   label
              FROM session_feature
             WHERE label IS NOT NULL
             ORDER BY occurred_at DESC
             LIMIT ?
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SessionFeatureRow row = new SessionFeatureRow();
            row.id = rs.getLong("id");
            row.occurredAt = rs.getObject("occurred_at", java.time.OffsetDateTime.class);
            row.userId = rs.getString("user_id");
            row.requestId = rs.getString("request_id");
            row.tlsFp = rs.getString("tls_fp");
            row.deviceJson = rs.getString("device_json");
            row.behaviorJson = rs.getString("behavior_json");
            row.contextJson = rs.getString("context_json");
            row.featureVector = rs.getString("feature_vector");
            row.decision = rs.getString("decision");
            row.confidence = rs.getDouble("confidence");
            row.label = rs.getString("label");
            return row;
        }, limit);
    }


    public java.util.List<SessionFeatureRow> findRecentForUser(String userId, int limit) {
        String sql = """
            SELECT id,
                   occurred_at,
                   user_id,
                   request_id,
                   tls_fp,
                   device_json::text AS device_json,
                   behavior_json::text AS behavior_json,
                   context_json::text AS context_json,
                   feature_vector::text AS feature_vector,
                   decision,
                   confidence,
                   label
            FROM session_feature
            WHERE user_id = ?
            ORDER BY occurred_at DESC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SessionFeatureRow row = new SessionFeatureRow();
            row.id = rs.getLong("id");
            row.occurredAt = rs.getObject("occurred_at", java.time.OffsetDateTime.class);
            row.userId = rs.getString("user_id");
            row.requestId = rs.getString("request_id");
            row.tlsFp = rs.getString("tls_fp");
            row.deviceJson = rs.getString("device_json");
            row.behaviorJson = rs.getString("behavior_json");
            row.contextJson = rs.getString("context_json");
            row.featureVector = rs.getString("feature_vector");
            row.decision = rs.getString("decision");
            row.confidence = rs.getDouble("confidence");
            row.label = rs.getString("label");
            return row;
        }, userId, limit);
    }
public Optional<SessionFeatureRow> findByRequestId(String requestId) {
    try {
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                """
                SELECT id, occurred_at, user_id, request_id, tls_fp, device_json, behavior_json, context_json,
                       feature_vector, decision, confidence, label
                FROM session_feature
                WHERE request_id = ?
                ORDER BY occurred_at DESC
                LIMIT 1
                """,
                mapper,
                requestId
        ));
    } catch (EmptyResultDataAccessException ex) {
        return Optional.empty();
    }
}

    /**
     * Finds the most recent prior session for this user that looks "trusted" enough to act as a baseline.
     *
     * Note: This is intentionally a simple heuristic for PoC transparency features.
     */
    public Optional<SessionFeatureRow> findLastTrustedBefore(String userId,
                                                            java.time.OffsetDateTime before,
                                                            double minConfidence) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id, occurred_at, user_id, request_id, tls_fp,
                           device_json::text AS device_json,
                           behavior_json::text AS behavior_json,
                           context_json::text AS context_json,
                           feature_vector::text AS feature_vector,
                           decision, confidence, label
                    FROM session_feature
                    WHERE user_id = ?
                      AND occurred_at < ?
                      AND decision = 'ALLOW'
                      AND confidence >= ?
                    ORDER BY occurred_at DESC
                    LIMIT 1
                    """,
                    mapper,
                    userId,
                    before,
                    minConfidence
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

}