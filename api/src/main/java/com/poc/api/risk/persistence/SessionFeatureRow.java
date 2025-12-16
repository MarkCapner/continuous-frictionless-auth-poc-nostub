package com.poc.api.risk.persistence;

import java.time.OffsetDateTime;

public class SessionFeatureRow {
  public Long id;
  public OffsetDateTime occurredAt;
  public String userId;
  public String requestId;
  public String tlsFp;
  public String deviceJson;
  public String behaviorJson;
  public String contextJson;
  public String featureVector;
  public String decision;
  public double confidence;
  public String label;
}
