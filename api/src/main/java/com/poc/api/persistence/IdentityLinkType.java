package com.poc.api.persistence;

/**
 * EPIC 10.1
 * Link types are intentionally broad; EPIC 10.2 will populate these.
 */
public enum IdentityLinkType {
  USER_DEVICE,
  DEVICE_TLS_FAMILY,
  USER_TLS_FAMILY,
  USER_BEHAVIOR_CLUSTER,
  DEVICE_BEHAVIOR_CLUSTER,
  TLS_FAMILY_BEHAVIOR_CLUSTER
}
