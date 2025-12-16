package com.poc.api.risk.api;

/**
 * Minimal read-only projection for explainability exposure.
 * Implemented by persistence models when ready.
 */
public interface DecisionExplainable {

  String getDecision();

  double getConfidence();

  String getFeatureContributionsJson();

  String getTopPositiveContributorsJson();

  String getTopNegativeContributorsJson();
}
