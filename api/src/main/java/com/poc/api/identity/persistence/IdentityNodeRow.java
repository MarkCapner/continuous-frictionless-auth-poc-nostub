package com.poc.api.identity.persistence;

import java.time.OffsetDateTime;

/**
 * EPIC 10.1
 * JDBC projection for identity_node.
 */
public class IdentityNodeRow {
  public long id;
  public IdentityNodeType nodeType;
  /** Natural key within nodeType (e.g., user_id, device_profile.id as string, etc.) */
  public String naturalKey;
  public String displayLabel;
  public String metaJson;
  public OffsetDateTime createdAt;
  public OffsetDateTime lastSeen;
}
