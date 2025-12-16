package com.poc.api.admin.persistence;

import java.time.OffsetDateTime;

public class PolicyMatchRow {
  public String requestId;
  public String userId;
  public String decision;
  public double confidence;
  public String policyJson;
  public OffsetDateTime occurredAt;
}
