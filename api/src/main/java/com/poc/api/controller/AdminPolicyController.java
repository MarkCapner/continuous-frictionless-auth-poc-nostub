package com.poc.api.controller;

import com.poc.api.model.PolicyRule;
import com.poc.api.service.PolicyRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policy")
public class AdminPolicyController {

    private final PolicyRuleService service;
    private final com.poc.api.persistence.SessionFeatureRepository sessionFeatureRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public AdminPolicyController(PolicyRuleService service, com.poc.api.persistence.SessionFeatureRepository sessionFeatureRepository, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.service = service;
        this.sessionFeatureRepository = sessionFeatureRepository;
        this.objectMapper = objectMapper;
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
    public ResponseEntity<?> create(@RequestBody PolicyRule rule) {
        try {
            long id = service.create(rule);
            return ResponseEntity.ok(service.findById(id).orElseThrow());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") long id, @RequestBody PolicyRule rule) {
        try {
            service.update(id, rule);
            return ResponseEntity.ok(service.findById(id).orElseThrow());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<?> setEnabled(@PathVariable("id") long id, @RequestParam("enabled") boolean enabled) {
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
    public java.util.List<com.poc.api.model.PolicyMatchEvent> matches(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        int capped = Math.max(1, Math.min(limit, 200));
        var rows = sessionFeatureRepository.findRecentPolicyMatches(capped);
        java.util.List<com.poc.api.model.PolicyMatchEvent> out = new java.util.ArrayList<>(rows.size());
        for (var r : rows) {
            java.util.Map<String,Object> policy = java.util.Collections.emptyMap();
            try {
                if (r.policyJson != null && !r.policyJson.isBlank()) {
                    policy = objectMapper.readValue(r.policyJson, java.util.Map.class);
                }
            } catch (Exception ignored) {}
            out.add(new com.poc.api.model.PolicyMatchEvent(
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
    static class ErrorResponse {
        public final String error;
        ErrorResponse(String error) { this.error = error; }
    }
}
