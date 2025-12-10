package com.poc.api.model;

import java.util.List;
import java.util.Map;

public record DecisionResponse(
    String decision,
    double confidence,
    Map<String, Double> breakdown,
    List<String> explanations,
    String session_id
) {}
