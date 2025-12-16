package com.poc.api.risk.persistence;

/**
 * Minimal contract for any decision persistence model.
 * Allows EPIC X.6 without coupling to a specific entity.
 */
public interface DecisionWritable {

  void setDecision(String decision);

  void setConfidence(double confidence);

  void setFeatureContributionsJson(String json);

  void setTopPositiveContributorsJson(String json);

  void setTopNegativeContributorsJson(String json);
}
