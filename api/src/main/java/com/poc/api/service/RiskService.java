package com.poc.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.ml.ModelProvider;
import com.poc.api.model.DecisionResponse;
import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DecisionLogRepository;
import com.poc.api.persistence.DeviceProfile;
import com.poc.api.persistence.SessionFeatureRepository;
import com.poc.api.persistence.ModelCanaryPolicyRepository;
import com.poc.api.persistence.ModelRegistryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

import com.poc.api.service.AccountSharingHeuristics;
import com.poc.api.service.UserReputationService;

@Service
public class RiskService {

  private final DeviceProfileService deviceProfileService;
  private final BehaviorStatsService behaviorStatsService;
  private final FeatureBuilder featureBuilder;
  private final RulesEngine rulesEngine;
  private final ModelProvider modelProvider;
  private final ModelCanaryPolicyRepository canaryPolicyRepository;
  private final ModelRegistryRepository modelRegistryRepository;
  private final SessionFeatureRepository sessionFeatureRepository;
  private final DecisionLogRepository decisionLogRepository;
  private final AccountSharingHeuristics accountSharingHeuristics;
  private final UserReputationService userReputationService;
  private final TlsFamilyService tlsFamilyService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RiskService(DeviceProfileService deviceProfileService,
                     BehaviorStatsService behaviorStatsService,
                     FeatureBuilder featureBuilder,
                     RulesEngine rulesEngine,
                     ModelProvider modelProvider,
                     ModelCanaryPolicyRepository canaryPolicyRepository,
                     ModelRegistryRepository modelRegistryRepository,
                     SessionFeatureRepository sessionFeatureRepository,
                     DecisionLogRepository decisionLogRepository,
                     AccountSharingHeuristics accountSharingHeuristics,
                     UserReputationService userReputationService,
                     TlsFamilyService tlsFamilyService) {
    this.deviceProfileService = deviceProfileService;
    this.behaviorStatsService = behaviorStatsService;
    this.featureBuilder = featureBuilder;
    this.rulesEngine = rulesEngine;
    this.modelProvider = modelProvider;
    this.canaryPolicyRepository = canaryPolicyRepository;
    this.modelRegistryRepository = modelRegistryRepository;
    this.sessionFeatureRepository = sessionFeatureRepository;
    this.decisionLogRepository = decisionLogRepository;
    this.accountSharingHeuristics = accountSharingHeuristics;
    this.userReputationService = userReputationService;
    this.tlsFamilyService = tlsFamilyService;
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

    // EPIC 9: normalise TLS and assign to a family.
    TlsFamilyService.Observation tlsObs = tlsFamilyService.observe(userId, tlsFp, tlsMeta);

    // Upsert device profile (raw TLS FP is still stored for traceability)
    DeviceProfile profile = deviceProfileService.upsert(userId, tlsFp, country, telemetry.device());

    // Behavior similarity + stats update
    BehaviorStatsService.BehaviorSimilarityResult behaviorRes =
        behaviorStatsService.updateAndComputeSimilarity(userId, telemetry.behavior());

    // Build feature vector
    FeatureBuilder.Features features = featureBuilder.build(profile, behaviorRes.score(), tlsFp, telemetry, tlsObs.tlsScore());

    // ML prediction via Tribuo (probability of being legit)
long selectedModelId = selectModelIdForCanary(userId, sessionId);
double pLegit = modelProvider.predictWithModelId(
    selectedModelId,
    features.deviceScore(),
    features.behaviorScore(),
    features.tlsScore(),
    features.contextScore()
);

// EPIC 5: Isolation-Forest-based anomaly score
double anomalyScore = modelProvider.anomalyScoreWithModelId(
    selectedModelId,
    features.deviceScore(),
    features.behaviorScore(),
    features.tlsScore(),
    features.contextScore()
);

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

    Map<String, Double> breakdown = Map.of(
        "device_score", features.deviceScore(),
        "behavior_score", features.behaviorScore(),
        "tls_score", features.tlsScore(),
        "context_score", features.contextScore()
    );

    Map<String, Double> enrichedBreakdown = new LinkedHashMap<>(breakdown);

    // EPIC 5: add anomaly score and selected behavioural z-scores + device/TLS rarity.
    enrichedBreakdown.put("ml_anomaly_score", anomalyScore);
    enrichedBreakdown.put("model_id_used", (double) selectedModelId);
    enrichedBreakdown.put("canary_enabled", isCanarySelected(userId, sessionId, selectedModelId) ? 1.0 : 0.0);

    behaviorRes.zScores().forEach((featureName, z) -> {
      enrichedBreakdown.put("behavior_z_" + featureName, z);
      // For the schema we care about a few canonical ones; others are still persisted
      // but will simply be ignored by the current FeatureVectorSchema.
      if ("key_interval_mean".equals(featureName)) {
        enrichedBreakdown.put("behavior_z_key_interval_mean", z);
      } else if ("key_interval_std".equals(featureName)) {
        enrichedBreakdown.put("behavior_z_key_interval_std", z);
      } else if ("scroll_rate".equals(featureName)) {
        enrichedBreakdown.put("behavior_z_scroll_rate", z);
      }
    });

    // Device / TLS rarity style metrics
    double seenCountLog = profile.seenCount > 0 ? Math.log10(1.0 + profile.seenCount) : 0.0;
    enrichedBreakdown.put("device_seen_count_log", seenCountLog);
    enrichedBreakdown.put("tls_fp_seen_count", (double) profile.seenCount);

    // EPIC 9: TLS family clustering signals (numeric so they appear in breakdown charts)
    enrichedBreakdown.put("tls_family_drift", tlsObs.familyDrift());
    enrichedBreakdown.put("tls_family_meta_present", tlsObs.metaPresent() ? 1.0 : 0.0);

    // EPIC 6: user-level intelligence & reputation
    var sharing = accountSharingHeuristics.evaluate(userId);
    var reputation = userReputationService.evaluate(userId);

    enrichedBreakdown.put("user_trust_score", reputation.trustScore());
    enrichedBreakdown.put("user_account_sharing_risk", reputation.accountSharingRisk());
    enrichedBreakdown.put("user_device_count", (double) reputation.deviceCount());
    enrichedBreakdown.put("user_tls_fp_count", (double) reputation.tlsFingerprintCount());
    enrichedBreakdown.put("user_country_count", (double) reputation.countryCount());
    enrichedBreakdown.put("user_sessions_30d", (double) reputation.sessionsLast30d());

    // Persist session features
    try {
      String deviceJson = objectMapper.writeValueAsString(telemetry.device());
      String behaviorJson = objectMapper.writeValueAsString(telemetry.behavior());
      String contextJson = objectMapper.writeValueAsString(telemetry.context());
      // Persist numeric feature_vector plus a few string fields for explainability.
      Map<String, Object> fv = new LinkedHashMap<>();
      fv.putAll(enrichedBreakdown);
      fv.put("tls_family_id", tlsObs.familyId());
      fv.put("tls_family_key", tlsObs.familyKey());
      String featureVectorJson = objectMapper.writeValueAsString(fv);
      sessionFeatureRepository.insert(userId, sessionId, tlsFp != null ? tlsFp : "none",
          deviceJson, behaviorJson, contextJson, featureVectorJson, decision, pLegit, null);
    } catch (JsonProcessingException e) {
      // In PoC we don't fail the request on logging errors.
    }

    // Persist decision log
    decisionLogRepository.insert(sessionId, userId, tlsFp != null ? tlsFp : "none",
        features.behaviorScore(), features.deviceScore(), features.contextScore(), pLegit, decision);

    var reasons = List.of(
        String.format("Rules decision: %s", decision),
        String.format("Model p(legit)=%.3f", pLegit),
        String.format("ML anomaly score=%.3f", anomalyScore),
        String.format("Behavior similarity score %.2f", features.behaviorScore()),
        String.format("User trust score=%.2f (devices=%d, countries=%d)",
            reputation.trustScore(), reputation.deviceCount(), reputation.countryCount()),
        String.format("Account sharing risk=%.2f (TLS fingerprints=%d)",
            reputation.accountSharingRisk(), reputation.tlsFingerprintCount())
    );

    return new DecisionResponse(
        decision,
        pLegit,
        enrichedBreakdown,
        reasons,
        sessionId,
        tlsFp != null ? tlsFp : "none",
        tlsMeta,
        modelProvider.getModelVersion()
    );
  }

  private long selectModelIdForCanary(String userId, String requestId) {
    var active = modelRegistryRepository.findActive().orElse(null);
    long activeId = active == null ? 0L : active.id();

    var c = canaryPolicyRepository.get("risk-model", "GLOBAL", "*").orElse(null);
    if (c == null || !c.enabled() || c.rolloutPercent() <= 0) return activeId;

    String key = (requestId != null && !requestId.isBlank()) ? requestId : userId;
    int bucket = stableBucket(key);
    if (bucket < c.rolloutPercent()) return c.modelId();
    return activeId;
  }

  private boolean isCanarySelected(String userId, String requestId, long selectedModelId) {
    var c = canaryPolicyRepository.get("risk-model", "GLOBAL", "*").orElse(null);
    if (c == null || !c.enabled() || c.rolloutPercent() <= 0) return false;
    return selectedModelId == c.modelId();
  }

  private int stableBucket(String key) {
    int h = (key == null) ? 0 : key.hashCode();
    h ^= (h >>> 16);
    long u = h & 0xffffffffL;
    return (int) (u % 100);
  }
}
