package com.poc.api.risk.counterfactual;

/**
 * Represents a minimal change suggestion that could
 * flip a decision outcome.
 */
public record CounterfactualSuggestion(
    String featureKey,
    double deltaRequired,
    String explanation
) {}
