package com.poc.api.persistence;

import java.time.OffsetDateTime;

public class DeviceProfile {
  public Long id;
  public String userId;
  public String tlsFp;
  public String uaFamily;
  public String uaVersion;
  public int screenW;
  public int screenH;
  public double pixelRatio;
  public short tzOffset;
  public String canvasHash;
  public String webglHash;
  public OffsetDateTime firstSeen;
  public OffsetDateTime lastSeen;
  public long seenCount;
  public String lastCountry;
}
