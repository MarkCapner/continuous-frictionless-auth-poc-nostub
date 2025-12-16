package com.poc.api.risk.explainability;

public class SessionExplainability {
  private final String sessionId;
  private final String contributionsJson;

  public SessionExplainability(String sessionId, String contributionsJson) {
    this.sessionId = sessionId;
    this.contributionsJson = contributionsJson;
  }

  public String getSessionId() { return sessionId; }
  public String getContributionsJson() { return contributionsJson; }
}
