
package com.poc.api.risk.drift.narrative;

import java.util.*;

public class DriftNarrativeService {

    public Narrative explain(Map<String, Double> signalDrift) {
        String primary = signalDrift.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");

        String summary = switch (primary) {
            case "BEHAVIOUR" -> "Primary drift driven by behavioural change compared to the user's baseline.";
            case "TLS" -> "Primary drift driven by changes in TLS fingerprint characteristics.";
            case "DEVICE" -> "Primary drift driven by device characteristic changes.";
            case "IDENTITY" -> "Primary drift driven by identity or reputation changes.";
            default -> "No dominant drift signal detected.";
        };

        return new Narrative(primary, summary, signalDrift);
    }

    public record Narrative(String dominantSignal, String summary, Map<String, Double> signalDrift) {}
}
