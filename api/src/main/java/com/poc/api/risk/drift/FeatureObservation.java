package com.poc.api.risk.drift;

/**
 * Represents current vs baseline observation for a feature.
 */
public record FeatureObservation(
    String featureKey,
    double baselineValue,
    double currentValue
) {}
