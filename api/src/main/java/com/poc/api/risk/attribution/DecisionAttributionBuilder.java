package com.poc.api.risk.attribution;

import java.util.ArrayList;
import java.util.List;

import com.poc.api.risk.model.RiskSignal;

/**
 * EPIC X.4
 *
 * Converts existing computed scores into canonical RiskSignal objects.
 * This class performs NO aggregation and NO decision logic.
 *
 * It is intentionally explicit and verbose to make signal provenance clear.
 */
public class DecisionAttributionBuilder {

  private final List<RiskSignal> signals = new ArrayList<>();

  public static DecisionAttributionBuilder create() {
    return new DecisionAttributionBuilder();
  }

  public DecisionAttributionBuilder addBehaviorScore(double value, double weight) {
    signals.add(new RiskSignal(
        "behavior.score",
        value,
        weight,
        "behavior"
    ));
    return this;
  }

  public DecisionAttributionBuilder addDeviceScore(double value, double weight) {
    signals.add(new RiskSignal(
        "device.score",
        value,
        weight,
        "device"
    ));
    return this;
  }

  public DecisionAttributionBuilder addContextScore(double value, double weight) {
    signals.add(new RiskSignal(
        "context.score",
        value,
        weight,
        "context"
    ));
    return this;
  }

  public DecisionAttributionBuilder addTlsScore(double value, double weight) {
    signals.add(new RiskSignal(
        "tls.score",
        value,
        weight,
        "tls"
    ));
    return this;
  }

  public DecisionAttributionBuilder addMlScore(String modelName, double value, double weight) {
    signals.add(new RiskSignal(
        "ml." + modelName,
        value,
        weight,
        "ml"
    ));
    return this;
  }

  public List<RiskSignal> build() {
    return List.copyOf(signals);
  }
}
