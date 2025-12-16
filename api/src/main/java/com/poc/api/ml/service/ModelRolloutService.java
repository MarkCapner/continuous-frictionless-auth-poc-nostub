package com.poc.api.ml.service;

import com.poc.api.ml.persistence.ModelCanaryPolicyRepository;
import com.poc.api.ml.persistence.ModelChangeEventRepository;
import com.poc.api.ml.persistence.ModelRegistryRepository;
import org.springframework.stereotype.Service;

@Service
public class ModelRolloutService {

  private final ModelRegistryRepository registry;
  private final ModelCanaryPolicyRepository canary;
  private final ModelChangeEventRepository changes;

  public ModelRolloutService(ModelRegistryRepository registry,
                             ModelCanaryPolicyRepository canary,
                             ModelChangeEventRepository changes) {
    this.registry = registry;
    this.canary = canary;
    this.changes = changes;
  }

  public void activate(String actor, long modelId, String reason) {
    Long fromId = registry.findActive().map(ModelRegistryRepository.ModelRecord::id).orElse(null);
    registry.activateById(modelId);
    changes.insert(new ModelChangeEventRepository.ChangeEvent(
        0, null, actor, "ACTIVATE", "risk-model", "GLOBAL", "*",
        fromId, modelId, reason, "{}"
    ));
  }

  public void startCanary(String actor, String scopeType, String scopeKey, long modelId, int percent, String reason) {
    canary.upsert("risk-model", scopeType, scopeKey, modelId, clamp(percent), true);
    changes.insert(new ModelChangeEventRepository.ChangeEvent(
        0, null, actor, "CANARY_START", "risk-model", scopeType, scopeKey,
        null, modelId, reason, "{}"
    ));
  }

  public void stepCanary(String actor, String scopeType, String scopeKey, int percent, String reason) {
    var c = canary.get("risk-model", scopeType, scopeKey).orElseThrow();
    canary.upsert("risk-model", scopeType, scopeKey, c.modelId(), clamp(percent), true);
    changes.insert(new ModelChangeEventRepository.ChangeEvent(
        0, null, actor, "CANARY_STEP", "risk-model", scopeType, scopeKey,
        null, c.modelId(), reason, "{}"
    ));
  }

  public void stopCanary(String actor, String scopeType, String scopeKey, String reason) {
    var c = canary.get("risk-model", scopeType, scopeKey).orElse(null);
    Long from = c == null ? null : c.modelId();
    canary.disable("risk-model", scopeType, scopeKey);
    changes.insert(new ModelChangeEventRepository.ChangeEvent(
        0, null, actor, "CANARY_STOP", "risk-model", scopeType, scopeKey,
        from, null, reason, "{}"
    ));
  }

  public void rollback(String actor, String scopeType, String scopeKey, Long toModelId, String reason, String evidenceJson) {
    Long current = registry.findActive().map(ModelRegistryRepository.ModelRecord::id).orElse(null);
    Long target = toModelId;

    if (target == null) {
      var last = changes.lastActivateOrRollback("risk-model", scopeType, scopeKey).orElse(null);
      if (last != null && last.fromModelId() != null) target = last.fromModelId();
    }
    if (target == null) return;

    registry.activateById(target);
    changes.insert(new ModelChangeEventRepository.ChangeEvent(
        0, null, actor, "ROLLBACK", "risk-model", scopeType, scopeKey,
        current, target, reason, evidenceJson == null ? "{}" : evidenceJson
    ));
  }

  private int clamp(int p) { return Math.max(0, Math.min(100, p)); }
}
