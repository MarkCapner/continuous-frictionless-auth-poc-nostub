package com.poc.api.risk.model;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Result of explicit risk aggregation.
 * Pure data + helpers. No side effects.
 */
public record RiskAggregateResult(
    double finalScore,
    String decision,
    double confidence,
    List<FeatureContribution> contributions
) {

  public List<FeatureContribution> topPositive(int n) {
    return contributions.stream()
        .filter(c -> c.contribution() > 0)
        .sorted(Comparator.comparingDouble(c -> -Math.abs(c.contribution())))
        .limit(n)
        .collect(Collectors.toList());
  }

  public List<FeatureContribution> topNegative(int n) {
    return contributions.stream()
        .filter(c -> c.contribution() < 0)
        .sorted(Comparator.comparingDouble(c -> -Math.abs(c.contribution())))
        .limit(n)
        .collect(Collectors.toList());
  }
}
