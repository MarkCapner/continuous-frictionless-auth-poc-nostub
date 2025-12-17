
package com.poc.api.risk.policy;

import java.util.*;

public class RiskPolicyEngine {

    public PolicyDecision evaluate(double totalDrift, String trend) {
        if (totalDrift > 0.8) {
            return new PolicyDecision("STEP_UP_AUTH", "High drift detected");
        }
        if ("WORSENING".equals(trend) && totalDrift > 0.5) {
            return new PolicyDecision("MONITOR", "Drift worsening over time");
        }
        return new PolicyDecision("ALLOW", "Risk within acceptable range");
    }

    public record PolicyDecision(String action, String reason) {}
}
