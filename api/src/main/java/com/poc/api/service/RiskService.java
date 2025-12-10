package com.poc.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.ml.ModelProvider;
import com.poc.api.model.DecisionResponse;
import com.poc.api.model.Telemetry;
import com.poc.api.persistence.DecisionLogRepository;
import com.poc.api.persistence.DeviceProfile;
import com.poc.api.persistence.SessionFeatureRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private final ContextEnricher contextEnricher;
  private final MeterRegistry meterRegistry;
  private static final Logger log = LoggerFactory.getLogger(RiskService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RiskService(DeviceProfileService deviceProfileService,
                     BehaviorStatsService behaviorStatsService,
                     FeatureBuilder featureBuilder,
                     RulesEngine rulesEngine,
                     ModelProvider modelProvider,
                     SessionFeatureRepository sessionFeatureRepository,
                     DecisionLogRepository decisionLogRepository,
                     ContextEnricher contextEnricher,
                     MeterRegistry meterRegistry) {
    this.deviceProfileService = deviceProfileService;
    this.behaviorStatsService = behaviorStatsService;
    this.featureBuilder = featureBuilder;
    this.rulesEngine = rulesEngine;
    this.modelProvider = modelProvider;
    this.sessionFeatureRepository = sessionFeatureRepository;
    this.decisionLogRepository = decisionLogRepository;
    this.contextEnricher = contextEnricher;
    this.meterRegistry = meterRegistry;
  }

    public DecisionResponse score(String tlsFp, Telemetry telemetry, String ip, String reqId) {
    long startNs = System.nanoTime();
    String userId = telemetry.user_id_hint() != null && !telemetry.user_id_hint().isBlank()
        ? telemetry.user_id_hint()
        : "anonymous";
    String sessionId = (reqId != null && !reqId.isBlank()) ? reqId : UUID.randomUUID().toString();

    Map<String, Object> context = contextEnricher.enrich(ip, telemetry.context());

    String country = null;
    boolean vpn = false;
    boolean highRiskAction = false;
    boolean profilingOptOut = false;
    if (context != null) {
      Object c = context.get("country");
      if (c instanceof String s) {
        country = s;
      }
      Object v = context.get("vpn");
      if (v instanceof Boolean b) {
        vpn = b;
      }
      Object a = context.get("highRiskAction");
      if (a instanceof Boolean b) {
        highRiskAction = b;
      }
      Object o = context.get("profilingOptOut");
      if (o instanceof Boolean b) {
        profilingOptOut = b;
      }
    }

    // Update behavior stats and compute similarity
    BehaviorStatsService.BehaviorSimilarityResult behaviorResult =
        behaviorStatsService.updateAndComputeSimilarity(userId, telemetry.behavior());
    double behaviorSim = behaviorResult.score();

    // Upsert device profile and derive simple context flags
    DeviceProfile deviceProfile = deviceProfileService.upsert(userId, tlsFp, country, telemetry.device());
    boolean newDevice = deviceProfile != null && deviceProfile.seenCount == 1L;
    boolean newTlsFingerprint = newDevice; // same uniqueness key (userId + tlsFp + canvas)

    // Build feature vector used by the ML layer
    FeatureBuilder.Features features =
        featureBuilder.build(deviceProfile, behaviorSim, tlsFp, telemetry, context);

    // ML probability that this session is legit
    double pLegit = modelProvider.predict(
        features.deviceScore(),
        features.behaviorScore(),
        features.tlsScore(),
        features.contextScore()
    );

    // Rules engine on top of ML
    RulesEngine.FeaturesWithContext fctx = new RulesEngine.FeaturesWithContext(
        deviceProfile,
        country,
        vpn,
        newDevice,
        newTlsFingerprint,
        highRiskAction,
        deviceProfile != null ? deviceProfile.lastSeen : null
    );
    RulesEngine.Decision rulesDecision = rulesEngine.apply(fctx, pLegit);

    String decision;
    switch (rulesDecision) {
      case ALLOW -> decision = "AUTO_LOGIN";
      case CHALLENGE -> decision = "STEP_UP";
      case DENY -> decision = "DENY";
      default -> decision = "DENY";
    }

    var breakdown = Map.of(
        "device_score", features.deviceScore(),
        "behavior_score", features.behaviorScore(),
        "tls_score", features.tlsScore(),
        "context_score", features.contextScore()
    );

    if (!profilingOptOut) {
      try {
        String deviceJson = objectMapper.writeValueAsString(telemetry.device());
        String behaviorJson = objectMapper.writeValueAsString(telemetry.behavior());
        String contextJson = objectMapper.writeValueAsString(context);
        String featureVectorJson = objectMapper.writeValueAsString(breakdown);
        sessionFeatureRepository.insert(userId, sessionId, tlsFp != null ? tlsFp : "none",
            deviceJson, behaviorJson, contextJson, featureVectorJson, decision, pLegit, null);
      } catch (JsonProcessingException e) {
        // In PoC we don't fail the request on logging errors.
      }

      decisionLogRepository.insert(sessionId, userId, tlsFp != null ? tlsFp : "none",
          features.behaviorScore(), features.deviceScore(), features.contextScore(), pLegit, decision);
    }

    long durationNs = System.nanoTime() - startNs;
    meterRegistry.timer("riskapi.score.latency").record(durationNs, java.util.concurrent.TimeUnit.NANOSECONDS);
    meterRegistry.counter("riskapi.score.total").increment();
    meterRegistry.counter("riskapi.score.decision", "decision", decision).increment();

    log.info("risk_decision sessionId={} userId={} tlsFp={} decision={} pLegit={} deviceScore={} behaviorScore={} tlsScore={} contextScore={} country={} vpn={} profilingOptOut={}",
        sessionId, userId, tlsFp != null ? tlsFp : "none", decision, pLegit,
        features.deviceScore(), features.behaviorScore(), features.tlsScore(), features.contextScore(),
        country, vpn, profilingOptOut);

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
