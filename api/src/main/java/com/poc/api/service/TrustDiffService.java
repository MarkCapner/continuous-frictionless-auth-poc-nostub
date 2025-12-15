package com.poc.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.model.TrustDiffItem;
import com.poc.api.persistence.SessionFeatureRow;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * EPIC 12.3: "What changed since last time" diff engine.
 *
 * Produces a small, user-safe list of plain-language change statements between the current session
 * and a prior trusted session.
 */
@Service
public class TrustDiffService {

    private final ObjectMapper objectMapper;

    public TrustDiffService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TrustDiffItem> diff(SessionFeatureRow current, SessionFeatureRow baseline) {
        if (current == null || baseline == null) return List.of();

        List<TrustDiffItem> out = new ArrayList<>();

        // TLS
        if (notBlank(current.tlsFp) && notBlank(baseline.tlsFp) && !Objects.equals(current.tlsFp, baseline.tlsFp)) {
            out.add(item("TLS", "Your network fingerprint looks different compared to last time.", "MEDIUM"));
        }

        Map<String, Object> curDevice = safeMap(current.deviceJson);
        Map<String, Object> baseDevice = safeMap(baseline.deviceJson);
        deviceDiff(curDevice, baseDevice, out);

        Map<String, Object> curBehaviour = safeMap(current.behaviorJson);
        Map<String, Object> baseBehaviour = safeMap(baseline.behaviorJson);
        behaviourDiff(curBehaviour, baseBehaviour, out);

        Map<String, Object> curContext = safeMap(current.contextJson);
        Map<String, Object> baseContext = safeMap(baseline.contextJson);
        contextDiff(curContext, baseContext, out);

        // Keep it minimal and stable for demos.
        if (out.size() > 8) {
            return out.subList(0, 8);
        }
        return out;
    }

    private void deviceDiff(Map<String, Object> cur, Map<String, Object> base, List<TrustDiffItem> out) {
        if (cur.isEmpty() || base.isEmpty()) return;

        // Only look at a small, user-friendly set of dimensions.
        checkKey(cur, base, out, List.of("timezone", "tz"), "DEVICE", "Your timezone appears to have changed.", "LOW");
        checkKey(cur, base, out, List.of("language", "locale"), "DEVICE", "Your language/locale appears to have changed.", "LOW");
        checkKey(cur, base, out, List.of("screen", "screenSize", "resolution"), "DEVICE", "Your screen characteristics look different.", "LOW");
        checkKey(cur, base, out, List.of("browser", "browserName", "uaBrowser"), "DEVICE", "Your browser looks different compared to last time.", "MEDIUM");
        checkKey(cur, base, out, List.of("os", "platform", "uaPlatform"), "DEVICE", "Your operating system / platform looks different.", "MEDIUM");
        checkKey(cur, base, out, List.of("mobile", "isMobile", "deviceClass"), "DEVICE", "Your device type (mobile/desktop) looks different.", "MEDIUM");
    }

    private void behaviourDiff(Map<String, Object> cur, Map<String, Object> base, List<TrustDiffItem> out) {
        if (cur.isEmpty() || base.isEmpty()) return;

        // Many builds store behavioural z-scores by feature name. We'll scan numeric leaf values and
        // summarise the biggest shifts, avoiding raw numbers.
        List<NumChange> changes = new ArrayList<>();
        collectNumericChanges("", cur, base, changes);

        // Filter noise: ignore small drifts.
        changes.removeIf(c -> Math.abs(c.delta) < 0.75);
        if (changes.isEmpty()) return;

        changes.sort((a, b) -> Double.compare(Math.abs(b.delta), Math.abs(a.delta)));

        // Create at most two behavioural messages to keep it readable.
        Set<String> buckets = new LinkedHashSet<>();
        for (NumChange c : changes) {
            String bucket = bucketForBehaviourKey(c.keyPath);
            if (bucket != null) buckets.add(bucket);
            if (buckets.size() >= 2) break;
        }

        for (String bucket : buckets) {
            String severity = "LOW";
            if ("typing".equals(bucket)) {
                severity = "MEDIUM";
                out.add(item("BEHAVIOUR", "Your typing rhythm was different compared to your usual pattern.", severity));
            } else if ("scroll".equals(bucket)) {
                out.add(item("BEHAVIOUR", "Your scrolling pattern looked different compared to last time.", severity));
            } else if ("pointer".equals(bucket)) {
                out.add(item("BEHAVIOUR", "Your mouse / pointer movement pattern looked different.", severity));
            } else {
                out.add(item("BEHAVIOUR", "Your interaction pattern looked a bit different today.", severity));
            }
        }
    }

