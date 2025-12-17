
package com.poc.api.analyst;

import java.util.*;

public class SessionInvestigationService {
    public Map<String, Object> buildEvidence(long sessionId) {
        return Map.of(
            "sessionId", sessionId,
            "summary", "Full analyst-grade evidence bundle"
        );
    }
}
