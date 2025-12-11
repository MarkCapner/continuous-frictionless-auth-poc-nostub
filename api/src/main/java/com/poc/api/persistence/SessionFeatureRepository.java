package com.poc.api.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionFeatureRepository {

    private final JdbcTemplate jdbcTemplate;

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

}