package com.poc.api.showcase.controller;

import com.poc.api.showcase.dto.TlsFamilyShowcaseResponse;
import com.poc.api.showcase.persistence.TlsFamilyRepository;
import com.poc.api.telemetry.tls.TlsMetaParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * EPIC 9: Showcase TLS family lookup API.
 */
@RestController
@RequestMapping("/api/showcase")
public class ShowcaseTlsFamilyController {

  private final TlsFamilyRepository repo;

  public ShowcaseTlsFamilyController(TlsFamilyRepository repo) {
    this.repo = repo;
  }

  @GetMapping("/tls-fp/family")
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
}
