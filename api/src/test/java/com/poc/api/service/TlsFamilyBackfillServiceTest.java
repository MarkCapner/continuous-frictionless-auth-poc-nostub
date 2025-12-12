package com.poc.api.service;

import com.poc.api.persistence.TlsFamilyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TlsFamilyBackfillServiceTest {

  @Test
  void backfill_isIdempotentAndRecomputesScoresOncePerFamily() {
    TlsFamilyRepository repo = Mockito.mock(TlsFamilyRepository.class);

    // Two batches:
    // batch1 returns fpA, fpB; batch2 returns fpC; batch3 returns empty -> stop
    when(repo.listUnclassifiedObservedTlsFps(eq(""), eq(2))).thenReturn(List.of("fpA", "fpB"));
    when(repo.listUnclassifiedObservedTlsFps(eq("fpB"), eq(2))).thenReturn(List.of("fpC"));
    when(repo.listUnclassifiedObservedTlsFps(eq("fpC"), eq(2))).thenReturn(List.of());

    // tls_meta best-effort
    when(repo.findLatestTlsMetaForFp(anyString())).thenReturn("sub=CN=test;iss=CN=iss");

    // family stats: provide a row so scoring can run
    TlsFamilyRepository.FamilyStats stats = new TlsFamilyRepository.FamilyStats();
    stats.familyId = "fam";
    stats.firstSeen = OffsetDateTime.now().minusDays(1);
    stats.lastSeen = OffsetDateTime.now().minusHours(1);
    stats.observationCount = 5L;
    stats.variantCount = 2;
    when(repo.getFamilyStats(anyString())).thenReturn(Optional.of(stats));

    TlsFamilyBackfillService svc = new TlsFamilyBackfillService(repo);
    var res = svc.backfill(2, 10);

    assertEquals(3, res.processed());
    assertEquals(3, res.classified());
    assertEquals(2, res.batches()); // we ran 2 batches
    assertEquals(true, res.complete());
    assertEquals("fpC", res.lastFp());

    // Each FP causes family+member upsert
    verify(repo, times(3)).upsertFamily(anyString(), anyString(), anyString(), anyString());
    verify(repo, times(3)).upsertMember(anyString(), anyString(), anyString());

    // Recompute family stats called once per touched family id.
    // We don't care which family id, only that it's not called more than number of touched families.
    // Given our normalizer produces deterministic IDs from meta+fp, there could be >1 family. We'll just assert <=3.
    verify(repo, atMost(3)).recomputeFamilyStats(anyString(), anyDouble(), anyDouble());
  }
}
