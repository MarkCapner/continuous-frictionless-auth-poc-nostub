package com.poc.api.showcase.controller;

import com.poc.api.showcase.dto.TrustSnapshot;
import com.poc.api.showcase.dto.TrustConsentRequest;
import com.poc.api.showcase.dto.TrustResetRequest;
import com.poc.api.showcase.dto.TrustUserSettings;
import com.poc.api.showcase.service.TrustSnapshotService;
import com.poc.api.showcase.persistence.TrustUserSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trust")
public class TrustController {

    private final TrustSnapshotService trustSnapshotService;
    private final TrustUserSettingsRepository trustUserSettingsRepository;

    public TrustController(TrustSnapshotService trustSnapshotService,
                           TrustUserSettingsRepository trustUserSettingsRepository) {
        this.trustSnapshotService = trustSnapshotService;
        this.trustUserSettingsRepository = trustUserSettingsRepository;
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<TrustSnapshot> sessionTrust(@PathVariable("sessionId") String sessionId) {
        return trustSnapshotService.buildForSession(sessionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    /**
     * EPIC 12.6: Consent hook (PoC). Allows a user to opt-in/out of trust signals.
     * This does not delete data; it stores a flag that downstream services can honor for minimisation.
     */
    @PostMapping("/consent")
    public ResponseEntity<TrustUserSettings> updateConsent(@RequestBody TrustConsentRequest req) {
        if (req == null || req.userId == null || req.userId.isBlank() || req.consentGranted == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(trustUserSettingsRepository.upsertConsent(req.userId.trim(), req.consentGranted));
    }

    /**
     * EPIC 12.6: Trust reset hook (PoC). Marks a baseline reset timestamp so "last time" comparisons
     * are made only after this point.
     */
    @PostMapping("/reset")
    public ResponseEntity<TrustUserSettings> resetTrust(@RequestBody TrustResetRequest req) {
        if (req == null || req.userId == null || req.userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String reason = (req.reason == null || req.reason.isBlank()) ? "User requested reset" : req.reason.trim();
        return ResponseEntity.ok(trustUserSettingsRepository.markBaselineReset(req.userId.trim(), reason));
    }

    @GetMapping("/settings/{userId}")
    public ResponseEntity<TrustUserSettings> settings(@PathVariable("userId") String userId) {
        return trustUserSettingsRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
