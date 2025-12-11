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
  tls_fp?: string;
  tls_meta?: string;
}

export interface SessionSummary {
  id: number;
  sessionId: string;
  userId: string | null;
  tlsFp: string;
  behaviorScore: number;
  deviceScore: number;
  contextScore: number;
  confidence: number;
  decision: string;
  createdAt: string;
}

//const API_BASE = "/api";
const API_BASE = "https://localhost:8443/api";


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

export async function fetchSessions(userHint: string, limit = 20): Promise<SessionSummary[]> {
  const params = new URLSearchParams({ user_hint: userHint, limit: String(limit) });
  const res = await fetch(`${API_BASE}/showcase/sessions?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch sessions: ${res.status}`);
  }
  return (await res.json()) as SessionSummary[];
}


export interface UserSummary {
  userId: string;
  sessions: number;
  devices: number;
  lastSeen: string;
  avgConfidence: number;
}

export async function fetchUserSummaries(limit = 20): Promise<UserSummary[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  const res = await fetch(`${API_BASE}/showcase/users?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch users: ${res.status}`);
  }
  return (await res.json()) as UserSummary[];
}
