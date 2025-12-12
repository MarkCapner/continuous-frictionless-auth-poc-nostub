package com.poc.api.tls;

import java.util.Map;

public record TlsNormalizationResult(
    String rawTlsFp,
    String rawMeta,
    String familyId,
    String familyKey,
    Map<String, String> subjectAttrs,
    Map<String, String> issuerAttrs,
    boolean metaPresent
) {}
