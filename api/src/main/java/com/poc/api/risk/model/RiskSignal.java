package com.poc.api.risk.model;

/**
 * Canonical representation of a risk signal prior to aggregation.
 * Pure data object – no logic, no side effects.
 */
public record RiskSignal(
    String key,
    double value,
    double weight,
    String source
) {}
