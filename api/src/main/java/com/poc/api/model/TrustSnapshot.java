package com.poc.api.model;

import java.time.OffsetDateTime;
import java.util.List;

public class TrustSnapshot {
    public String sessionId;
    public String userId;

    public String decision;      // ALLOW / STEP_UP / BLOCK (or whatever rules engine produced)
    public double confidence;

    public String riskSummary;   // one-sentence plain language summary
    public List<TrustSignal> signals;

    /**
     * Whether the user has granted consent for trust signals (PoC hook).
     * When false, the system should minimise/omit sensitive signals where possible.
     */
    public Boolean consentGranted;

    /**
     * When set, indicates a point-in-time after which "baseline" comparisons should be made.
     * (i.e., user requested a trust/baseline reset).
     */
    public OffsetDateTime baselineResetAt;

    /**
     * User-safe, plain-language description of what changed since the last trusted session.
     * Empty when there is no suitable baseline.
     */
    public List<TrustDiffItem> changes;

    /**
     * The baseline session used for the diff (typically the last trusted session).
     * Null when there is no suitable baseline.
     */
    public String baselineSessionId;
}
