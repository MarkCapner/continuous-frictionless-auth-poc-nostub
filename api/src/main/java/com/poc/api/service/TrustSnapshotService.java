package com.poc.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.api.model.TrustSnapshot;
import com.poc.api.persistence.SessionFeatureRepository;
import com.poc.api.persistence.SessionFeatureRow;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class TrustSnapshotService {

    private final SessionFeatureRepository sessionFeatureRepository;
    private final ObjectMapper objectMapper;
    private final TrustExplanationService trustExplanationService;

    public TrustSnapshotService(SessionFeatureRepository sessionFeatureRepository,
                               ObjectMapper objectMapper,
                               TrustExplanationService trustExplanationService) {
        this.sessionFeatureRepository = sessionFeatureRepository;
        this.objectMapper = objectMapper;
        this.trustExplanationService = trustExplanationService;
    }

    public Optional<TrustSnapshot> buildForSession(String sessionId) {
        Optional<SessionFeatureRow> rowOpt = sessionFeatureRepository.findByRequestId(sessionId);
        if (rowOpt.isEmpty()) return Optional.empty();

        SessionFeatureRow row = rowOpt.get();

        Map<String, Object> fv = safeParseJsonMap(row.featureVector);

        TrustExplanationService.Explanation exp = trustExplanationService.explain(row.decision, row.confidence, fv);

        TrustSnapshot snap = new TrustSnapshot();
        snap.sessionId = sessionId;
        snap.decision = row.decision;
        snap.confidence = row.confidence;

        // EPIC 12.2: deterministic, plain-language narrative + per-signal explanations.
        snap.riskSummary = exp.riskSummary();
        snap.signals = exp.signals();
        return Optional.of(snap);
    }

    private Map<String, Object> safeParseJsonMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }


    // JSON parsing only; explanation logic lives in TrustExplanationService.
}
