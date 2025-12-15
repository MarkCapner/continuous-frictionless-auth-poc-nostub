package com.poc.api.controller;

import com.poc.api.ml.ModelProvider;
import com.poc.api.persistence.ModelCanaryPolicyRepository;
import com.poc.api.persistence.ModelChangeEventRepository;
import com.poc.api.persistence.ModelRegistryRepository;
import com.poc.api.persistence.ModelScorecardRepository;
import com.poc.api.persistence.RetrainJobRepository;
import com.poc.api.service.MlTrainingService;
import com.poc.api.service.ModelRolloutService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/model")
public class AdminModelController {

    private final ModelProvider modelProvider;
    private final ModelRegistryRepository registry;
    private final RetrainJobRepository retrainJobs;
    private final ModelScorecardRepository scorecards;
    private final ModelCanaryPolicyRepository canary;
    private final ModelChangeEventRepository changes;
    private final MlTrainingService training;
    private final ModelRolloutService rollout;

    public AdminModelController(ModelProvider modelProvider,
                                ModelRegistryRepository registry,
                                RetrainJobRepository retrainJobs,
                                ModelScorecardRepository scorecards,
                                ModelCanaryPolicyRepository canary,
                                ModelChangeEventRepository changes,
                                MlTrainingService training,
                                ModelRolloutService rollout) {
        this.modelProvider = modelProvider;
        this.registry = registry;
        this.retrainJobs = retrainJobs;
        this.scorecards = scorecards;
        this.canary = canary;
        this.changes = changes;
        this.training = training;
        this.rollout = rollout;
    }

    /**
     * UI expects: registryId/Name/Format/Version/LastTrainedAt/now.
     */
    @GetMapping("/info")
    public Map<String, Object> getModelInfo() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("now", Instant.now().toString());

        registry.findActive().ifPresentOrElse(rec -> {
            out.put("registryId", rec.id());
            out.put("registryName", rec.name());
            out.put("registryFormat", rec.format());
            out.put("registryVersion", rec.version());
            out.put("lastTrainedAt", rec.createdAt() != null ? rec.createdAt().toString() : null);
        }, () -> {
            out.put("registryId", null);
            out.put("registryName", null);
            out.put("registryFormat", null);
            out.put("registryVersion", null);
            out.put("lastTrainedAt", null);
        });

        // Extra fields (safe for UI to ignore)
        out.put("modelVersion", safe(modelProvider.getModelVersion(), "rules-only"));
        return out;
    }

    @GetMapping("/jobs")
    public List<RetrainJobRepository.RetrainJobRow> listJobs(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        return retrainJobs.list(limit);
    }

    @GetMapping("/scorecards")
    public List<ModelScorecardRepository.ScorecardRow> listScorecards(
            @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
            @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return scorecards.list(scopeType, scopeKey, limit);
    }

    @GetMapping("/scorecards/{id}")
    public ModelScorecardRepository.ScorecardRow getScorecard(@PathVariable("id") long id) {
        return scorecards.get(id).orElseThrow();
    }

    @GetMapping("/registry")
    public List<ModelRegistryRepository.ModelRecord> listRegistry(
            @RequestParam(name = "kind", defaultValue = "risk-model") String kind,
            @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
            @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return registry.listRecent(kind, scopeType, scopeKey, limit);
    }

    @GetMapping("/canary")
    public Map<String, Object> getCanary(
            @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
            @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey) {
        return canary.get("risk-model", scopeType, scopeKey)
                .<Map<String, Object>>map(c -> Map.of(
                        "id", c.id(),
                        "createdAt", c.createdAt(),
                        "updatedAt", c.updatedAt(),
                        "kind", c.kind(),
                        "scopeType", c.scopeType(),
                        "scopeKey", c.scopeKey(),
                        "modelId", c.modelId(),
                        "rolloutPercent", c.rolloutPercent(),
                        "enabled", c.enabled()
                ))
                .orElse(null);
    }

    @GetMapping("/changes")
    public List<ModelChangeEventRepository.ChangeEvent> listChanges(
            @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
            @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey,
            @RequestParam(name = "limit", defaultValue = "100") int limit) {
        return changes.list("risk-model", scopeType, scopeKey, limit);
    }

    /** Enqueue async retrain (runner picks up from retrain_job). */
    @PostMapping("/retrain")
    public Map<String, Object> enqueueRetrain(
            @RequestParam(name = "reason", defaultValue = "manual") String reason,
            @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
            @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey) {
        Long fromId = registry.findActive().map(ModelRegistryRepository.ModelRecord::id).orElse(null);
        long jobId = retrainJobs.enqueue("risk-model", scopeType, scopeKey, reason, fromId);
        return Map.of("jobId", jobId);
    }

    /** Synchronous retrain now (used for demos). */
    @PostMapping("/retrainNow")
    public Map<String, Object> retrainNow(@RequestParam(name = "limit", defaultValue = "500") int limit) {
        var result = training.retrainFromRecentWithResult(limit);
        return Map.of(
                "modelId", result.modelId(),
                "version", result.version(),
                "metrics", result.metrics()
        );
    }

    @PostMapping("/activate")
    public Map<String, Object> activate(@RequestParam(name = "modelId") long modelId,
                                        @RequestParam(name = "actor", defaultValue = "ui") String actor,
                                        @RequestParam(name = "reason", defaultValue = "manual") String reason) {
        rollout.activate(actor, modelId, reason);
        return Map.of("ok", true, "activeModelId", modelId);
    }

    @PostMapping("/rollback")
    public Map<String, Object> rollback(@RequestParam(name = "actor", defaultValue = "ui") String actor,
                                        @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
                                        @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey,
                                        @RequestParam(name = "toModelId", required = false) Long toModelId,
                                        @RequestParam(name = "reason", defaultValue = "manual") String reason,
                                        @RequestParam(name = "evidenceJson", required = false) String evidenceJson) {
        rollout.rollback(actor, scopeType, scopeKey, toModelId, reason, evidenceJson);
        return Map.of("ok", true);
    }

    @PostMapping("/canary/start")
    public Map<String, Object> startCanary(@RequestParam(name = "modelId") long modelId,
                                           @RequestParam(name = "percent", defaultValue = "5") int percent,
                                           @RequestParam(name = "actor", defaultValue = "ui") String actor,
                                           @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
                                           @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey,
                                           @RequestParam(name = "reason", defaultValue = "manual") String reason) {
        rollout.startCanary(actor, scopeType, scopeKey, modelId, percent, reason);
        return Map.of("ok", true);
    }

    @PostMapping("/canary/step")
    public Map<String, Object> stepCanary(@RequestParam(name = "percent") int percent,
                                          @RequestParam(name = "actor", defaultValue = "ui") String actor,
                                          @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
                                          @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey,
                                          @RequestParam(name = "reason", defaultValue = "manual") String reason) {
        rollout.stepCanary(actor, scopeType, scopeKey, percent, reason);
        return Map.of("ok", true);
    }

    @PostMapping("/canary/stop")
    public Map<String, Object> stopCanary(@RequestParam(name = "actor", defaultValue = "ui") String actor,
                                          @RequestParam(name = "scopeType", defaultValue = "GLOBAL") String scopeType,
                                          @RequestParam(name = "scopeKey", defaultValue = "*") String scopeKey,
                                          @RequestParam(name = "reason", defaultValue = "manual") String reason) {
        rollout.stopCanary(actor, scopeType, scopeKey, reason);
        return Map.of("ok", true);
    }

    private static String safe(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
