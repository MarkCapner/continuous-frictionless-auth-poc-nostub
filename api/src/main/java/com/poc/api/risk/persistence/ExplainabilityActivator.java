package com.poc.api.risk.persistence;

import com.poc.api.risk.attribution.DecisionAttributionBuilder;
import com.poc.api.risk.pipeline.RiskDecisionPipeline;

/**
 * Explicit activation point for explainability persistence.
 */
public final class ExplainabilityActivator {

  private static final RiskDecisionPipeline PIPELINE =
      new RiskDecisionPipeline();

  private ExplainabilityActivator() {}

  public static void apply(
      DecisionWritable decision,
      DecisionAttributionBuilder attribution
  ) {
    PIPELINE.apply(decision, attribution);
  }
}
