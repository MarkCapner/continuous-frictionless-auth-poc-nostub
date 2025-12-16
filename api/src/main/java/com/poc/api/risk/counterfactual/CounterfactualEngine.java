package com.poc.api.risk.counterfactual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.poc.api.risk.model.FeatureContribution;
import com.poc.api.risk.model.RiskAggregateResult;

/**
 * EPIC X.10
 *
 * Computes simple counterfactual suggestions:
 * "Which minimal feature changes would flip the decision?"
 *
 * Pure logic, no persistence, no side effects.
 */
public class CounterfactualEngine {

  public List<CounterfactualSuggestion> suggest(RiskAggregateResult result) {
    List<CounterfactualSuggestion> suggestions = new ArrayList<>();

    double target = result.decision().equals("ALLOW") ? -result.finalScore() : Math.abs(result.finalScore());

    result.contributions().stream()
        .sorted(Comparator.comparingDouble(c -> -Math.abs(c.contribution())))
        .forEach(c -> {
          double deltaNeeded = target / (c.weight() == 0 ? 1.0 : c.weight());
          suggestions.add(
              new CounterfactualSuggestion(
                  c.key(),
                  deltaNeeded,
                  "Adjust " + c.key() + " by " + String.format("%.3f", deltaNeeded)
              )
          );
        });

    return suggestions;
  }
}
