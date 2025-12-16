package com.poc.api.risk.policy;

import java.util.Map;

/**
 * Parsed policy action.
 *
 * Supported JSON fields:
 * - decision: "ALLOW" | "STEP_UP" | "BLOCK"
 * - confidence_cap: number in [0,1] to cap confidence (only lowers)
 * - reason: plain language string added to decision explanations
 */
public record PolicyAction(String decisionOverride, Double confidenceCap, String reason) {

    public static PolicyAction from(Map<String, Object> action) {
        if (action == null || action.isEmpty()) return null;

        String decision = null;
        Double cap = null;
        String reason = null;

        Object d = action.get("decision");
        if (d instanceof String s && !s.isBlank()) decision = s.trim();

        Object c = action.get("confidence_cap");
        if (c instanceof Number n) cap = n.doubleValue();
        else if (c instanceof String s) {
            try { cap = Double.parseDouble(s); } catch (Exception ignored) {}
        }

        Object r = action.get("reason");
        if (r instanceof String s && !s.isBlank()) reason = s.trim();

        if (decision == null && cap == null && reason == null) return null;
        return new PolicyAction(decision, cap, reason);
    }
}
