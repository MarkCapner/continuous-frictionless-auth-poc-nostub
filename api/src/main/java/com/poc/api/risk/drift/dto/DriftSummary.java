package com.poc.api.risk.drift.dto;

import java.util.List;

public record DriftSummary(
    double deviceDrift,
    double behaviorDrift,
    double tlsDrift,
    double featureDrift,
    double modelInstability,
    double maxDrift,
    List<String> warnings,
    String tlsFamily,
    String deviceSig
) {}
