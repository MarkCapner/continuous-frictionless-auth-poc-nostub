package com.poc.api.service;

import com.poc.api.persistence.TlsFamilyRepository;
import com.poc.api.tls.TlsNormalizationResult;
import com.poc.api.tls.TlsNormalizer;
import org.springframework.stereotype.Service;

@Service
public class TlsFamilyService {

  private final TlsFamilyRepository repo;

  public TlsFamilyService(TlsFamilyRepository repo) {
    this.repo = repo;
  }

  public Observation observe(String userId, String tlsFp, String tlsMeta) {
    TlsNormalizationResult n = TlsNormalizer.normalize(tlsFp, tlsMeta);

    // Persist families & membership.
    repo.upsertFamily(n.familyId(), n.familyKey(), n.rawTlsFp(), n.rawMeta());
    repo.upsertMember(n.rawTlsFp(), n.familyId(), n.rawMeta());
    boolean newForUser = repo.upsertUserFamily(userId, n.familyId());

    // Score heuristic:
    // - if meta present and family seen for user: strong
    // - if meta present but new family: weak
    // - if meta missing: neutral (can't family-cluster)
    double tlsScore;
    double familyDrift;
    if (!n.metaPresent()) {
      tlsScore = (tlsFp != null && !tlsFp.isBlank()) ? 0.7 : 0.5;
      familyDrift = 0.25;
    } else if (newForUser) {
      tlsScore = 0.35;
      familyDrift = 1.0;
    } else {
      tlsScore = 0.90;
      familyDrift = 0.0;
    }

    return new Observation(
        n.familyId(),
        n.familyKey(),
        tlsScore,
        familyDrift,
        n.metaPresent()
    );
  }

  public record Observation(
      String familyId,
      String familyKey,
      double tlsScore,
      double familyDrift,
      boolean metaPresent
  ) {}
}
