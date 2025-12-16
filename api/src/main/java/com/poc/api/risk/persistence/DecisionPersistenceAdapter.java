package com.poc.api.risk.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.risk.model.RiskAggregateResult;

/**
 * EPIC X.6
 *
 * Canonical adapter responsible for persisting
 * RiskAggregateResult explainability data.
 *
 * This adapter is intentionally NOT wired yet.
 */
public class DecisionPersistenceAdapter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public void applyAggregation(
      DecisionWritable decision,
      RiskAggregateResult result
  ) {
    try {
      decision.setDecision(result.decision());
      decision.setConfidence(result.confidence());
      decision.setFeatureContributionsJson(
          objectMapper.writeValueAsString(result.contributions())
      );
      decision.setTopPositiveContributorsJson(
          objectMapper.writeValueAsString(result.topPositive(5))
      );
      decision.setTopNegativeContributorsJson(
          objectMapper.writeValueAsString(result.topNegative(5))
      );
    } catch (Exception e) {
      // never allow explainability to break persistence
    }
  }
}
