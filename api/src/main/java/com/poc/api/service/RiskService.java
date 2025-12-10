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
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RiskService(DeviceProfileService deviceProfileService,
                     BehaviorStatsService behaviorStatsService,
                     FeatureBuilder featureBuilder,
                     RulesEngine rulesEngine,
                     ModelProvider modelProvider,
                     SessionFeatureRepository sessionFeatureRepository,
                     DecisionLogRepository decisionLogRepository) {
    this.deviceProfileService = deviceProfileService;
    this.behaviorStatsService = behaviorStatsService;
    this.featureBuilder = featureBuilder;
    this.rulesEngine = rulesEngine;
    this.modelProvider = modelProvider;
    this.sessionFeatureRepository = sessionFeatureRepository;
    this.decisionLogRepository = decisionLogRepository;
  }

  public DecisionResponse score(String tlsFp, Telemetry telemetry, String ip, String reqId) {
    String userId = telemetry.user_id_hint() != null && !telemetry.user_id_hint().isBlank()
        ? telemetry.user_id_hint()
        : "anonymous";
    String sessionId = (reqId != null && !reqId.isBlank()) ? reqId : UUID.randomUUID().toString();

    String country = null;
    boolean vpn = false;
    boolean highRiskAction = false;
    boolean profilingOptOut = false;
    if (telemetry.context() != null) {
      Object c = telemetry.context().get("country");
      if (c instanceof String s) {
        country = s;
      }
      Object v = telemetry.context().get("vpn");
      if (v instanceof Boolean b) {
        vpn = b;
      }
      Object h = telemetry.context().get("high_risk_action");
      if (h instanceof Boolean b) {
        highRiskAction = b;
      }
      Object o = telemetry.context().get("profiling_opt_out");
      if (o instanceof Boolean b) {
        profilingOptOut = b;
      }
    }

    DeviceProfile profile = null;
    if (!profilingOptOut) {
      profile = deviceProfileService.upsert(userId, tlsFp, country, telemetry.device());
    }

    BehaviorStatsService.BehaviorSimilarityResult behaviorRes =
        behaviorStatsService.updateAndComputeSimilarity(
            profilingOptOut ? null : userId,
            telemetry.behavior()
        );

    FeatureBuilder.Features features =
        featureBuilder.build(profile, behaviorRes.score(), tlsFp, telemetry);

    double pLegit = modelProvider.predict(
        features.deviceScore(),
        features.behaviorScore(),
        features.tlsScore(),
        features.contextScore()
    );

    RulesEngine.FeaturesWithContext fctx = new RulesEngine.FeaturesWithContext(
        profile,
        country,
        vpn,
        profile == null || profile.seenCount <= 1,
        profile == null || profile.seenCount <= 1,
        highRiskAction,
        profile != null ? profile.lastSeen : null
    );

    RulesEngine.Decision rulesDecision = rulesEngine.apply(fctx, pLegit);
    String decision;
    switch (rulesDecision) {
      case ALLOW -> decision = "AUTO_LOGIN";
      case CHALLENGE -> decision = "STEP_UP";
      case DENY -> decision = "DENY";
      default -> decision = "DENY";
    }

    Map<String, Double> breakdown = Map.of(
        "device_score", features.deviceScore(),
        "behavior_score", features.behaviorScore(),
        "tls_score", features.tlsScore(),
        "context_score", features.contextScore()
    );

    if (!profilingOptOut) {
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
    }

    var explanations = profilingOptOut
        ? List.of("ML + rules engine decision", "profiling opt-out: no data stored")
        : List.of("ML + rules engine decision");

    return new DecisionResponse(
        decision,
        pLegit,
        breakdown,
        explanations,
        sessionId
    );
  }
}
