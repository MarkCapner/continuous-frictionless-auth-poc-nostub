package com.poc.api.controller;

import com.poc.api.ml.ModelProvider;
import com.poc.api.persistence.ModelRegistryRepository;
import com.poc.api.service.MlTrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple admin endpoints for inspecting and retraining the ML model (EPIC 5).
 *
 * These are intentionally lightweight and unauthenticated for the PoC; in a
 * real deployment they must be protected behind admin auth.
 */
@RestController
@RequestMapping("/api/admin/model")
public class AdminModelController {

  private final MlTrainingService mlTrainingService;
  private final ModelProvider modelProvider;
  private final ModelRegistryRepository modelRegistryRepository;

  public AdminModelController(MlTrainingService mlTrainingService,
                              ModelProvider modelProvider,
                              ModelRegistryRepository modelRegistryRepository) {
    this.mlTrainingService = mlTrainingService;
    this.modelProvider = modelProvider;
    this.modelRegistryRepository = modelRegistryRepository;
  }

  @GetMapping
  public ResponseEntity<Map<String, Object>> getModelInfo() {
    Map<String, Object> body = new HashMap<>();
    body.put("ready", modelProvider.isReady());
    body.put("modelVersion", modelProvider.getModelVersion());

    ModelRegistryRepository.ModelRecord active = modelRegistryRepository.findActive().orElse(null);
    if (active != null) {
      body.put("registryName", active.name());
      body.put("registryFormat", active.format());
      body.put("registryVersion", active.version());
      OffsetDateTime createdAt = active.createdAt();
      body.put("lastTrainedAt", createdAt);
    } else {
      body.put("registryName", null);
      body.put("registryFormat", null);
      body.put("registryVersion", null);
      body.put("lastTrainedAt", null);
    }

    return ResponseEntity.ok(body);
  }

  @PostMapping("/retrain")
  public ResponseEntity<Map<String, Object>> retrain(@RequestParam(name = "limit", defaultValue = "500") int limit) {
    mlTrainingService.retrainFromRecent(limit);
    // Return fresh info after retraining
    return getModelInfo();
  }
}
