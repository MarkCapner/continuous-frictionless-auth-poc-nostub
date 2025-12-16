package com.poc.api.risk.pipeline;

import com.poc.api.risk.attribution.DecisionAttributionBuilder;
import com.poc.api.risk.aggregate.AggregatedDecisionApplier;
import com.poc.api.risk.model.RiskAggregateResult;
import com.poc.api.risk.persistence.DecisionPersistenceAdapter;
import com.poc.api.risk.persistence.DecisionWritable;

/**
 * EPIC X.7
 *
 * Canonical end-to-end wiring of the risk decision pipeline.
 * This class is intentionally NOT auto-wired into controllers yet.
 */
public class RiskDecisionPipeline {

  private final AggregatedDecisionApplier applier =
      new AggregatedDecisionApplier();

  private final DecisionPersistenceAdapter persistenceAdapter =
      new DecisionPersistenceAdapter();

  public void apply(
      DecisionWritable decision,
      DecisionAttributionBuilder attributionBuilder
  ) {
    RiskAggregateResult result =
        applier.aggregate(attributionBuilder.build());

    persistenceAdapter.applyAggregation(decision, result);
  }
}
