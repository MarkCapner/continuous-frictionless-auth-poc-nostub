package com.poc.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.model.PolicyRule;
import com.poc.api.model.PolicyScope;
import com.poc.api.persistence.PolicyRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * EPIC 13 — Policy Engine & Controls
 *  - 13.1 storage + scope precedence
 *  - 13.7 guardrails validation hooks (create/update time)
 */
@Service
public class PolicyRuleService {

    private final PolicyRuleRepository repo;
    private final ObjectMapper mapper;

    public PolicyRuleService(PolicyRuleRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<PolicyRule> listAll() {
        return repo.listAll();
    }

    public Optional<PolicyRule> get(long id) {
        return repo.get(id);
    }

    public long create(PolicyRule rule) {
        normalise(rule);
        validate(rule);
        return repo.create(rule);
    }

    public void update(long id, PolicyRule rule) {
        rule.setId(id);
        normalise(rule);
        validate(rule);
        repo.update(rule);
    }

    public void setEnabled(long id, boolean enabled) {
        repo.setEnabled(id, enabled);
    }

    public void delete(long id) {
        repo.delete(id);
    }

    /**
     * Returns enabled policies in precedence order:
     * USER -> TENANT -> GLOBAL (each ordered by priority desc).
     *
     * Tenant is optional in the PoC; pass null/blank to skip.
     */
    public List<PolicyRule> resolveEffectivePolicies(String tenantId, String userId) {
        List<PolicyRule> out = new ArrayList<>();
        if (StringUtils.hasText(userId)) {
            out.addAll(repo.listByScope(PolicyScope.USER, userId, true));
        }
        if (StringUtils.hasText(tenantId)) {
            out.addAll(repo.listByScope(PolicyScope.TENANT, tenantId, true));
        }
        out.addAll(repo.listByScope(PolicyScope.GLOBAL, null, true));
        return out;
    }

    private void normalise(PolicyRule rule) {
        // Ensure GLOBAL doesn't accidentally carry a scopeRef.
        if (rule.getScope() == PolicyScope.GLOBAL) {
            rule.setScopeRef(null);
        }
        // Ensure scope is uppercase if it came from loose JSON binding.
        if (rule.getScope() != null) {
            rule.setScope(PolicyScope.valueOf(rule.getScope().name().toUpperCase(Locale.ROOT)));
        }
    }

    /**
     * EPIC 13.7 — Guardrails:
     *  - conditionJson/actionJson must be valid JSON
     *  - action.decision must be ALLOW/STEP_UP/BLOCK if present
     *  - action.reason required when decision override present
     *  - action.confidence_cap must be (0,1] if present
     */
    private void validate(PolicyRule rule) {
        if (rule.getScope() == null) {
            throw new IllegalArgumentException("scope is required");
        }
        if (rule.getScope() != PolicyScope.GLOBAL && !StringUtils.hasText(rule.getScopeRef())) {
            throw new IllegalArgumentException("scopeRef is required for scope " + rule.getScope());
        }
        if (!StringUtils.hasText(rule.getConditionJson())) {
            throw new IllegalArgumentException("conditionJson is required");
        }
        if (!StringUtils.hasText(rule.getActionJson())) {
            throw new IllegalArgumentException("actionJson is required");
        }

        JsonNode cond;
        JsonNode action;
        try {
            cond = mapper.readTree(rule.getConditionJson());
        } catch (Exception e) {
            throw new IllegalArgumentException("conditionJson must be valid JSON");
        }
        try {
            action = mapper.readTree(rule.getActionJson());
        } catch (Exception e) {
            throw new IllegalArgumentException("actionJson must be valid JSON");
        }

        // Minimal shape check: condition/action should be objects
        if (!cond.isObject()) {
            throw new IllegalArgumentException("conditionJson must be a JSON object");
        }
        if (!action.isObject()) {
            throw new IllegalArgumentException("actionJson must be a JSON object");
        }

        // action.decision guard
        JsonNode decisionNode = action.get("decision");
        if (decisionNode != null && !decisionNode.isNull()) {
            String d = decisionNode.asText("").trim().toUpperCase(Locale.ROOT);
            if (!Set.of("ALLOW", "STEP_UP", "BLOCK").contains(d)) {
                throw new IllegalArgumentException("action.decision must be one of ALLOW, STEP_UP, BLOCK");
            }
            JsonNode reasonNode = action.get("reason");
            if (reasonNode == null || reasonNode.asText("").isBlank()) {
                throw new IllegalArgumentException("action.reason is required when action.decision is present");
            }
        }

        // action.confidence_cap guard
        JsonNode capNode = action.get("confidence_cap");
        if (capNode != null && !capNode.isNull()) {
            double v;
            if (capNode.isNumber()) {
                v = capNode.doubleValue();
            } else {
                try {
                    v = Double.parseDouble(capNode.asText());
                } catch (Exception e) {
                    throw new IllegalArgumentException("action.confidence_cap must be a number");
                }
            }
            if (!(v > 0.0 && v <= 1.0)) {
                throw new IllegalArgumentException("action.confidence_cap must be in (0,1]");
            }
        }
    }
}
