package com.poc.api.showcase.controller;

import com.poc.api.showcase.dto.TrustSnapshot;
import com.poc.api.showcase.dto.TrustConsentRequest;
import com.poc.api.showcase.dto.TrustResetRequest;
import com.poc.api.showcase.dto.TrustUserSettings;
import com.poc.api.showcase.service.TrustSnapshotService;
import com.poc.api.showcase.persistence.TrustUserSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trust")
public class ShowcaseTrustController {

    private final TrustSnapshotService trustSnapshotService;
    private final TrustUserSettingsRepository trustUserSettingsRepository;

    public ShowcaseTrustController(TrustSnapshotService trustSnapshotService,
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

    @PostMapping("/consent")
    public ResponseEntity<TrustUserSettings> updateConsent(@RequestBody TrustConsentRequest req) {
        if (req == null || req.userId == null || req.userId.isBlank() || req.consentGranted == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(trustUserSettingsRepository.upsertConsent(req.userId.trim(), req.consentGranted));
    }

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
