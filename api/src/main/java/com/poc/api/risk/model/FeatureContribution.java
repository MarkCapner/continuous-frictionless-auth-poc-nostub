package com.poc.api.risk.model;

/**
 * Signed contribution of a feature to the final risk score.
 */
public record FeatureContribution(
    String key,
    double rawValue,
    double weight,
    double contribution
) {}
