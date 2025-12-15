package com.poc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.model.PolicyRule;
import com.poc.api.model.PolicyMatchEvent;
import com.poc.api.model.PolicySimulationRequest;
import com.poc.api.model.PolicySimulationResult;
import com.poc.api.persistence.SessionFeatureRepository;
import com.poc.api.service.PolicyRuleService;
import com.poc.api.service.policy.PolicyEngine;
import com.poc.api.service.policy.PolicyOutcome;
import com.poc.api.service.policy.PolicyContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * EPIC 13.1–13.5 Admin policy APIs (CRUD + effective resolution + recent matches + simulation).
 */
@RestController
@RequestMapping("/api/admin/policy")
public class AdminPolicyController {

    private final PolicyRuleService service;
    private final SessionFeatureRepository sessionFeatureRepository;
    private final ObjectMapper objectMapper;
    private final PolicyEngine policyEngine;

    public AdminPolicyController(PolicyRuleService service,
                                SessionFeatureRepository sessionFeatureRepository,
                                ObjectMapper objectMapper,
                                PolicyEngine policyEngine) {
        this.service = service;
        this.sessionFeatureRepository = sessionFeatureRepository;
        this.objectMapper = objectMapper;
        this.policyEngine = policyEngine;
    }

    @GetMapping
    public List<PolicyRule> listAll() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyRule> get(@PathVariable("id") long id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PolicyRule body) {
        try {
            // validate JSON fields are parseable maps (or empty)
            parseMap(body.getConditionJson());
            parseMap(body.getActionJson());
            long id = service.create(body);
            return ResponseEntity.ok(Map.of("id", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") long id, @RequestBody PolicyRule body) {
        try {
            parseMap(body.getConditionJson());
            parseMap(body.getActionJson());
            service.update(id, body);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<?> enabled(@PathVariable("id") long id,
                                    @RequestParam("enabled") boolean enabled) {
        service.setEnabled(id, enabled);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/effective")
    public List<PolicyRule> effective(
            @RequestParam(value = "tenant_id", required = false) String tenantId,
            @RequestParam(value = "user_id", required = false) String userId
    ) {
        return service.resolveEffectivePolicies(tenantId, userId);
    }

    @GetMapping("/matches")
    public List<PolicyMatchEvent> matches(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        int capped = Math.max(1, Math.min(limit, 200));
        var rows = sessionFeatureRepository.findRecentPolicyMatches(capped);
        List<PolicyMatchEvent> out = new ArrayList<>(rows.size());
        for (var r : rows) {
            Map<String, Object> policy = Collections.emptyMap();
            try {
                if (r.policyJson != null && !r.policyJson.isBlank()) {
                    //noinspection unchecked
                    policy = objectMapper.readValue(r.policyJson, Map.class);
                }
            } catch (Exception ignored) { }
            out.add(new PolicyMatchEvent(
                    r.requestId,
                    r.userId,
                    r.decision,
                    r.confidence,
                    r.occurredAt,
                    policy
            ));
        }
        return out;
    }

    /**
     * EPIC 13.5: Simulate the impact of a (draft) policy against a historical session.
     *
     * No side-effects: does not write anything, does not enable the policy.
     */
    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@RequestBody PolicySimulationRequest req) {
        try {
            if (req == null || req.sessionId() == null || req.sessionId().isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("sessionId is required"));
            }
            var rowOpt = sessionFeatureRepository.findByRequestId(req.sessionId());
            if (rowOpt.isEmpty()) {
                return ResponseEntity.status(404).body(new ErrorResponse("session not found")); 
            }
            var row = rowOpt.get();

            // Build a context from stored feature_vector JSON, falling back to an empty map.
            Map<String, Object> values = new LinkedHashMap<>();
            if (row.featureVector != null && !row.featureVector.isBlank()) {
                try {
                    //noinspection unchecked
                    values = objectMapper.readValue(row.featureVector, Map.class);
                } catch (Exception ignored) { }
            }
            PolicyContext ctx = PolicyEngine.context(values);

            // Determine baseline outcome: evaluate current stored policies to get a "would be today" outcome
            String tenantId = req.tenantId();
            String userId = req.userId() != null ? req.userId() : row.userId;
            PolicyOutcome baseline = policyEngine.evaluate(tenantId, userId, ctx);

            // Evaluate the draft policy itself (independent)
            Map<String, Object> cond = parseMap(req.conditionJson());
            boolean draftMatches = com.poc.api.service.policy.PolicyMatcher.matches(cond, ctx.values());
            Map<String, Object> action = parseMap(req.actionJson());

            // Apply draft action to a simulated decision+confidence
            String beforeDecision = row.decision;
            double beforeConfidence = row.confidence;

            String afterDecision = beforeDecision;
            double afterConfidence = beforeConfidence;

            Object d = action.get("decision");
            if (d instanceof String s && !s.isBlank()) {
                afterDecision = s.trim();
            }
            Object cap = action.get("confidence_cap");
            if (cap instanceof Number n) {
                double c = n.doubleValue();
                if (!Double.isNaN(c) && c >= 0.0 && c <= 1.0) {
                    afterConfidence = Math.min(afterConfidence, c);
                }
            }
            String reason = null;
            Object r = action.get("reason");
            if (r instanceof String s && !s.isBlank()) reason = s;

            PolicySimulationResult out = new PolicySimulationResult(
                    req.sessionId(),
                    row.userId,
                    beforeDecision,
                    beforeConfidence,
                    draftMatches,
                    afterDecision,
                    afterConfidence,
                    reason,
                    baseline.matched(),
                    baseline.policyId(),
                    baseline.description()
            );
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> parseMap(Object maybeJson) throws Exception {
        if (maybeJson == null) return Collections.emptyMap();
        if (maybeJson instanceof Map<?, ?> m) {
            //noinspection unchecked
            return (Map<String, Object>) m;
        }
        if (maybeJson instanceof String s) {
            if (s.isBlank()) return Collections.emptyMap();
            //noinspection unchecked
            return objectMapper.readValue(s, Map.class);
        }
        // unknown type -> try serialize + parse
        String json = objectMapper.writeValueAsString(maybeJson);
        //noinspection unchecked
        return objectMapper.readValue(json, Map.class);
    }

    static class ErrorResponse {
        public final String error;
        ErrorResponse(String error) { this.error = error; }
    }
}