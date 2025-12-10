package com.poc.api.persistence;

import java.time.OffsetDateTime;

public class UserSummaryRow {
  public String userId;
  public long sessions;
  public long devices;
  public OffsetDateTime lastSeen;
  public double avgConfidence;
}
