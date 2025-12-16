package com.poc.api.risk.aggregate;

import java.util.ArrayList;
import java.util.List;

import com.poc.api.risk.model.FeatureContribution;
import com.poc.api.risk.model.RiskAggregateResult;
import com.poc.api.risk.model.RiskSignal;

/**
 * Pure, deterministic risk aggregation engine.
 * No side effects, no persistence, no external dependencies.
 */
public class RiskAggregationEngine {

  public RiskAggregateResult evaluate(List<RiskSignal> signals) {
    List<FeatureContribution> contributions = new ArrayList<>();
    double total = 0.0;

    for (RiskSignal s : signals) {
      double contribution = s.value() * s.weight();
      total += contribution;
      contributions.add(
          new FeatureContribution(
              s.key(),
              s.value(),
              s.weight(),
              contribution
          )
      );
    }

    String decision = total >= 0 ? "ALLOW" : "DENY";
    double confidence = Math.min(1.0, Math.abs(total));

    return new RiskAggregateResult(
        total,
        decision,
        confidence,
        contributions
    );
  }
}
