package com.poc.api.showcase.service;

import com.poc.api.showcase.dto.TrustSignal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EPIC 12.2
 *
 * Converts risk outputs + derived features into deterministic, user-safe plain language.
 *
 * Notes:
 *  - No raw telemetry
 *  - No hashes/fingerprints
 *  - No ML jargon
 */
@Service
public class TrustExplanationService {

  public record Explanation(String riskSummary, List<TrustSignal> signals) {}

  public Explanation explain(String decision, double confidence, Map<String, Object> fv) {
    if (decision == null || decision.isBlank()) decision = "UNKNOWN";

    TrustSignal device = deviceSignal(fv);
    TrustSignal behaviour = behaviourSignal(fv);
    TrustSignal tls = tlsSignal(fv);
    TrustSignal context = contextSignal(fv);

    List<TrustSignal> signals = List.of(device, behaviour, tls, context);

    String summary = buildSummary(decision, confidence, signals);
    return new Explanation(summary, signals);
  }

  private String buildSummary(String decision, double confidence, List<TrustSignal> signals) {
    // Pick up to two most important non-OK signals (deterministic ordering).
    List<TrustSignal> concerns = new ArrayList<>();
    for (TrustSignal s : signals) {
      if (!"OK".equalsIgnoreCase(s.status)) concerns.add(s);
    }
    concerns.sort(Comparator
        .comparingInt((TrustSignal s) -> statusRank(s.status))
        .thenComparing(s -> s.category == null ? "" : s.category));

    String c = String.format(Locale.ROOT, "%.2f", confidence);

    if ("ALLOW".equalsIgnoreCase(decision)) {
      if (concerns.isEmpty()) {
        return "We recognised you and your usual patterns look consistent (confidence " + c + ").";
      }
      TrustSignal top = concerns.get(0);
      // Example: "We recognised your device, but your typing rhythm was unusual today."
      String but = clauseFor(top);
      return "We recognised you, but " + but + " (confidence " + c + ").";
    }

    if ("STEP_UP".equalsIgnoreCase(decision)) {
      if (concerns.isEmpty()) {
        return "We need an extra check for this session (confidence " + c + ").";
      }
      String because = joinClauses(concerns, 2);
      return "We need an extra check because " + because + " (confidence " + c + ").";
    }

    if ("BLOCK".equalsIgnoreCase(decision)) {
      if (concerns.isEmpty()) {
        return "We couldn't trust this session (confidence " + c + ").";
      }
      String because = joinClauses(concerns, 2);
      return "We couldn't trust this session because " + because + " (confidence " + c + ").";
    }

    // Unknown / other decisions.
    if (concerns.isEmpty()) {
      return "This session was evaluated (confidence " + c + ").";
    }
    return "This session was evaluated because " + joinClauses(concerns, 2) + " (confidence " + c + ").";
  }

  private String joinClauses(List<TrustSignal> concerns, int max) {
    List<String> clauses = new ArrayList<>();
    for (int i = 0; i < concerns.size() && i < max; i++) {
      clauses.add(clauseFor(concerns.get(i)));
    }
    if (clauses.isEmpty()) return "something looked different";
    if (clauses.size() == 1) return clauses.get(0);
    return clauses.get(0) + " and " + clauses.get(1);
  }

  private String clauseFor(TrustSignal s) {
    // Keep these short – they are embedded in the main sentence.
    if (s == null) return "something looked different";
    if ("DEVICE".equalsIgnoreCase(s.category)) return "this device looks new";
    if ("BEHAVIOUR".equalsIgnoreCase(s.category)) return "your interaction pattern was unusual today";
    if ("TLS".equalsIgnoreCase(s.category)) return "your network pattern changed";
    if ("CONTEXT".equalsIgnoreCase(s.category)) return "this session looked unusual";
    return "something looked different";
  }

  private int statusRank(String status) {
    if (status == null) return 10;
    String s = status.toUpperCase(Locale.ROOT);
    // Lower number = more severe.
    return switch (s) {
      case "ALERT", "HIGH", "BLOCK" -> 0;
      case "WARN", "WARNING" -> 1;
      case "INFO" -> 2;
      case "OK" -> 9;
      default -> 5;
    };
  }

