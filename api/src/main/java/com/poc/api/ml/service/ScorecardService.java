package com.poc.api.ml.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.risk.persistence.DecisionLogRepository;
import com.poc.api.risk.persistence.DecisionLogRow;
import com.poc.api.ml.persistence.ModelScorecardRepository;
import com.poc.api.ml.persistence.ModelScorecardRepository.ScorecardRow;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScorecardService {

  private final DecisionLogRepository decisionLogRepository;
  private final ModelScorecardRepository scorecardRepository;
  private final ObjectMapper om = new ObjectMapper();

  public ScorecardService(DecisionLogRepository decisionLogRepository,
                          ModelScorecardRepository scorecardRepository) {
    this.decisionLogRepository = decisionLogRepository;
    this.scorecardRepository = scorecardRepository;
  }

  public long generateGlobalScorecard(String triggerType, OffsetDateTime pivot, int n, Long modelId, String modelVersion) {
    Metrics baseline = computeMetrics(decisionLogRepository.findLastNBefore(pivot, n));
    Metrics recovery = computeMetrics(decisionLogRepository.findFirstNAfter(pivot, n));

    Map<String,Object> baseJson = baseline.toJson();
    Map<String,Object> recJson = recovery.toJson();
    Map<String,Object> deltaJson = delta(baseJson, recJson);

    String status = classifyStatus(baseJson, recJson, baseline.count, recovery.count);

    try {
      return scorecardRepository.insert(new ScorecardRow(
          0, OffsetDateTime.now(),
          "risk-model", "GLOBAL", "*",
          modelId, modelVersion, triggerType,
          baseline.count, recovery.count,
          om.writeValueAsString(baseJson),
          om.writeValueAsString(recJson),
          om.writeValueAsString(deltaJson),
          status,
          "pivot=" + pivot
      ));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String classifyStatus(Map<String,Object> base, Map<String,Object> rec, int bn, int rn) {
    if (bn < 20 || rn < 20) return "INCONCLUSIVE";
    double baseDeny = (double) base.get("denyRate");
    double recDeny = (double) rec.get("denyRate");
    double baseAllow = (double) base.get("allowRate");
    double recAllow = (double) rec.get("allowRate");
    if ((recDeny - baseDeny) > 0.05) return "REGRESSION";
    if ((recAllow - baseAllow) < -0.05) return "REGRESSION";
    return "OK";
  }

  private Map<String,Object> delta(Map<String,Object> base, Map<String,Object> rec) {
    Map<String,Object> d = new LinkedHashMap<>();
    for (String k : base.keySet()) {
      Object bv = base.get(k);
      Object rv = rec.get(k);
      if (bv instanceof Number && rv instanceof Number) {
        d.put(k, ((Number)rv).doubleValue() - ((Number)bv).doubleValue());
      }
    }
    return d;
  }

  private Metrics computeMetrics(List<com.poc.api.risk.persistence.DecisionLogRow> rows) {
    Metrics m = new Metrics();
    for (var r : rows) {
      m.count++;
      String dec = r.decision;
      if ("ALLOW".equalsIgnoreCase(dec)) m.allow++;
      else if ("DENY".equalsIgnoreCase(dec)) m.deny++;
      else m.challenge++;
      m.confSum += r.confidence;
    }
    return m;
  }

  private static class Metrics {
    int count = 0;
    int allow = 0;
    int deny = 0;
    int challenge = 0;
    double confSum = 0;

    Map<String,Object> toJson() {
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("n", count);
      m.put("allowRate", count == 0 ? 0.0 : (double)allow / count);
      m.put("denyRate", count == 0 ? 0.0 : (double)deny / count);
      m.put("challengeRate", count == 0 ? 0.0 : (double)challenge / count);
      m.put("avgConfidence", count == 0 ? 0.0 : confSum / count);
      return m;
    }
  }
}
