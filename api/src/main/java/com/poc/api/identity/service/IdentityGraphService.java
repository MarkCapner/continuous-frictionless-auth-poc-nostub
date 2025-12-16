package com.poc.api.identity.service;

import com.poc.api.identity.persistence.IdentityGraphRepository;
import com.poc.api.identity.persistence.IdentityLinkRow;
import com.poc.api.identity.persistence.IdentityLinkType;
import com.poc.api.identity.persistence.IdentityNodeRow;
import com.poc.api.identity.persistence.IdentityNodeType;
import com.poc.api.showcase.persistence.TlsFamilyRepository;
import com.poc.api.telemetry.persistence.DeviceProfile;
import com.poc.api.telemetry.persistence.DeviceProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * EPIC 10.2
 * Best-effort identity graph population from existing signals.
 *
 * This service is intentionally non-blocking: callers should treat failures as non-fatal.
 */
@Service
public class IdentityGraphService {

  private static final Logger log = LoggerFactory.getLogger(IdentityGraphService.class);

  private final IdentityGraphRepository graphRepo;
  private final TlsFamilyRepository tlsFamilyRepo;

  public IdentityGraphService(IdentityGraphRepository graphRepo, TlsFamilyRepository tlsFamilyRepo) {
    this.graphRepo = graphRepo;
    this.tlsFamilyRepo = tlsFamilyRepo;
  }

  /**
   * Observe a device profile write and update identity graph nodes/links.
   */
  public void observeDeviceProfile(DeviceProfile dp) {
    if (dp == null) return;
    try {

      // USER node
      IdentityNodeRow user = graphRepo.upsertNode(
          IdentityNodeType.USER,
          safe(dp.userId),
          safe(dp.userId),
          null
      );

      // DEVICE node (use device_profile.id as stable natural key for this PoC)
      String deviceKey = "device_profile:" + dp.id;
      String deviceLabel = (dp.uaFamily != null ? dp.uaFamily : "device") + " • " + dp.screenW + "x" + dp.screenH;
      String deviceMeta = buildJsonMeta(Map.of(
          "deviceProfileId", String.valueOf(dp.id),
          "tlsFp", safe(dp.tlsFp),
          "uaFamily", safe(dp.uaFamily),
          "uaVersion", safe(dp.uaVersion),
          "canvasHash", safe(dp.canvasHash),
          "webglHash", safe(dp.webglHash),
          "tzOffset", String.valueOf(dp.tzOffset)
      ));
      IdentityNodeRow device = graphRepo.upsertNode(
          IdentityNodeType.DEVICE,
          deviceKey,
          deviceLabel,
          deviceMeta
      );

      // USER <-> DEVICE link (hard, confidence=1 for PoC)
      graphRepo.upsertLink(
          user.id,
          device.id,
          IdentityLinkType.USER_DEVICE,
          1.0,
          "device_profile observation",
          buildJsonMeta(Map.of("source", "device_profile", "deviceProfileId", String.valueOf(dp.id))));

      // BEHAVIOR_CLUSTER node (placeholder per-user cluster key for now)
      String behKey = "behavior_cluster:user:" + safe(dp.userId);
      IdentityNodeRow beh = graphRepo.upsertNode(
          IdentityNodeType.BEHAVIOR_CLUSTER,
          behKey,
          "Behavior cluster • " + safe(dp.userId),
          buildJsonMeta(Map.of("userId", safe(dp.userId), "kind", "per-user-baseline"))
      );

      graphRepo.upsertLink(
          user.id,
          beh.id,
          IdentityLinkType.USER_BEHAVIOR_CLUSTER,
          0.85,
          "per-user behavior baseline cluster",
          buildJsonMeta(Map.of("source", "behavior_baseline")));

      graphRepo.upsertLink(
          device.id,
          beh.id,
          IdentityLinkType.DEVICE_BEHAVIOR_CLUSTER,
          0.65,
          "device observed under user baseline",
          buildJsonMeta(Map.of("source", "device_profile")));

      // DEVICE / USER <-> TLS_FAMILY if we can map the raw FP
      if (dp.tlsFp != null && !dp.tlsFp.isBlank()) {
        Optional<TlsFamilyRepository.FamilyLookup> famOpt = tlsFamilyRepo.findFamilyByRawFp(dp.tlsFp);
        if (famOpt.isPresent()) {
          var fam = famOpt.get();
          IdentityNodeRow famNode = graphRepo.upsertNode(
              IdentityNodeType.TLS_FAMILY,
              fam.familyId,
              "TLS Family • " + safe(fam.familyKey),
              buildJsonMeta(Map.of(
                  "familyId", fam.familyId,
                  "familyKey", safe(fam.familyKey),
                  "sampleTlsFp", safe(fam.sampleTlsFp)
              ))
          );

          double base = (fam.stability != null) ? fam.stability : (fam.confidence != null ? fam.confidence : 0.6);
          double deviceConf = clamp01(0.55 + 0.45 * base);
          double userConf = clamp01(deviceConf + 0.10);

          graphRepo.upsertLink(
              device.id,
              famNode.id,
              IdentityLinkType.DEVICE_TLS_FAMILY,
              deviceConf,
              "device tls_fp maps to tls_family",
              buildJsonMeta(Map.of("rawFp", safe(dp.tlsFp), "familyId", fam.familyId)));

          graphRepo.upsertLink(
              user.id,
              famNode.id,
              IdentityLinkType.USER_TLS_FAMILY,
              userConf,
              "user observed with tls_family",
              buildJsonMeta(Map.of("rawFp", safe(dp.tlsFp), "familyId", fam.familyId)));

          graphRepo.upsertLink(
              famNode.id,
              beh.id,
              IdentityLinkType.TLS_FAMILY_BEHAVIOR_CLUSTER,
              clamp01(0.40 + 0.40 * base),
              "tls_family seen with user behavior cluster",
              buildJsonMeta(Map.of("userId", safe(dp.userId))));
        }
      }

    } catch (Exception e) {
      log.warn("[identity] best-effort observeDeviceProfile failed: {}", e.getMessage());
    }
  }

