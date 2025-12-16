package com.poc.api.telemetry.persistence;

import java.time.OffsetDateTime;

public class TlsFingerprintStatsRow {
  public String tlsFp;
  public long profiles;
  public long users;
  public OffsetDateTime firstSeen;
  public OffsetDateTime lastSeen;
}
