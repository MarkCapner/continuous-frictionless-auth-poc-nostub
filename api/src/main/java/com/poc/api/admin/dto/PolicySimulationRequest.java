package com.poc.api.admin.dto;

/**
 * EPIC 13.5: request to simulate a draft policy against a stored session.
 *
 * sessionId corresponds to session_feature.request_id.
 */
public record PolicySimulationRequest(
        String sessionId,
        String tenantId,
        String userId,
        Object conditionJson,
        Object actionJson
) {}
