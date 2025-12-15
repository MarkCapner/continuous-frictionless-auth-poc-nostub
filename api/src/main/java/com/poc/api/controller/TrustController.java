package com.poc.api.controller;

import com.poc.api.model.TrustSnapshot;
import com.poc.api.service.TrustSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trust")
public class TrustController {

    private final TrustSnapshotService trustSnapshotService;

    public TrustController(TrustSnapshotService trustSnapshotService) {
        this.trustSnapshotService = trustSnapshotService;
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<TrustSnapshot> sessionTrust(@PathVariable("sessionId") String sessionId) {
        return trustSnapshotService.buildForSession(sessionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
