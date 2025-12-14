package com.poc.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.ml.FeatureVectorSchema;
import com.poc.api.ml.ModelProvider;
import com.poc.api.persistence.ModelRegistryRepository;
import com.poc.api.persistence.SessionFeatureRepository;
import com.poc.api.persistence.SessionFeatureRow;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class MlTrainingService {

  public record TrainResult(long modelId, String version, Map<String,Object> metrics) {}

  private final SessionFeatureRepository sessionFeatureRepository;
  private final ModelRegistryRepository modelRegistryRepository;
  private final ModelProvider modelProvider;
  private final ObjectMapper om = new ObjectMapper();

  public MlTrainingService(SessionFeatureRepository sessionFeatureRepository,
                           ModelRegistryRepository modelRegistryRepository,
                           ModelProvider modelProvider) {
    this.sessionFeatureRepository = sessionFeatureRepository;
    this.modelRegistryRepository = modelRegistryRepository;
    this.modelProvider = modelProvider;
  }

  public void retrainFromRecent(int limit) {
    retrainFromRecentWithResult(limit);
  }

  public TrainResult retrainFromRecentWithResult(int limit) {
    List<SessionFeatureRow> rows = sessionFeatureRepository.findRecentWithLabel(Math.min(limit, 2000));
    if (rows.isEmpty()) {
      return new TrainResult(0L, "none", Map.of("trained_examples", 0));
    }

    List<ModelProvider.TrainingExample> examples = new ArrayList<>();
    List<double[]> vectors = new ArrayList<>();

    for (SessionFeatureRow r : rows) {
      double[] v = extractBase4(r.featureVector);
      vectors.add(v);

      boolean legit = "ALLOW".equalsIgnoreCase(r.decision);
      examples.add(new ModelProvider.TrainingExample(v, legit));
    }

    modelProvider.train(examples, vectors);
    String version = modelProvider.getModelVersion();

    byte[] bytes = modelProvider.exportArtifactBytes();
    String sha256 = sha256(bytes);

    modelRegistryRepository.deactivateAll();
    long modelId = modelRegistryRepository.insertReturningId(
        "behavior-risk-model",
        "JAVA_SERIALIZED_TRIBUO_LOGREG_IFOREST",
        version,
        bytes,
        sha256,
        true,
        "risk-model",
        "GLOBAL",
        "*",
        json(Map.of("trained_examples", examples.size(), "trained_at", Instant.now().toString()))
    );

    modelProvider.setActiveFromRegistry(modelId, bytes);

    return new TrainResult(modelId, version, Map.of("trained_examples", examples.size(), "sha256", sha256));
  }

  private double[] extractBase4(String featureVectorJson) {
    // Default zeros
    double device = 0, behavior = 0, tls = 0, context = 0;
    if (featureVectorJson != null && !featureVectorJson.isBlank()) {
      try {
        JsonNode n = om.readTree(featureVectorJson);
        // Accept either object {device_score:..} or array [..]
        if (n.isObject()) {
          device = n.path("device_score").asDouble(0);
          behavior = n.path("behavior_score").asDouble(0);
          tls = n.path("tls_score").asDouble(0);
          context = n.path("context_score").asDouble(0);
        } else if (n.isArray() && n.size() >= 4) {
          device = n.get(0).asDouble(0);
          behavior = n.get(1).asDouble(0);
          tls = n.get(2).asDouble(0);
          context = n.get(3).asDouble(0);
        }
      } catch (Exception ignore) {}
    }
    return FeatureVectorSchema.fromScores(device, behavior, tls, context);
  }

  private String json(Map<String,Object> m) {
    try { return om.writeValueAsString(m); } catch (Exception e) { return "{}"; }
  }

  private static String sha256(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(bytes == null ? new byte[0] : bytes);
      return Base64.getEncoder().encodeToString(dig);
    } catch (Exception e) {
      return "";
    }
  }
}
