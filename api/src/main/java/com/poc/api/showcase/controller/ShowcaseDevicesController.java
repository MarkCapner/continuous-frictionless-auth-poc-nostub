package com.poc.api.showcase.controller;

import com.poc.api.telemetry.persistence.DeviceProfile;
import com.poc.api.telemetry.persistence.DeviceProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping({"/api/showcase/devices","/api/v1/showcase/devices"})
public class ShowcaseDevicesController {

  private final DeviceProfileRepository deviceProfileRepository;

  public ShowcaseDevicesController(DeviceProfileRepository deviceProfileRepository) {
    this.deviceProfileRepository = deviceProfileRepository;
  }

  public record DeviceSummary(
      Long id,
      String userId,
      String tlsFp,
      String uaFamily,
      String uaVersion,
      int screenW,
      int screenH,
      double pixelRatio,
      short tzOffset,
      String canvasHash,
      String webglHash,
      OffsetDateTime firstSeen,
      OffsetDateTime lastSeen,
      long seenCount,
      String lastCountry
  ) {}

  public record DeviceDiffChange(
      String field,
      String kind,
      String leftValue,
      String rightValue
  ) {}

  public record DeviceDiffResponse(
      DeviceSummary left,
      DeviceSummary right,
      List<DeviceDiffChange> changes
  ) {}

  @GetMapping("/history")
  public ResponseEntity<List<DeviceSummary>> deviceHistory(@RequestParam("user_hint") String userHint) {
    var profiles = deviceProfileRepository.findByUser(userHint);
    List<DeviceSummary> result = profiles.stream().map(this::toDeviceSummary).toList();
    return ResponseEntity.ok(result);
  }

  @GetMapping("/diff")
  public ResponseEntity<DeviceDiffResponse> deviceDiff(
      @RequestParam("left_id") long leftId,
      @RequestParam("right_id") long rightId
  ) {
    var leftOpt = deviceProfileRepository.findById(leftId);
    var rightOpt = deviceProfileRepository.findById(rightId);
    if (leftOpt.isEmpty() || rightOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    var left = leftOpt.get();
    var right = rightOpt.get();
    var changes = computeDeviceDiff(left, right);
    return ResponseEntity.ok(new DeviceDiffResponse(
        toDeviceSummary(left),
        toDeviceSummary(right),
        changes
    ));
  }

  private DeviceSummary toDeviceSummary(DeviceProfile p) {
    return new DeviceSummary(
        p.id,
        p.userId,
        p.tlsFp,
        p.uaFamily,
        p.uaVersion,
        p.screenW,
        p.screenH,
        p.pixelRatio,
        p.tzOffset,
        p.canvasHash,
        p.webglHash,
        p.firstSeen,
        p.lastSeen,
        p.seenCount,
        p.lastCountry
    );
  }

  private List<DeviceDiffChange> computeDeviceDiff(DeviceProfile left, DeviceProfile right) {
    List<DeviceDiffChange> changes = new ArrayList<>();
    addDiff(changes, "uaFamily", left.uaFamily, right.uaFamily);
    addDiff(changes, "uaVersion", left.uaVersion, right.uaVersion);
    addDiff(changes, "screenW", String.valueOf(left.screenW), String.valueOf(right.screenW));
    addDiff(changes, "screenH", String.valueOf(left.screenH), String.valueOf(right.screenH));
    addDiff(changes, "pixelRatio", String.valueOf(left.pixelRatio), String.valueOf(right.pixelRatio));
    addDiff(changes, "tzOffset", String.valueOf(left.tzOffset), String.valueOf(right.tzOffset));
    addDiff(changes, "canvasHash", left.canvasHash, right.canvasHash);
    addDiff(changes, "webglHash", left.webglHash, right.webglHash);
    addDiff(changes, "lastCountry", left.lastCountry, right.lastCountry);
    return changes;
  }

  private void addDiff(List<DeviceDiffChange> changes, String field, String left, String right) {
    if (!Objects.equals(left, right)) {
      changes.add(new DeviceDiffChange(field, "CHANGED", left != null ? left : "", right != null ? right : ""));
    }
  }
}
