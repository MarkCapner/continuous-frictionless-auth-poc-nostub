package com.poc.api.model;

public class TrustSignal {
    public String category;   // e.g. DEVICE, BEHAVIOUR, TLS, CONTEXT
    public String label;      // short user-facing label
    public String status;     // OK, WARN, RISK
    public String explanation; // user-safe plain language
}
