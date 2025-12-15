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

    public AdminPolicyController(PolicyRuleService service) {
        this.service = service;
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

    static class ErrorResponse {
        public final String error;
        ErrorResponse(String error) { this.error = error; }
    }
}
