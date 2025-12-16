package com.poc.api.risk.aggregate;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.poc.api.risk.model.RiskSignal;

public class RiskAggregationEngineTest {

  @Test
  void aggregatesSignalsDeterministically() {
    RiskAggregationEngine engine = new RiskAggregationEngine();

    List<RiskSignal> signals = List.of(
        new RiskSignal("behavior.score", 0.5, 1.0, "behavior"),
        new RiskSignal("device.score", -0.2, 1.0, "device")
    );

    var result = engine.evaluate(signals);

    assertEquals(0.3, result.finalScore(), 1e-6);
    assertEquals("ALLOW", result.decision());
    assertTrue(result.confidence() > 0);
    assertEquals(2, result.contributions().size());
  }

  @Test
  void negativeScoreProducesDeny() {
    RiskAggregationEngine engine = new RiskAggregationEngine();

    List<RiskSignal> signals = List.of(
        new RiskSignal("tls.rarity", -0.8, 1.0, "tls")
    );

    var result = engine.evaluate(signals);

    assertEquals("DENY", result.decision());
  }
}
