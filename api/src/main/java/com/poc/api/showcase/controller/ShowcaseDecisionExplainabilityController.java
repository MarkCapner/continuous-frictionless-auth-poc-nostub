package com.poc.api.showcase.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EPIC X.8
 *
 * Opt-in explainability endpoint.
 * Not wired to persistence yet.
 */
@RestController
@RequestMapping("/api/showcase/decision")
public class ShowcaseDecisionExplainabilityController {

  @GetMapping("/{id}/explain")
  public Object explain(@PathVariable("id") String id) {
    // Wiring to persistence will be added once activated.
    return null;
  }
}