  public RebuildResult rebuildFromDeviceProfiles(DeviceProfileRepository dpRepo, long afterId, int batchSize, int maxBatches) {
    long cursor = afterId;
    long processed = 0;
    int batches = 0;

    for (int i = 0; i < maxBatches; i++) {
      List<DeviceProfile> rows = dpRepo.listAfterId(cursor, batchSize);
      if (rows.isEmpty()) break;
      for (DeviceProfile dp : rows) {
        observeDeviceProfile(dp);
        processed++;
        if (dp.id != null) cursor = Math.max(cursor, dp.id);
      }
      batches++;
    }

    boolean complete = batches < maxBatches;
    return new RebuildResult(processed, batches, complete, cursor);
  }

  public ClusterResult cluster(IdentityNodeType type, String key, int depth, int limitPerNode) {
    Optional<IdentityNodeRow> rootOpt = graphRepo.findNode(type, key);
    if (rootOpt.isEmpty()) {
      return ClusterResult.notFound(type, key);
    }
    IdentityNodeRow root = rootOpt.get();

    Map<Long, IdentityNodeRow> nodes = new LinkedHashMap<>();
    Map<Long, IdentityLinkRow> links = new LinkedHashMap<>();

    ArrayDeque<Long> q = new ArrayDeque<>();
    Map<Long, Integer> dist = new HashMap<>();

    nodes.put(root.id, root);
    q.add(root.id);
    dist.put(root.id, 0);

    while (!q.isEmpty()) {
      long cur = q.removeFirst();
      int d = dist.getOrDefault(cur, 0);
      if (d >= depth) continue;

      List<IdentityLinkRow> lks = graphRepo.listLinksForNode(cur, limitPerNode);
      for (IdentityLinkRow lk : lks) {
        links.put(lk.id, lk);
        long other = (lk.fromNodeId == cur) ? lk.toNodeId : lk.fromNodeId;
        if (!nodes.containsKey(other)) {
          graphRepo.findNodeById(other).ifPresent(n -> nodes.put(other, n));
        }
        if (!dist.containsKey(other)) {
          dist.put(other, d + 1);
          q.addLast(other);
        }
      }
    }

    return new ClusterResult(root, new ArrayList<>(nodes.values()), new ArrayList<>(links.values()), null);
  }

  public record RebuildResult(long processed, int batches, boolean complete, long lastId) {}

  public record ClusterResult(IdentityNodeRow root, List<IdentityNodeRow> nodes, List<IdentityLinkRow> links, String message) {
    public static ClusterResult notFound(IdentityNodeType type, String key) {
      return new ClusterResult(null, List.of(), List.of(), "Root node not found: " + type + ":" + key);
    }
  }

  private static String safe(String s) { return s == null ? "" : s; }

  private static double clamp01(double v) { return (v < 0) ? 0 : Math.min(v, 1); }

  private static String buildJsonMeta(Map<String, String> map) {
    if (map == null || map.isEmpty()) return "{}";
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean first = true;
    for (var e : map.entrySet()) {
      if (!first) sb.append(",");
      first = false;
      sb.append("\"").append(escape(e.getKey())).append("\":");
      sb.append("\"").append(escape(e.getValue())).append("\"");
    }
    sb.append("}");
    return sb.toString();
  }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
