package com.poc.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.model.PolicyRule;
import com.poc.api.model.PolicyScope;
import com.poc.api.persistence.PolicyRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public Optional<PolicyRule> findById(long id) {
        return repo.findById(id);
    }

    public long create(PolicyRule rule) {
        validate(rule);
        normalise(rule);
        return repo.insert(rule);
    }

    public void update(long id, PolicyRule rule) {
        validate(rule);
        normalise(rule);
        repo.update(id, rule);
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
    }

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

        // Validate JSON (cheap & deterministic)
        try {
            mapper.readTree(rule.getConditionJson());
        } catch (Exception e) {
            throw new IllegalArgumentException("conditionJson must be valid JSON");
        }
        try {
            mapper.readTree(rule.getActionJson());
        } catch (Exception e) {
            throw new IllegalArgumentException("actionJson must be valid JSON");
        }
    }
}
