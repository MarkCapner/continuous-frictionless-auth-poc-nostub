package com.poc.api.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.admin.dto.PolicyRule;
import com.poc.api.admin.dto.PolicyScope;
import com.poc.api.admin.persistence.PolicyRuleRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

  public PolicyRule create(PolicyRule rule) {
    validatePolicy(rule);
    long id = repo.insert(rule);
    return repo.findById(id).orElse(rule);
  }

  public PolicyRule update(long id, PolicyRule rule) {
    validatePolicy(rule);
    repo.update(id, rule);
    return repo.findById(id).orElse(rule);
  }

  public void setEnabled(long id, boolean enabled) {
    repo.setEnabled(id, enabled);
  }

  public void delete(long id) {
    repo.delete(id);
  }

  /**
   * EPIC 13.6: Resolve policies in precedence order USER -> TENANT -> GLOBAL.
   *
   * Some repository variants do not implement a dedicated resolveEffectivePolicies query.
   * To remain compatible, we resolve in-memory using repo.listAll().
   */
  public List<PolicyRule> resolveEffectivePolicies(String tenantId, String userId) {
    List<PolicyRule> all = repo.listAll();

    List<PolicyRule> user = all.stream()
        .filter(PolicyRule::isEnabled)
        .filter(r -> r.getScope() == PolicyScope.USER)
        .filter(r -> userId != null && userId.equals(r.getScopeRef()))
        .sorted(Comparator.comparingInt(PolicyRule::getPriority).reversed())
        .collect(Collectors.toList());

    List<PolicyRule> tenant = all.stream()
        .filter(PolicyRule::isEnabled)
        .filter(r -> r.getScope() == PolicyScope.TENANT)
        .filter(r -> tenantId != null && tenantId.equals(r.getScopeRef()))
        .sorted(Comparator.comparingInt(PolicyRule::getPriority).reversed())
        .collect(Collectors.toList());

    List<PolicyRule> global = all.stream()
        .filter(PolicyRule::isEnabled)
        .filter(r -> r.getScope() == PolicyScope.GLOBAL)
        .sorted(Comparator.comparingInt(PolicyRule::getPriority).reversed())
        .collect(Collectors.toList());

    user.addAll(tenant);
    user.addAll(global);
    return user;
  }

  // ---------- EPIC 13.7 Guardrails: validation on create/update ----------

  private void validatePolicy(PolicyRule rule) {
    JsonNode act = parseJson(rule.getActionJson());

    if (act != null && act.isObject()) {
      JsonNode decision = act.get("decision");
      JsonNode reason = act.get("reason");
      JsonNode cap = act.get("confidence_cap");

      if (decision != null && !decision.isNull()) {
        String d = decision.asText("").toUpperCase();
        if (!d.equals("ALLOW") && !d.equals("STEP_UP") && !d.equals("BLOCK")) {
          throw new IllegalArgumentException("action.decision must be one of ALLOW, STEP_UP, BLOCK");
        }
        if (reason == null || reason.isNull() || reason.asText("").isBlank()) {
          throw new IllegalArgumentException("action.reason is required when action.decision is present");
        }
      }

      if (cap != null && !cap.isNull()) {
        double v = cap.asDouble(Double.NaN);
        if (!(v > 0.0 && v <= 1.0)) {
          throw new IllegalArgumentException("action.confidence_cap must be in (0,1]");
        }
      }
    }
  }

  private JsonNode parseJson(Object json) {
    if (json == null) return null;
    try {
      if (json instanceof String s) {
        if (s.isBlank()) return null;
        return mapper.readTree(s);
      }
      return mapper.valueToTree(json);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON in policy action: " + e.getMessage(), e);
    }
  }
}
