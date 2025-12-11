package com.poc.api.persistence;

import java.time.OffsetDateTime;

public class TlsFingerprintDeviceRow {
  public String userId;
  public String uaFamily;
  public String uaVersion;
  public int screenW;
  public int screenH;
  public double pixelRatio;
  public String lastCountry;
  public long seenCount;
  public OffsetDateTime firstSeen;
  public OffsetDateTime lastSeen;
}
