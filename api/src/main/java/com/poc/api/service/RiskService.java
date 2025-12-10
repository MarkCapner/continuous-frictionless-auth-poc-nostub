package com.poc.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.ml.ModelProvider;
import com.poc.api.model.DecisionResponse;
import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DecisionLogRepository;
import com.poc.api.persistence.DeviceProfile;
import com.poc.api.persistence.SessionFeatureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskService {

  private final DeviceProfileService deviceProfileService;
  private final BehaviorStatsService behaviorStatsService;
  private final FeatureBuilder featureBuilder;
  private final RulesEngine rulesEngine;
  private final ModelProvider modelProvider;
  private final SessionFeatureRepository sessionFeatureRepository;
  private final DecisionLogRepository decisionLogRepository;
  private final AccountSharingHeuristics accountSharingHeuristics;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RiskService(DeviceProfileService deviceProfileService,
                     BehaviorStatsService behaviorStatsService,
                     FeatureBuilder featureBuilder,
                     RulesEngine rulesEngine,
                     ModelProvider modelProvider,
                     SessionFeatureRepository sessionFeatureRepository,
                     DecisionLogRepository decisionLogRepository,
                     AccountSharingHeuristics accountSharingHeuristics) {
    this.deviceProfileService = deviceProfileService;
    this.behaviorStatsService = behaviorStatsService;
    this.featureBuilder = featureBuilder;
    this.rulesEngine = rulesEngine;
    this.modelProvider = modelProvider;
    this.sessionFeatureRepository = sessionFeatureRepository;
    this.decisionLogRepository = decisionLogRepository;
    this.accountSharingHeuristics = accountSharingHeuristics;
  }

  public DecisionResponse score(String tlsFp, String tlsMeta, Telemetry telemetry, String ip, String reqId) {
    String userId = telemetry.user_id_hint() != null ? telemetry.user_id_hint() : "anonymous";
    String sessionId = (reqId != null && !reqId.isBlank()) ? reqId : UUID.randomUUID().toString();

    String country = null;
    boolean vpn = false;
    boolean highRiskAction = false;
    if (telemetry.context() != null) {
      Object c = telemetry.context().get("country");
      if (c instanceof String s) country = s;
      Object v = telemetry.context().get("vpn");
      if (v instanceof Boolean b) vpn = b;
      Object h = telemetry.context().get("high_risk_action");
      if (h instanceof Boolean b) highRiskAction = b;
    }

    // Upsert device profile
    DeviceProfile profile = deviceProfileService.upsert(userId, tlsFp, country, telemetry.device());

    // Behavior similarity + stats update
    BehaviorStatsService.BehaviorSimilarityResult behaviorRes =
        behaviorStatsService.updateAndComputeSimilarity(userId, telemetry.behavior());

    // Build feature vector
    FeatureBuilder.Features features = featureBuilder.build(profile, behaviorRes.score(), tlsFp, telemetry);

    // Anomaly score based on behavior similarity (1 - similarity, clamped to [0,1])
    double anomalyScore = 1.0 - behaviorRes.score();
    if (anomalyScore < 0.0) anomalyScore = 0.0;
    if (anomalyScore > 1.0) anomalyScore = 1.0;

    // ML prediction via Tribuo, then penalise by anomalyScore
    double rawPLegit = modelProvider.predict(features.deviceScore(), features.behaviorScore(),
        features.tlsScore(), features.contextScore());
    double pLegit = rawPLegit * (1.0 - 0.5 * anomalyScore);

    // Rules
    RulesEngine.FeaturesWithContext fctx = new RulesEngine.FeaturesWithContext(
        profile,
        country,
        vpn,
        profile.seenCount <= 1,
        profile.seenCount <= 1, // new TLS FP approximated by seenCount==1
        highRiskAction,
        profile.lastSeen
    );
    RulesEngine.Decision decisionEnum = rulesEngine.apply(fctx, pLegit);
    String decision = decisionEnum.name();

    // Account sharing / multi-device heuristics
    AccountSharingHeuristics.Result heuristics = accountSharingHeuristics.evaluate(userId);
    boolean suspiciousAccountSharing = heuristics.suspicious();
    if (suspiciousAccountSharing && decisionEnum == RulesEngine.Decision.AUTO_LOGIN) {
      decisionEnum = RulesEngine.Decision.STEP_UP;
      decision = decisionEnum.name();
    }

    java.util.List<String> explanations = new java.util.ArrayList<>();
    explanations.add("ML + rules engine decision");
    if (suspiciousAccountSharing) {
      explanations.add("Account-sharing heuristic triggered: "
          + heuristics.tlsFingerprintCount() + " TLS fingerprints, "
          + heuristics.countryCount() + " countries observed for this user");
    }

    Map<String, Double> breakdown = Map.of(
        "device_score", features.deviceScore(),
        "behavior_score", features.behaviorScore(),
        "tls_score", features.tlsScore(),
        "context_score", features.contextScore(),
        "anomaly_score", anomalyScore
    );

    // Persist session features
    try {
      String deviceJson = objectMapper.writeValueAsString(telemetry.device());
      String behaviorJson = objectMapper.writeValueAsString(telemetry.behavior());
      String contextJson = objectMapper.writeValueAsString(telemetry.context());
      String featureVectorJson = objectMapper.writeValueAsString(breakdown);
      sessionFeatureRepository.insert(userId, sessionId, tlsFp != null ? tlsFp : "none",
          deviceJson, behaviorJson, contextJson, featureVectorJson, decision, pLegit, null);
    } catch (JsonProcessingException e) {
      // In PoC we don't fail the request on logging errors.
    }

    // Persist decision log
    decisionLogRepository.insert(sessionId, userId, tlsFp != null ? tlsFp : "none",
        features.behaviorScore(), features.deviceScore(), features.contextScore(), pLegit, decision);

    return new DecisionResponse(
        decision,
        pLegit,
        breakdown,
        explanations,
        sessionId,
        tlsFp != null ? tlsFp : "none",
        tlsMeta
    );
  }
}
