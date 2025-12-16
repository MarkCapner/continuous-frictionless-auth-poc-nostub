package com.poc.api.risk.model;

public record FeatureContribution(
    String key,
    double rawValue,
    double weight,
    double contribution
) {}
