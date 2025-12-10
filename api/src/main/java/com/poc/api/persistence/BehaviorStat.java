package com.poc.api.persistence;

import java.time.OffsetDateTime;

public class BehaviorStat {
  public Long id;
  public String userId;
  public String feature;
  public double mean;
  public double variance;
  public double decay;
  public OffsetDateTime updatedAt;
}
