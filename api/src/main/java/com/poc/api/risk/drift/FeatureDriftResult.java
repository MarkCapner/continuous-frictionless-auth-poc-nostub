package com.poc.api.risk.drift;

/**
 * Drift explanation result for a feature.
 */
public record FeatureDriftResult(
    String featureKey,
    double driftScore,
    DriftLevel level,
    String explanation
) {}
