package com.poc.api.risk.api.dto;

/**
 * EPIC X.8
 *
 * API DTO for explainable risk decisions.
 * Returned only when explicitly requested.
 */
public record DecisionExplainabilityDto(
    String decision,
    double confidence,
    String featureContributionsJson,
    String topPositiveContributorsJson,
    String topNegativeContributorsJson
) {}
