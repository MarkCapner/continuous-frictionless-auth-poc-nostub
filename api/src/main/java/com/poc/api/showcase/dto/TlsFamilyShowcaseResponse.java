package com.poc.api.showcase.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record TlsFamilyShowcaseResponse(
    String fp,
    boolean notObserved,
    String message,
    String familyId,
    String familyKey,
    String sampleTlsFp,
    long users,
    long seenCount,
    OffsetDateTime createdAt,
    OffsetDateTime lastSeen,
    List<String> variants,
    Map<String, String> subject,
    Map<String, String> issuer,
    Double confidence,
    Double stability
) {

  public static TlsFamilyShowcaseResponse notObserved(String fp) {
    return new TlsFamilyShowcaseResponse(
        fp,
        true,
        "Family not yet observed",
        null,
        null,
        null,
        0,
        0,
        null,
        null,
        List.of(),
        Map.of(),
        Map.of(),
        null,
        null
    );
  }
}
