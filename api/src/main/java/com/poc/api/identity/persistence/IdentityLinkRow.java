package com.poc.api.identity.persistence;

import java.time.OffsetDateTime;

/**
 * EPIC 10.1
 * JDBC projection for identity_link.
 */
public class IdentityLinkRow {
  public long id;
  public long fromNodeId;
  public long toNodeId;
  public IdentityLinkType linkType;
  public double confidence;
  public String reason;
  public String evidenceJson;
  public OffsetDateTime firstSeen;
  public OffsetDateTime lastSeen;
}
