
package com.poc.api.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.persistence.ModelRegistryRepository;
import com.poc.api.persistence.ModelRegistryRepository.ModelRecord;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Simple logistic regression style scorer implemented manually, without Tribuo.
 *
 * It still honours the model_registry table:
 * - On startup it loads the latest active model parameters if present and checksum matches.
 * - If none are present, it seeds a default set of weights and persisting them.
 * - Periodically it retrains weights from session_feature rows using a tiny batch gradient descent.
 */
@Component
public class ModelProvider {

  private static final Logger log = LoggerFactory.getLogger(ModelProvider.class);

  private static final String MODEL_NAME = "risk_manual_logreg";
  private static final String MODEL_VERSION = "v1";

  private final ModelRegistryRepository modelRegistryRepository;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private volatile Weights weights = new Weights(0.4, 0.3, 0.2, 0.1, -0.3);

  public ModelProvider(ModelRegistryRepository modelRegistryRepository,
                       JdbcTemplate jdbcTemplate) {
    this.modelRegistryRepository = modelRegistryRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  public record Weights(
      double wDevice,
      double wBehavior,
      double wTls,
      double wContext,
      double bias
  ) {}

  @PostConstruct
  public void init() {
    loadOrSeedWeights();
  }

  private void loadOrSeedWeights() {
    try {
      var activeOpt = modelRegistryRepository.findActive();
      if (activeOpt.isPresent()) {
        ModelRecord rec = activeOpt.get();
        byte[] bytes = rec.bytes();
        String expectedSha = rec.sha256();
        String actualSha = sha256Hex(bytes);
        if (!actualSha.equalsIgnoreCase(expectedSha)) {
          log.warn("Active model sha256 mismatch (expected {}, actual {}), seeding default weights",
              expectedSha, actualSha);
          seedDefaultWeights();
          return;
        }
        Weights loaded = deserializeWeights(bytes);
        if (loaded != null) {
          this.weights = loaded;
          log.info("Loaded active weights '{}' version {} from model_registry (id={})",
              rec.name(), rec.version(), rec.id());
          return;
        } else {
          log.warn("Failed to deserialize active weights from model_registry, seeding default weights");
        }
      } else {
        log.info("No active model in model_registry, seeding default weights");
      }
    } catch (Exception e) {
      log.warn("Error while loading weights from model_registry, seeding default weights", e);
    }

    seedDefaultWeights();
  }

  private void seedDefaultWeights() {
    this.weights = new Weights(0.4, 0.3, 0.2, 0.1, -0.3);
    persistWeights(this.weights, MODEL_VERSION);
  }

  private void persistWeights(Weights w, String version) {
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(w);
      String sha = sha256Hex(bytes);
      modelRegistryRepository.deactivateAll();
      modelRegistryRepository.insert(MODEL_NAME, "json-weights", version, bytes, sha, true);
      log.info("Persisted weights '{}' version {} (sha256={})", MODEL_NAME, version, sha);
    } catch (Exception e) {
      log.warn("Failed to persist weights to model_registry", e);
    }
  }

  private Weights deserializeWeights(byte[] bytes) {
    try {
      return objectMapper.readValue(bytes, Weights.class);
    } catch (Exception e) {
      log.warn("Failed to deserialize weights from bytes (base64={})",
          Base64.getEncoder().encodeToString(bytes), e);
      return null;
    }
  }

  private String sha256Hex(byte[] bytes) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(bytes);
    StringBuilder sb = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  /**
   * Periodic online update using recent session_feature rows.
   * Runs hourly and adjusts weights with a small batch gradient descent.
   */
  @Scheduled(cron = "0 0 * * * *")
  public void refreshFromSessionFeatures() {
    try {
      List<TrainingExample> examples = loadExamples(1, 1000);
      if (examples.size() < 50) {
        return;
      }
      Weights updated = trainLogistic(examples, this.weights, 0.05, 20);
      this.weights = updated;
      persistWeights(updated, MODEL_VERSION + "-online");
      log.info("Updated weights from session_feature data (n={})", examples.size());
    } catch (Exception e) {
      log.warn("Failed to refresh weights from session_feature data", e);
    }
  }

