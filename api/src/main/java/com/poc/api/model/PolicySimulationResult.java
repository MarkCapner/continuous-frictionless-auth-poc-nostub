package com.poc.api.model;

/**
 * EPIC 13.5: result of simulating a draft policy against a historical session.
 */
public record PolicySimulationResult(
        String sessionId,
        String userId,
        String beforeDecision,
        double beforeConfidence,
        boolean draftMatched,
        String afterDecision,
        double afterConfidence,
        String draftReason,
        boolean baselineMatched,
        Long baselinePolicyId,
        String baselinePolicyDescription
) {}
