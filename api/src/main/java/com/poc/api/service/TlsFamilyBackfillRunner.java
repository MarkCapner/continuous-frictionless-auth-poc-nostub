package com.poc.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * EPIC 9.1.4: Optional startup backfill.
 *
 * Disabled by default. Enable via:
 *   poc.tls.backfill.startup.enabled=true
 */
@Component
public class TlsFamilyBackfillRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(TlsFamilyBackfillRunner.class);

  private final TlsFamilyBackfillService service;

  @Value("${poc.tls.backfill.startup.enabled:false}")
  private boolean enabled;

  @Value("${poc.tls.backfill.batchSize:500}")
  private int batchSize;

  @Value("${poc.tls.backfill.maxBatches:20}")
  private int maxBatches;

  public TlsFamilyBackfillRunner(TlsFamilyBackfillService service) {
    this.service = service;
  }

  @Override
  public void run(String... args) {
    if (!enabled) {
      log.info("[tls-backfill] startup backfill disabled (poc.tls.backfill.startup.enabled=false)");
      return;
    }
    log.info("[tls-backfill] startup backfill starting (batchSize={}, maxBatches={})", batchSize, maxBatches);
    var result = service.backfill(batchSize, maxBatches);
    log.info("[tls-backfill] startup backfill finished: processed={}, classified={}, batches={}, complete={}, lastFp='{}'",
        result.processed(), result.classified(), result.batches(), result.complete(), result.lastFp());
  }
}
