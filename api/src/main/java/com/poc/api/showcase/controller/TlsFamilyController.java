package com.poc.api.showcase.controller;

import com.poc.api.showcase.persistence.TlsFamilyRepository;
import com.poc.api.showcase.service.TlsFamilyBackfillService;
import com.poc.api.telemetry.tls.TlsMetaParser;
import com.poc.api.telemetry.tls.TlsNormalizationResult;
import com.poc.api.telemetry.tls.TlsNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EPIC 9: TLS normalisation + family extraction APIs.
 */
@RestController
@RequestMapping("/api")
public class TlsFamilyController {

  private final TlsFamilyRepository repo;
  private final TlsFamilyBackfillService backfillService;

  /**
   * Simple PoC admin guard.
   *
   * If set (non-blank), the caller must provide X-Admin-Token with the same value.
   * This is intentionally lightweight and only used for EPIC 9.1.3 admin mutations.
   */
  private final String adminToken;

  public TlsFamilyController(
      TlsFamilyRepository repo,
      TlsFamilyBackfillService backfillService,
      @Value("${poc.admin.token:dev-admin}") String adminToken
  ) {
    this.repo = repo;
    this.backfillService = backfillService;
    this.adminToken = adminToken;
  }

  @GetMapping("/admin/tls-families")
  public ResponseEntity<List<TlsFamilyRepository.TlsFamilySummaryRow>> listFamilies(
      @RequestParam(name = "limit", defaultValue = "100") int limit
  ) {
    return ResponseEntity.ok(repo.listFamilies(limit));
  }

  @GetMapping("/admin/tls-families/lookup")
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

  @GetMapping("/showcase/tls-fp/family")
  public ResponseEntity<TlsFamilyShowcaseResponse> showcaseLookupByFp(
      @RequestParam("fp") String rawTlsFp,
      @RequestParam(name = "variants_limit", defaultValue = "10") int variantsLimit
  ) {
    Optional<TlsFamilyRepository.FamilyLookup> found = repo.findFamilyByRawFp(rawTlsFp);
    if (found.isEmpty()) {
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


  /**
   * EPIC 9.1.3: Admin-only mutation to force normalise & classify a TLS FP into a family.
   *
   * This is intentionally idempotent: calling it multiple times yields the same family.
   *
   * Note: To avoid polluting families, a valid TLS meta (subject/issuer) should be provided.
   */
  @PostMapping("/admin/tls-families/force-classify")
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
          java.time.OffsetDateTime.now()
      );
      repo.recomputeFamilyStats(n.familyId(), scores.confidence(), scores.stability());
    });

    // Reuse showcase lookup to return a stable DTO.
    return showcaseLookupByFp(rawTlsFp, variantsLimit);
  }


  /**
   * EPIC 9.1.4: Admin-triggered backfill of TLS families for historical TLS fingerprints.
   *
   * Safe & resumable:
   *  - Only processes TLS FPs not yet present in tls_family_member.
   *  - Batches are limited to prevent long-running requests.
   */
  @PostMapping("/admin/tls-families/backfill")
  public ResponseEntity<BackfillResponse> backfillTlsFamilies(
      @RequestParam(name = "batchSize", defaultValue = "500") int batchSize,
      @RequestParam(name = "maxBatches", defaultValue = "20") int maxBatches,
      @RequestHeader(name = "X-Admin-Token", required = false) String adminTokenHeader
  ) {
    requireAdmin(adminTokenHeader);
    var result = backfillService.backfill(batchSize, maxBatches);
    return ResponseEntity.ok(new BackfillResponse(
        result.processed(),
        result.classified(),
        result.batches(),
        result.complete(),
        result.lastFp()
    ));
  }

  private void requireAdmin(String headerToken) {
    String expected = (adminToken == null) ? "" : adminToken.trim();
    if (expected.isBlank()) {
      return; // admin guard disabled
    }
    String got = (headerToken == null) ? "" : headerToken.trim();
    if (!expected.equals(got)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "Admin token required"
      );
    }
  }

  // (intentionally no other admin mutations in this controller)

  public record TlsFamilyDetails(
      String familyId,
      String familyKey,
      String sampleTlsFp,
      long users,
      long seenCount,
      java.time.OffsetDateTime createdAt,
      java.time.OffsetDateTime firstSeen,
      java.time.OffsetDateTime lastSeen,
      long observationCount,
      int variantCount,
      Double confidence,
      Double stability,
      List<String> variants,
      Map<String, String> subject,
      Map<String, String> issuer
  ) {}

  /**
   * Showcase-safe TLS family lookup response.
   *
   * EPIC 9.1.1: The Showcase should never hard-fail when a TLS FP has not yet been clustered.
   * Instead of returning 404, we return 200 with notObserved=true.
   */
  public record TlsFamilyShowcaseResponse(
      String fp,
      boolean notObserved,
      String message,
      String familyId,
      String familyKey,
      String sampleTlsFp,
      long users,
      long seenCount,
      java.time.OffsetDateTime createdAt,
      java.time.OffsetDateTime lastSeen,
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

  public record BackfillResponse(
      long processed,
      long classified,
      int batches,
      boolean complete,
      String lastFp
  ) {}
}
