package com.poc.api.controller;

import com.poc.api.ml.ModelProvider;
import com.poc.api.persistence.ModelRegistryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/model")
public class AdminModelController {

    private final ModelProvider modelProvider;
    private final ModelRegistryRepository registry;

    public AdminModelController(ModelProvider modelProvider, ModelRegistryRepository registry) {
        this.modelProvider = modelProvider;
        this.registry = registry;
    }

    @GetMapping("/info")
    public Map<String, Object> getModelInfo() {
        // Avoid Map.of(...) because it throws NPE if any value is null.
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("modelVersion", safe(modelProvider.getModelVersion(), "unknown"));
        out.put("serverTime", Instant.now().toString());

        registry.findActive().ifPresentOrElse(rec -> {
            out.put("activeModelId", rec.id());
            out.put("activeCreatedAt", rec.createdAt() != null ? rec.createdAt().toString() : null);
            out.put("activeVersion", safe(rec.version(), "unknown"));
            out.put("artifactBytes", rec.bytes() != null ? rec.bytes().length : 0);
        }, () -> {
            out.put("activeModelId", null);
            out.put("activeCreatedAt", null);
            out.put("activeVersion", null);
            out.put("artifactBytes", 0);
        });

        return out;
    }

    private static String safe(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
