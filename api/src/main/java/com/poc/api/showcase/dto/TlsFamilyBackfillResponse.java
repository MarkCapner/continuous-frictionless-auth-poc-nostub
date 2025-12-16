package com.poc.api.showcase.dto;

public record TlsFamilyBackfillResponse(
    long processed,
    long classified,
    int batches,
    boolean complete,
    String lastFp
) {}