  private static class TrainingExample {
    final double device;
    final double behavior;
    final double tls;
    final double context;
    final int label; // 1 = legit, 0 = fraud

    TrainingExample(double device, double behavior, double tls, double context, int label) {
      this.device = device;
      this.behavior = behavior;
      this.tls = tls;
      this.context = context;
      this.label = label;
    }
  }

  private List<TrainingExample> loadExamples(int daysBack, int maxRows) {
    List<TrainingExample> examples = new ArrayList<>();

    String sql = "SELECT feature_vector, decision " +
        "FROM session_feature " +
        "WHERE occurred_at > now() - (? * INTERVAL '1 day') " +
        "ORDER BY occurred_at DESC " +
        "LIMIT ?";

    jdbcTemplate.query(sql, rs -> {
      String featureJson = rs.getString("feature_vector");
      String decision = rs.getString("decision");
      if (featureJson == null || decision == null) {
        return;
      }
      try {
        JsonNode node = objectMapper.readTree(featureJson);
        double device = node.path("device_score").asDouble(0.5);
        double behavior = node.path("behavior_score").asDouble(0.5);
        double tls = node.path("tls_score").asDouble(0.5);
        double context = node.path("context_score").asDouble(0.5);

        int y;
        if ("AUTO_LOGIN".equalsIgnoreCase(decision)) {
          y = 1;
        } else if ("DENY".equalsIgnoreCase(decision)) {
          y = 0;
        } else {
          // Skip STEP_UP etc.
          return;
        }

        examples.add(new TrainingExample(device, behavior, tls, context, y));
      } catch (Exception e) {
        // Skip malformed rows
      }
    }, daysBack, maxRows);

    return examples;
  }

  private Weights trainLogistic(List<TrainingExample> data, Weights initial,
                                double learningRate, int epochs) {
    double wD = initial.wDevice;
    double wB = initial.wBehavior;
    double wT = initial.wTls;
    double wC = initial.wContext;
    double b = initial.bias;

    for (int epoch = 0; epoch < epochs; epoch++) {
      double gradWD = 0.0;
      double gradWB = 0.0;
      double gradWT = 0.0;
      double gradWC = 0.0;
      double gradB = 0.0;

      for (TrainingExample ex : data) {
        double z = wD * ex.device + wB * ex.behavior + wT * ex.tls + wC * ex.context + b;
        double p = 1.0 / (1.0 + Math.exp(-z));
        double diff = p - ex.label; // derivative of log-loss

        gradWD += diff * ex.device;
        gradWB += diff * ex.behavior;
        gradWT += diff * ex.tls;
        gradWC += diff * ex.context;
        gradB += diff;
      }

      double n = data.size();
      if (n > 0) {
        gradWD /= n;
        gradWB /= n;
        gradWT /= n;
        gradWC /= n;
        gradB /= n;

        wD -= learningRate * gradWD;
        wB -= learningRate * gradWB;
        wT -= learningRate * gradWT;
        wC -= learningRate * gradWC;
        b  -= learningRate * gradB;
      }
    }

    return new Weights(wD, wB, wT, wC, b);
  }

  public double predict(double deviceScore,
                        double behaviorScore,
                        double tlsScore,
                        double contextScore) {
    Weights w = this.weights;
    double z = w.wDevice * deviceScore
        + w.wBehavior * behaviorScore
        + w.wTls * tlsScore
        + w.wContext * contextScore
        + w.bias;
    double p = 1.0 / (1.0 + Math.exp(-z));
    // Clamp numerically just in case
    if (p < 1e-6) p = 1e-6;
    if (p > 1.0 - 1e-6) p = 1.0 - 1e-6;
    return p;
  }
}
