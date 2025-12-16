package com.poc.api.risk.aggregate;

import java.util.List;

import com.poc.api.risk.attribution.DecisionAttributionBuilder;
import com.poc.api.risk.model.RiskAggregateResult;
import com.poc.api.risk.model.RiskSignal;

/**
 * EPIC X.5
 *
 * Centralizes explicit risk aggregation at decision creation time.
 * This is the ONLY place aggregation should occur.
 */
public class AggregatedDecisionApplier {

  private final RiskAggregationEngine engine = new RiskAggregationEngine();

  public RiskAggregateResult aggregate(List<RiskSignal> signals) {
    return engine.evaluate(signals);
  }
}
