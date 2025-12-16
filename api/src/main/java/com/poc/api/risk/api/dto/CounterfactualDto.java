package com.poc.api.risk.api.dto;

public record CounterfactualDto(
    String feature,
    double deltaRequired,
    String explanation
) {}
