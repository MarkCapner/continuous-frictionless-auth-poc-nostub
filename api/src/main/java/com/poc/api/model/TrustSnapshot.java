package com.poc.api.model;

import java.util.List;

public class TrustSnapshot {
    public String sessionId;
    public String decision;      // ALLOW / STEP_UP / BLOCK (or whatever rules engine produced)
    public double confidence;
    public String riskSummary;   // one-sentence plain language summary
    public List<TrustSignal> signals;
}