  private TrustSignal deviceSignal(Map<String, Object> fv) {
    double seen = getDouble(fv, "device_seen_count_log");
    double score = getDouble(fv, "device_score");

    TrustSignal s = new TrustSignal();
    s.category = "DEVICE";

    if (seen <= 0.3) {
      s.status = "WARN";
      s.label = "Device looks new";
      s.explanation = "We haven't seen this device much for your account yet.";
      return s;
    }

    // If we have a score, provide a slightly richer explanation.
    if (score >= 0.65) {
      s.status = "WARN";
      s.label = "Device differs from usual";
      s.explanation = "This device is recognised, but some device characteristics differ from your usual pattern.";
      return s;
    }

    s.status = "OK";
    s.label = "Device recognised";
    s.explanation = "This device matches what we usually see for your account.";
    return s;
  }

  private TrustSignal behaviourSignal(Map<String, Object> fv) {
    double zMean = getDouble(fv, "behavior_z_key_interval_mean");
    double zStd = getDouble(fv, "behavior_z_key_interval_std");
    double zScroll = getDouble(fv, "behavior_z_scroll_rate");

    double maxAbs = maxAbs(zMean, zStd, zScroll);
    String top = topBehaviourDimension(zMean, zStd, zScroll);

    TrustSignal s = new TrustSignal();
    s.category = "BEHAVIOUR";

    if (maxAbs >= 3.0) {
      s.status = "WARN";
      s.label = "Interaction pattern changed";
      s.explanation = "Your " + top + " was noticeably different from your usual baseline today.";
      return s;
    }
    if (maxAbs >= 2.2) {
      s.status = "WARN";
      s.label = "Interaction slightly unusual";
      s.explanation = "Your " + top + " was a bit different from your usual baseline today.";
      return s;
    }

    s.status = "OK";
    s.label = "Behaviour consistent";
    s.explanation = "Your interaction patterns look similar to your usual baseline.";
    return s;
  }

  private TrustSignal tlsSignal(Map<String, Object> fv) {
    double drift = getDouble(fv, "tls_family_drift");
    double meta = getDouble(fv, "tls_family_meta_present");
    double score = getDouble(fv, "tls_score");

    TrustSignal s = new TrustSignal();
    s.category = "TLS";

    if (meta < 0.5) {
      s.status = "WARN";
      s.label = "Network details missing";
      s.explanation = "We couldn't fully observe the network details on this request.";
      return s;
    }

    if (drift >= 0.5 || score >= 0.70) {
      s.status = "WARN";
      s.label = "Network pattern changed";
      s.explanation = "Your network pattern looks different from what we usually see for your account.";
      return s;
    }

    s.status = "OK";
    s.label = "Network looks normal";
    s.explanation = "Your network pattern matches your usual profile.";
    return s;
  }

  private TrustSignal contextSignal(Map<String, Object> fv) {
    double anomaly = getDouble(fv, "ml_anomaly_score");
    double score = getDouble(fv, "context_score");

    TrustSignal s = new TrustSignal();
    s.category = "CONTEXT";

    // Use anomaly if present, otherwise fall back to context_score.
    double v = anomaly > 0.0 ? anomaly : score;

    if (v >= 0.80) {
      s.status = "WARN";
      s.label = "Session looks unusual";
      s.explanation = "This session looks more unusual than your typical sessions.";
      return s;
    }
    if (v >= 0.65) {
      s.status = "WARN";
      s.label = "Session slightly unusual";
      s.explanation = "This session looks a bit different from your typical sessions.";
      return s;
    }

    s.status = "OK";
    s.label = "Session looks typical";
    s.explanation = "Overall, this session looks similar to your typical sessions.";
    return s;
  }

  private double maxAbs(double... values) {
    double m = 0.0;
    for (double v : values) {
      double a = Math.abs(v);
      if (a > m) m = a;
    }
    return m;
  }

  private String topBehaviourDimension(double zMean, double zStd, double zScroll) {
    double aMean = Math.abs(zMean);
    double aStd = Math.abs(zStd);
    double aScroll = Math.abs(zScroll);
    if (aMean >= aStd && aMean >= aScroll) return "typing rhythm";
    if (aStd >= aMean && aStd >= aScroll) return "typing consistency";
    return "scrolling";
  }

  private double getDouble(Map<String, Object> map, String key) {
    if (map == null || key == null) return 0.0;
    Object v = map.get(key);
    if (v == null) return 0.0;
    if (v instanceof Number n) return n.doubleValue();
    try {
      return Double.parseDouble(String.valueOf(v));
    } catch (Exception e) {
      return 0.0;
    }
  }
}
