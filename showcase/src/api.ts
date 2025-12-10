export interface DeviceTelemetry {
  ua: string;
  ua_ch?: any;
  platform?: string;
  cores?: number;
  memory_gb?: number;
  screen: { w: number; h: number; pixel_ratio: number };
  tz_offset: number;
  langs: string[];
  canvas_hash?: string;
  webgl_hash?: string;
}

export interface BehaviorTelemetry {
  mouse_moves: number;
  mouse_distance: number;
  key_presses: number;
  avg_key_interval_ms: number;
  scroll_events: number;
}

export interface TelemetryPayload {
  user_id_hint?: string;
  device: DeviceTelemetry;
  behavior: BehaviorTelemetry;
  context?: Record<string, unknown>;
}

export interface DecisionResponse {
  decision: "AUTO_LOGIN" | "STEP_UP" | "DENY" | string;
  confidence: number;
  breakdown: {
    device_score: number;
    tls_score: number;
    behavior_score: number;
    context_score: number;
  };
  explanations: string[];
  session_id: string;
}

const API_BASE = "/api";

export async function postProfileCheck(payload: TelemetryPayload): Promise<DecisionResponse> {
  const res = await fetch(`${API_BASE}/auth/profile-check`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });
  if (!res.ok) {
    throw new Error(`API error: ${res.status}`);
  }
  return (await res.json()) as DecisionResponse;
}