    private void contextDiff(Map<String, Object> cur, Map<String, Object> base, List<TrustDiffItem> out) {
        if (cur.isEmpty() || base.isEmpty()) return;

        checkKey(cur, base, out, List.of("country", "ipCountry", "geoCountry"), "CONTEXT", "Your approximate location looks different.", "MEDIUM");
        checkKey(cur, base, out, List.of("asn", "ipAsn", "networkAsn"), "CONTEXT", "Your network provider looks different.", "LOW");
        checkKey(cur, base, out, List.of("vpn", "isVpn", "proxy"), "CONTEXT", "Your connection type looks different (e.g., VPN/proxy).", "MEDIUM");
    }

    private void checkKey(Map<String, Object> cur,
                          Map<String, Object> base,
                          List<TrustDiffItem> out,
                          List<String> keys,
                          String dimension,
                          String message,
                          String severity) {
        Object a = firstPresent(cur, keys);
        Object b = firstPresent(base, keys);
        if (a == null || b == null) return;
        if (!Objects.equals(normalize(a), normalize(b))) {
            out.add(item(dimension, message, severity));
        }
    }

    private Object firstPresent(Map<String, Object> map, List<String> keys) {
        for (String k : keys) {
            if (map.containsKey(k)) return map.get(k);
        }
        return null;
    }

    private Object normalize(Object o) {
        if (o == null) return null;
        if (o instanceof String s) return s.trim();
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof Boolean b) return b;
        return String.valueOf(o);
    }

    private Map<String, Object> safeMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private TrustDiffItem item(String dimension, String change, String severity) {
        TrustDiffItem i = new TrustDiffItem();
        i.dimension = dimension;
        i.change = change;
        i.severity = severity;
        return i;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static class NumChange {
        String keyPath;
        double delta;
        NumChange(String keyPath, double delta) { this.keyPath = keyPath; this.delta = delta; }
    }

    @SuppressWarnings("unchecked")
    private void collectNumericChanges(String prefix,
                                      Object cur,
                                      Object base,
                                      List<NumChange> out) {
        if (cur == null || base == null) return;

        if (cur instanceof Map<?, ?> cm && base instanceof Map<?, ?> bm) {
            for (Map.Entry<?, ?> e : cm.entrySet()) {
                String key = String.valueOf(e.getKey());
                Object curV = e.getValue();
                Object baseV = ((Map<Object, Object>) bm).get(e.getKey());
                collectNumericChanges(join(prefix, key), curV, baseV, out);
            }
            return;
        }

        // Leaf: numeric vs numeric
        Double c = asDouble(cur);
        Double b = asDouble(base);
        if (c != null && b != null) {
            out.add(new NumChange(prefix, c - b));
        }
    }

    private String join(String prefix, String key) {
        if (prefix == null || prefix.isBlank()) return key;
        return prefix + "." + key;
    }

    private Double asDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception ignore) { return null; }
        }
        return null;
    }

    private String bucketForBehaviourKey(String key) {
        if (key == null) return null;
        String k = key.toLowerCase(Locale.ROOT);
        if (k.contains("key") || k.contains("type") || k.contains("keystroke") || k.contains("typing")) return "typing";
        if (k.contains("scroll")) return "scroll";
        if (k.contains("mouse") || k.contains("pointer") || k.contains("cursor")) return "pointer";
        return "other";
    }
}
