package com.poc.api.risk.dto;

import java.util.List;
import java.util.Map;

public record DecisionResponse(
    String decision,
    double confidence,
    Map<String, Double> breakdown,
    List<String> explanations,
    String session_id,
    String tls_fp,
    String tls_meta,
    String model_version,
    Map<String, Object> policy
) {}
