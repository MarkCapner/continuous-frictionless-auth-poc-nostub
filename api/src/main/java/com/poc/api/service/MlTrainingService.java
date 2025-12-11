package com.poc.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.ml.FeatureVectorSchema;
import com.poc.api.ml.ModelProvider;
import com.poc.api.persistence.ModelRegistryRepository;
import com.poc.api.persistence.SessionFeatureRepository;
import com.poc.api.persistence.SessionFeatureRow;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * EPIC 5: Training pipeline that turns persisted session_feature rows into
 * Tribuo training examples and updates the active in-memory model in ModelProvider,
 * while also recording metadata in the model_registry table.
 */
@Service
public class MlTrainingService {

  private final SessionFeatureRepository sessionFeatureRepository;
  private final ModelRegistryRepository modelRegistryRepository;
  private final ModelProvider modelProvider;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public MlTrainingService(SessionFeatureRepository sessionFeatureRepository,
                           ModelRegistryRepository modelRegistryRepository,
                           ModelProvider modelProvider) {
    this.sessionFeatureRepository = sessionFeatureRepository;
    this.modelRegistryRepository = modelRegistryRepository;
    this.modelProvider = modelProvider;
  }

  @PostConstruct
  public void initialTrainIfPossible() {
    // Best-effort training from any existing labeled rows.
    retrainFromRecent(500);
  }

  public void retrainFromRecent(int limit) {
    List<SessionFeatureRow> rows = sessionFeatureRepository.findRecentWithLabel(limit);
    if (rows.isEmpty()) {
      return;
    }

    List<ModelProvider.TrainingExample> examples = new ArrayList<>();
    List<double[]> vectors = new ArrayList<>();
    OffsetDateTime newest = null;

    for (SessionFeatureRow row : rows) {
      Map<String, Double> breakdown = parseBreakdown(row.featureVector);
      if (breakdown == null || breakdown.isEmpty()) {
        continue;
      }
      double[] v = FeatureVectorSchema.fromBreakdown(breakdown);
      String label = row.label != null ? row.label : row.decision;
      if (label == null) {
        continue;
      }
      examples.add(new ModelProvider.TrainingExample(v, label));
      vectors.add(v);
      if (newest == null || row.occurredAt.isAfter(newest)) {
        newest = row.occurredAt;
      }
    }

    if (examples.isEmpty()) {
      return;
    }

    modelProvider.train(examples, vectors);

    // Record metadata in model_registry.
    String version = modelProvider.getModelVersion();
    String bytesStr = "MODEL:" + version;
    byte[] bytes = bytesStr.getBytes(StandardCharsets.UTF_8);
    String sha256 = sha256(bytes);

    modelRegistryRepository.deactivateAll();
    modelRegistryRepository.insert(
        "behavior-risk-model",
        "TRIBUO_LOGREG_IFOREST",
        version,
        bytes,
        sha256,
        true
    );
  }

  private Map<String, Double> parseBreakdown(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      return null;
    }
  }

  private static String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(bytes);
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
