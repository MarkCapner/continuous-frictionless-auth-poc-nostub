package com.poc.api.risk.api.dto;

public record FeatureDriftDto(
    String feature,
    double driftScore,
    String level,
    String explanation
) {}
