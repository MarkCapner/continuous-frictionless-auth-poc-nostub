package com.poc.api.admin.controller;

import com.poc.api.showcase.dto.TlsFamilyBackfillResponse;
import com.poc.api.showcase.dto.TlsFamilyDetails;
import com.poc.api.showcase.dto.TlsFamilyShowcaseResponse;
import com.poc.api.showcase.persistence.TlsFamilyRepository;
import com.poc.api.showcase.service.TlsFamilyBackfillService;
import com.poc.api.telemetry.tls.TlsMetaParser;
import com.poc.api.telemetry.tls.TlsNormalizationResult;
import com.poc.api.telemetry.tls.TlsNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EPIC 9: Admin TLS family management APIs.
 */
@RestController
@RequestMapping({"/api/admin/tls-families","/api/v1/admin/tls-families"})
public class AdminTlsFamiliesController {

  private final TlsFamilyRepository repo;
  private final TlsFamilyBackfillService backfillService;
  private final String adminToken;

  public AdminTlsFamiliesController(
      TlsFamilyRepository repo,
      TlsFamilyBackfillService backfillService,
      @Value("${poc.admin.token:dev-admin}") String adminToken
  ) {
    this.repo = repo;
    this.backfillService = backfillService;
    this.adminToken = adminToken;
  }

  @GetMapping
  public ResponseEntity<List<TlsFamilyRepository.TlsFamilySummaryRow>> listFamilies(
      @RequestParam(name = "limit", defaultValue = "100") int limit
  ) {
    return ResponseEntity.ok(repo.listFamilies(limit));
  }

  @GetMapping("/lookup")
  public ResponseEntity<TlsFamilyDetails> lookupByFp(
      @RequestParam("fp") String rawTlsFp,
      @RequestParam(name = "variants_limit", defaultValue = "25") int variantsLimit
  ) {
    Optional<TlsFamilyRepository.FamilyLookup> found = repo.findFamilyByRawFp(rawTlsFp);
    if (found.isEmpty()) return ResponseEntity.notFound().build();
    var f = found.get();
    long users = repo.countUsersForFamily(f.familyId);
    List<String> variants = repo.listVariants(f.familyId, variantsLimit);

    Map<String, String> kv = TlsMetaParser.parseKv(f.sampleMeta);
    var subAttrs = TlsMetaParser.parseDnAttrs(kv.get("sub"));
    var issAttrs = TlsMetaParser.parseDnAttrs(kv.get("iss"));

    return ResponseEntity.ok(new TlsFamilyDetails(
        f.familyId,
        f.familyKey,
        f.sampleTlsFp,
        users,
        f.seenCount,
        f.createdAt,
        f.firstSeen,
        f.lastSeen,
        f.observationCount,
        f.variantCount,
        f.confidence,
        f.stability,
        variants,
        subAttrs,
        issAttrs
    ));
  }

  /**
   * EPIC 9.1.3: Admin-only mutation to force normalise & classify a TLS FP into a family.
   */
  @PostMapping("/force-classify")
  public ResponseEntity<TlsFamilyShowcaseResponse> forceClassify(
      @RequestParam("fp") String rawTlsFp,
      @RequestParam(name = "tls_meta", required = false) String tlsMeta,
      @RequestParam(name = "variants_limit", defaultValue = "10") int variantsLimit,
      @RequestHeader(name = "X-Admin-Token", required = false) String adminTokenHeader
  ) {
    requireAdmin(adminTokenHeader);

    TlsNormalizationResult n = TlsNormalizer.normalize(rawTlsFp, tlsMeta);
    if (!n.metaPresent()) {
      return ResponseEntity.badRequest().body(new TlsFamilyShowcaseResponse(
          rawTlsFp,
          true,
          "tls_meta is required to force classify (missing subject/issuer)",
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
      ));
    }

    // Persist family and membership.
    repo.upsertFamily(n.familyId(), n.familyKey(), n.rawTlsFp(), n.rawMeta());
    repo.upsertMember(n.rawTlsFp(), n.familyId(), n.rawMeta());

    // EPIC 9.1.5: Update derived stats & scores immediately so the UI reflects it.
    repo.getFamilyStats(n.familyId()).ifPresent(stats -> {
      var scores = com.poc.api.showcase.service.TlsFamilyScoring.compute(
          stats.observationCount,
          stats.variantCount,
          stats.lastSeen,
          OffsetDateTime.now()
      );
      repo.recomputeFamilyStats(n.familyId(), scores.confidence(), scores.stability());
    });

    // Return a stable DTO using the showcase-style payload.
    return lookupShowcaseStyle(rawTlsFp, variantsLimit);
  }

  /**
   * EPIC 9.1.4: Admin-triggered backfill of TLS families for historical TLS fingerprints.
   */
  @PostMapping("/backfill")
  public ResponseEntity<TlsFamilyBackfillResponse> backfillTlsFamilies(
      @RequestParam(name = "batchSize", defaultValue = "500") int batchSize,
      @RequestParam(name = "maxBatches", defaultValue = "20") int maxBatches,
      @RequestHeader(name = "X-Admin-Token", required = false) String adminTokenHeader
  ) {
    requireAdmin(adminTokenHeader);
    var result = backfillService.backfill(batchSize, maxBatches);
    return ResponseEntity.ok(new TlsFamilyBackfillResponse(
        result.processed(),
        result.classified(),
        result.batches(),
        result.complete(),
        result.lastFp()
    ));
  }

  private ResponseEntity<TlsFamilyShowcaseResponse> lookupShowcaseStyle(String rawTlsFp, int variantsLimit) {
    Optional<TlsFamilyRepository.FamilyLookup> found = repo.findFamilyByRawFp(rawTlsFp);
    if (found.isEmpty()) {
      // For admin force-classify, this is unexpected but keep behaviour similar.
      return ResponseEntity.ok(TlsFamilyShowcaseResponse.notObserved(rawTlsFp));
    }
    var f = found.get();
    long users = repo.countUsersForFamily(f.familyId);
    List<String> variants = repo.listVariants(f.familyId, variantsLimit);

    Map<String, String> kv = TlsMetaParser.parseKv(f.sampleMeta);
    var subAttrs = TlsMetaParser.parseDnAttrs(kv.get("sub"));
    var issAttrs = TlsMetaParser.parseDnAttrs(kv.get("iss"));

    return ResponseEntity.ok(new TlsFamilyShowcaseResponse(
        rawTlsFp,
        false,
        null,
        f.familyId,
        f.familyKey,
        f.sampleTlsFp,
        users,
        f.seenCount,
        f.createdAt,
        f.lastSeen,
        variants,
        subAttrs,
        issAttrs,
        f.confidence,
        f.stability
    ));
  }

  private void requireAdmin(String providedToken) {
    String expected = (adminToken == null) ? "" : adminToken.trim();
    if (!expected.isBlank()) {
      if (providedToken == null || providedToken.isBlank() || !expected.equals(providedToken.trim())) {
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.FORBIDDEN,
            "Missing/invalid admin token"
        );
      }
    }
  }
}
