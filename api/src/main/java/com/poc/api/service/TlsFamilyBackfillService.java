package com.poc.api.service;

import com.poc.api.persistence.TlsFamilyRepository;
import com.poc.api.tls.TlsNormalizationResult;
import com.poc.api.tls.TlsNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * EPIC 9.1.4: Backfill TLS families for historical TLS fingerprints.
 *
 * Goal:
 *  - Ensure every historically observed TLS FP is normalised & classified into a family.
 *  - Be safe, resumable and idempotent.
 *
 * Approach:
 *  - We only process TLS FPs that are NOT yet present in tls_family_member.
 *  - We page lexicographically using a cursor (afterFp).
 *  - For each FP we best-effort fetch the latest tls_meta from session_feature to
 *    improve normalisation.
 */
@Service
public class TlsFamilyBackfillService {

  private static final Logger log = LoggerFactory.getLogger(TlsFamilyBackfillService.class);

  private final TlsFamilyRepository repo;

  public TlsFamilyBackfillService(TlsFamilyRepository repo) {
    this.repo = repo;
  }

  public BackfillResult backfill(int batchSize, int maxBatches) {
    int batchesRun = 0;
    long processed = 0;
    long classified = 0;
    String cursor = "";

    java.util.Set<String> touchedFamilies = new java.util.HashSet<>();

    while (batchesRun < maxBatches) {
      List<String> fps = repo.listUnclassifiedObservedTlsFps(cursor, batchSize);
      if (fps.isEmpty()) {
        break;
      }

      for (String fp : fps) {
        processed++;
        cursor = fp;

        String tlsMeta = repo.findLatestTlsMetaForFp(fp);
        TlsNormalizationResult n = TlsNormalizer.normalize(fp, tlsMeta);

        // Persist family and membership (idempotent via upserts + we only process missing members).
        repo.upsertFamily(n.familyId(), n.familyKey(), n.rawTlsFp(), n.rawMeta());
        repo.upsertMember(n.rawTlsFp(), n.familyId(), n.rawMeta());
        touchedFamilies.add(n.familyId());
        classified++;
      }

      batchesRun++;
      log.info("[tls-backfill] batch {} complete: processed={}, lastFp='{}'", batchesRun, processed, cursor);
    }

    // EPIC 9.1.5: Recompute family stats & scores once per touched family.
    var now = java.time.OffsetDateTime.now();
    for (String familyId : touchedFamilies) {
      repo.getFamilyStats(familyId).ifPresent(stats -> {
        var scores = TlsFamilyScoring.compute(stats.observationCount, stats.variantCount, stats.lastSeen, now);
        repo.recomputeFamilyStats(familyId, scores.confidence(), scores.stability());
      });
    }

    boolean complete = batchesRun < maxBatches;
    return new BackfillResult(processed, classified, batchesRun, complete, cursor);
  }

  public record BackfillResult(
      long processed,
      long classified,
      int batches,
      boolean complete,
      String lastFp
  ) {}
}
