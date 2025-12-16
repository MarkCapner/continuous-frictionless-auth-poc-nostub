package com.poc.api.risk.policy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PolicyOutcome {

    private final boolean matched;
    private final Long policyId;
    private final String description;
    private final PolicyAction action;

    private PolicyOutcome(boolean matched, Long policyId, String description, PolicyAction action) {
        this.matched = matched;
        this.policyId = policyId;
        this.description = description;
        this.action = action;
    }

    public static PolicyOutcome noMatch() {
        return new PolicyOutcome(false, null, null, null);
    }

    public static PolicyOutcome matched(long id, String description, PolicyAction action) {
        return new PolicyOutcome(true, id, description, action);
    }

    public boolean matched() { return matched; }
    public Long policyId() { return policyId; }
    public String description() { return description; }
    public PolicyAction action() { return action; }

    public Map<String, Object> asExplainMap() {
        if (!matched) return Collections.emptyMap();
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("matched", true);
        m.put("policy_id", policyId);
        m.put("description", description);
        if (action != null) {
            if (action.decisionOverride() != null) m.put("decision_override", action.decisionOverride());
            if (action.confidenceCap() != null) m.put("confidence_cap", action.confidenceCap());
            if (action.reason() != null) m.put("reason", action.reason());
        }
        return m;
    }
}
