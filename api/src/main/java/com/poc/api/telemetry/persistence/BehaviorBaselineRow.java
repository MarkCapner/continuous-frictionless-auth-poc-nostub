package com.poc.api.telemetry.persistence;

import java.time.OffsetDateTime;

public class BehaviorBaselineRow {
  public String userId;
  public String feature;
  public double mean;
  public double stdDev;
  public double variance;
  public double decay;
  public OffsetDateTime updatedAt;
}
