package com.poc.api.service.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.model.PolicyRule;
import com.poc.api.service.PolicyRuleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EPIC 13.2: Evaluates stored policy rules to optionally override/adjust the final decision.
 *
 * - Deterministic, rule-based evaluation (no ML).
 * - Policies are resolved by scope precedence: USER -> TENANT -> GLOBAL (already handled by PolicyRuleService).
 * - First matching policy wins (in the resolved order).
 */
@Service
public class PolicyEngine {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final PolicyRuleService policyRuleService;
    private final ObjectMapper objectMapper;

    // EPIC 13.7 guardrails
    private static final double BLOCK_MIN_RISK = 0.85;
    private static final double ALLOW_MAX_RISK = 0.55;

    public PolicyEngine(PolicyRuleService policyRuleService, ObjectMapper objectMapper) {
        this.policyRuleService = policyRuleService;
        this.objectMapper = objectMapper;
    }

    public PolicyOutcome evaluate(String tenantId, String userId, PolicyContext ctx) {
        List<PolicyRule> policies = policyRuleService.resolveEffectivePolicies(
                StringUtils.hasText(tenantId) ? tenantId : null,
                StringUtils.hasText(userId) ? userId : null
        );
        if (policies == null || policies.isEmpty()) {
            return PolicyOutcome.noMatch();
        }

        for (PolicyRule rule : policies) {
            if (rule == null || !rule.isEnabled()) continue;

            Map<String, Object> cond = safeParse(rule.getConditionJson());
            if (!PolicyMatcher.matches(cond, ctx.values())) continue;

            Map<String, Object> action = safeParse(rule.getActionJson());
            PolicyAction parsed = PolicyAction.from(action);
            if (parsed == null) continue;

            PolicyAction guarded = applyGuardrails(parsed, ctx.values());
            return PolicyOutcome.matched(rule.getId(), rule.getDescription(), guarded);
}

        return PolicyOutcome.noMatch();
    }

// EPIC 13.7 — runtime guardrails. Return a possibly "suppressed" action (no override), with a reason explaining why.
private PolicyAction applyGuardrails(PolicyAction action, Map<String, Object> ctx) {
    if (action == null) return null;

    // Validate confidence cap range if provided
    Double cap = action.confidenceCap();
    if (cap != null) {
        if (!(cap > 0.0 && cap <= 1.0)) {
            return new PolicyAction(null, null, "Policy suppressed: confidence_cap must be in (0,1].");
        }
    }

    // Require reason when overriding decision
    if (action.decisionOverride() != null && (action.reason() == null || action.reason().isBlank())) {
        return new PolicyAction(null, null, "Policy suppressed: override requires a non-empty reason.");
    }

    Double risk = asDouble(ctx.get("risk.score"));
    Double anomaly = asDouble(ctx.get("behaviour.anomaly"));

    // Guardrail: BLOCK only allowed when pre-policy risk already high
    if ("BLOCK".equalsIgnoreCase(action.decisionOverride())) {
        if (risk == null || risk < 0.85) {
            return new PolicyAction(null, null, "Policy suppressed: BLOCK requires pre-policy risk ≥ 0.85.");
        }
    }

    // Guardrail: ALLOW cannot override high-risk or anomalous sessions
    if ("ALLOW".equalsIgnoreCase(action.decisionOverride())) {
        boolean anomalous = anomaly != null && anomaly > 0.75;
        if ((risk != null && risk > 0.55) || anomalous) {
            return new PolicyAction(null, null, "Policy suppressed: ALLOW not permitted for high-risk/anomalous sessions.");
        }
    }

    return action;
}

private static Double asDouble(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.doubleValue();
    try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return null; }
}

    private Map<String, Object> safeParse(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, MAP);
        } catch (Exception e) {
            // Invalid JSON should not break runtime scoring in PoC; treat as non-match.
            return Collections.emptyMap();
        }
    }

    /**
     * Helper for creating a context map from known signals.
     */
    public static PolicyContext context(Map<String, Object> values) {
        return new PolicyContext(values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values));
    }
}