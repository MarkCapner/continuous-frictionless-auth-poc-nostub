package com.poc.api.persistence;

import java.time.OffsetDateTime;

public class DecisionLogRow {
  public Long id;
  public String sessionId;
  public String userId;
  public String tlsFp;
  public double behaviorScore;
  public double deviceScore;
  public double contextScore;
  public double confidence;
  public String decision;
  public OffsetDateTime createdAt;
}
