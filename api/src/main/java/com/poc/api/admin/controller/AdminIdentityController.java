package com.poc.api.admin.controller;

import com.poc.api.telemetry.persistence.DeviceProfileRepository;
import com.poc.api.identity.persistence.IdentityNodeType;
import com.poc.api.identity.service.IdentityGraphService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/identity")
public class AdminIdentityController {

  private final IdentityGraphService identityGraph;
  private final DeviceProfileRepository deviceProfiles;
  private final String adminToken;

  public AdminIdentityController(
      IdentityGraphService identityGraph,
      DeviceProfileRepository deviceProfiles,
      @Value("${poc.admin.token:dev-admin}") String adminToken
  ) {
    this.identityGraph = identityGraph;
    this.deviceProfiles = deviceProfiles;
    this.adminToken = adminToken;
  }

  @PostMapping("/rebuild")
  public ResponseEntity<RebuildResponse> rebuild(
      @RequestHeader(name = "X-Admin-Token", required = false) String adminTokenHeader,
      @RequestParam(name = "after_id", defaultValue = "0") long afterId,
      @RequestParam(name = "batchSize", defaultValue = "500") int batchSize,
      @RequestParam(name = "maxBatches", defaultValue = "20") int maxBatches
  ) {
    requireAdmin(adminTokenHeader);
    var r = identityGraph.rebuildFromDeviceProfiles(deviceProfiles, afterId, batchSize, maxBatches);
    return ResponseEntity.ok(new RebuildResponse(r.processed(), r.batches(), r.complete(), r.lastId()));
  }

  @GetMapping("/cluster")
  public ResponseEntity<?> cluster(
      @RequestHeader(name = "X-Admin-Token", required = false) String adminTokenHeader,
      @RequestParam("type") IdentityNodeType type,
      @RequestParam("key") String key,
      @RequestParam(name = "depth", defaultValue = "2") int depth,
      @RequestParam(name = "limitPerNode", defaultValue = "200") int limitPerNode
  ) {
    requireAdmin(adminTokenHeader);
    var c = identityGraph.cluster(type, key, depth, limitPerNode);
    if (c.root() == null) return ResponseEntity.status(404).body(c);
    return ResponseEntity.ok(c);
  }

  private void requireAdmin(String headerToken) {
    String expected = (adminToken == null) ? "" : adminToken.trim();
    if (expected.isBlank()) return;
    String got = (headerToken == null) ? "" : headerToken.trim();
    if (!expected.equals(got)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "Admin token required"
      );
    }
  }

  public record RebuildResponse(long processed, int batches, boolean complete, long lastId) {}
}
