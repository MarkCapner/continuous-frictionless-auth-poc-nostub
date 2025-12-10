package com.poc.api.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record Telemetry(
    String user_id_hint,
    @NotNull Device device,
    @NotNull Behavior behavior,
    Map<String, Object> context
) {

  public record Device(
      @NotNull String ua,
      Map<String, Object> ua_ch,
      String platform,
      Integer cores,
      Double memory_gb,
      Screen screen,
      Integer tz_offset,
      List<String> langs,
      String canvas_hash,
      String webgl_hash
  ) {
    public record Screen(Integer w, Integer h, Double pixel_ratio) {}
  }

  public record Behavior(
      Integer mouse_moves,
      Double mouse_distance,
      Integer key_presses,
      Double avg_key_interval_ms,
      Integer scroll_events
  ) {}
}
