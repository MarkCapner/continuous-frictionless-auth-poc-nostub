package com.poc.api.risk.drift;

public enum DriftLevel {
  STABLE("Feature consistent with historical baseline"),
  MODERATE("Feature shows noticeable variation"),
  HIGH("Feature deviates significantly from baseline");

  private final String explanation;

  DriftLevel(String explanation) {
    this.explanation = explanation;
  }

  public String explanation() {
    return explanation;
  }
}
