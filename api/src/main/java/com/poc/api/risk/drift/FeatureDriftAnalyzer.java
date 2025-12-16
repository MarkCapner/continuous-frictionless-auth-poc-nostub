package com.poc.api.risk.drift;

import java.util.ArrayList;
import java.util.List;

/**
 * EPIC X.11
 *
 * Lightweight, explainability-first feature drift analyzer.
 * Designed for UI explanation, not statistical enforcement.
 */
public class FeatureDriftAnalyzer {

  public List<FeatureDriftResult> analyze(List<FeatureObservation> history) {
    List<FeatureDriftResult> results = new ArrayList<>();
    for (FeatureObservation o : history) {
      double driftScore = Math.abs(o.currentValue() - o.baselineValue());
      DriftLevel level =
          driftScore < 0.2 ? DriftLevel.STABLE :
          driftScore < 0.5 ? DriftLevel.MODERATE :
                             DriftLevel.HIGH;

      results.add(new FeatureDriftResult(
          o.featureKey(),
          driftScore,
          level,
          level.explanation()
      ));
    }
    return results;
  }
}
