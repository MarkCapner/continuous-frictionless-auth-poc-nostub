package com.poc.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.model.TrustSignal;
import com.poc.api.model.TrustSnapshot;
import com.poc.api.persistence.SessionFeatureRepository;
import com.poc.api.persistence.SessionFeatureRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TrustSnapshotService {

    private final SessionFeatureRepository sessionFeatureRepository;
    private final ObjectMapper objectMapper;

    public TrustSnapshotService(SessionFeatureRepository sessionFeatureRepository, ObjectMapper objectMapper) {
        this.sessionFeatureRepository = sessionFeatureRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<TrustSnapshot> buildForSession(String sessionId) {
        Optional<SessionFeatureRow> rowOpt = sessionFeatureRepository.findByRequestId(sessionId);
        if (rowOpt.isEmpty()) return Optional.empty();

        SessionFeatureRow row = rowOpt.get();

        Map<String, Object> fv = safeParseJsonMap(row.featureVector);

        double deviceSeen = getDouble(fv, "device_seen_count_log");
        double tlsFamilyDrift = getDouble(fv, "tls_family_drift");
        double tlsMetaPresent = getDouble(fv, "tls_family_meta_present");
        double anomalyScore = getDouble(fv, "ml_anomaly_score");

        double maxBehaviorAbsZ = maxAbsBehaviorZ(fv);

        TrustSnapshot snap = new TrustSnapshot();
        snap.sessionId = sessionId;
        snap.decision = row.decision;
        snap.confidence = row.confidence;

        // Minimal, user-safe summary for 12.1 (12.2 will expand this).
        snap.riskSummary = summarise(row.decision, row.confidence, deviceSeen, maxBehaviorAbsZ, tlsFamilyDrift, anomalyScore);

        List<TrustSignal> signals = new ArrayList<>();
        signals.add(deviceSignal(deviceSeen));
        signals.add(behaviourSignal(maxBehaviorAbsZ));
        signals.add(tlsSignal(tlsFamilyDrift, tlsMetaPresent));
        signals.add(contextSignal(anomalyScore));

        snap.signals = signals;
        return Optional.of(snap);
    }

    private Map<String, Object> safeParseJsonMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double maxAbsBehaviorZ(Map<String, Object> fv) {
        double max = 0.0;
        for (var e : fv.entrySet()) {
            String k = e.getKey();
            if (k != null && k.startsWith("behavior_z_")) {
                double z = getDouble(fv, k);
                double az = Math.abs(z);
                if (az > max) max = az;
            }
        }
        return max;
    }

    private TrustSignal deviceSignal(double deviceSeenCountLog) {
        TrustSignal s = new TrustSignal();
        s.category = "DEVICE";
        if (deviceSeenCountLog <= 0.3) {
            s.status = "WARN";
            s.label = "Device looks new";
            s.explanation = "We haven't seen this device much for your account yet.";
        } else {
            s.status = "OK";
            s.label = "Device recognised";
            s.explanation = "This device matches what we usually see for your account.";
        }
        return s;
    }

    private TrustSignal behaviourSignal(double maxAbsZ) {
        TrustSignal s = new TrustSignal();
        s.category = "BEHAVIOUR";
        if (maxAbsZ >= 2.5) {
            s.status = "WARN";
            s.label = "Typing / interaction changed";
            s.explanation = "Some interaction patterns differ from your usual baseline today.";
        } else {
            s.status = "OK";
            s.label = "Behaviour consistent";
            s.explanation = "Your interaction patterns look similar to your usual baseline.";
        }
        return s;
    }

    private TrustSignal tlsSignal(double tlsFamilyDrift, double metaPresent) {
        TrustSignal s = new TrustSignal();
        s.category = "TLS";
        if (metaPresent < 0.5) {
            s.status = "WARN";
            s.label = "Network fingerprint incomplete";
            s.explanation = "We couldn't fully observe your network fingerprint on this request.";
        } else if (tlsFamilyDrift >= 0.5) {
            s.status = "WARN";
            s.label = "Network fingerprint changed";
            s.explanation = "Your network fingerprint looks different from what we usually see.";
        } else {
            s.status = "OK";
            s.label = "Network looks normal";
            s.explanation = "Your network fingerprint matches your usual pattern.";
        }
        return s;
    }

    private TrustSignal contextSignal(double anomalyScore) {
        TrustSignal s = new TrustSignal();
        s.category = "CONTEXT";
        if (anomalyScore >= 0.75) {
            s.status = "WARN";
            s.label = "Session looks unusual";
            s.explanation = "This session looks more unusual than your typical sessions.";
        } else {
            s.status = "OK";
            s.label = "Session looks typical";
            s.explanation = "Overall, this session looks similar to your typical sessions.";
        }
        return s;
    }

    private String summarise(String decision, double confidence, double deviceSeen, double maxAbsZ, double tlsDrift, double anomaly) {
        String base;
        if (decision == null) decision = "UNKNOWN";
        switch (decision) {
            case "ALLOW" -> base = "You're trusted on this session.";
            case "STEP_UP" -> base = "We need an extra check on this session.";
            case "BLOCK" -> base = "We couldn't trust this session.";
            default -> base = "This session was evaluated.";
        }

        List<String> reasons = new ArrayList<>();
        if (deviceSeen <= 0.3) reasons.add("device looks new");
        if (maxAbsZ >= 2.5) reasons.add("behaviour changed");
        if (tlsDrift >= 0.5) reasons.add("network fingerprint changed");
        if (anomaly >= 0.75) reasons.add("session looks unusual");

        String reason = reasons.isEmpty() ? "signals look consistent" : String.join(", ", reasons);

        return base + " (" + reason + ", confidence " + String.format("%.2f", confidence) + ")";
    }
}
