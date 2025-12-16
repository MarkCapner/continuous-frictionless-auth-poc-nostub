package com.poc.api.showcase.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record TlsFamilyDetails(
    String familyId,
    String familyKey,
    String sampleTlsFp,
    long users,
    long seenCount,
    OffsetDateTime createdAt,
    OffsetDateTime firstSeen,
    OffsetDateTime lastSeen,
    long observationCount,
    long variantCount,
    Double confidence,
    Double stability,
    List<String> variants,
    Map<String, String> subject,
    Map<String, String> issuer
) {}
