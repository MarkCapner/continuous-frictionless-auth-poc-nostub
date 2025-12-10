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
}
