package com.poc.api.controller;

import com.poc.api.persistence.TlsFamilyRepository;
import com.poc.api.tls.TlsMetaParser;
import org.springframework.http.ResponseEntity;
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

  public TlsFamilyController(TlsFamilyRepository repo) {
    this.repo = repo;
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
        f.lastSeen,
        variants,
        subAttrs,
        issAttrs
    ));
  }

  @GetMapping("/showcase/tls-fp/family")
  public ResponseEntity<TlsFamilyDetails> showcaseLookupByFp(
      @RequestParam("fp") String rawTlsFp,
      @RequestParam(name = "variants_limit", defaultValue = "10") int variantsLimit
  ) {
    return lookupByFp(rawTlsFp, variantsLimit);
  }

  public record TlsFamilyDetails(
      String familyId,
      String familyKey,
      String sampleTlsFp,
      long users,
      long seenCount,
      java.time.OffsetDateTime createdAt,
      java.time.OffsetDateTime lastSeen,
      List<String> variants,
      Map<String, String> subject,
      Map<String, String> issuer
  ) {}
}
