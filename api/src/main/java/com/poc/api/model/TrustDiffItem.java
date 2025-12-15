package com.poc.api.model;

/**
 * A user-safe, plain-language description of what changed between two sessions.
 *
 * This intentionally avoids raw telemetry values, hashes, fingerprints, or identifiers.
 */
public class TrustDiffItem {
    /** DEVICE / BEHAVIOUR / TLS / CONTEXT */
    public String dimension;

    /** Plain language description of the change. */
    public String change;

    /** LOW / MEDIUM / HIGH */
    public String severity;
}
