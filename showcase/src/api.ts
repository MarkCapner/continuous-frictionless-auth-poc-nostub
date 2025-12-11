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
  key_interval_std_ms: number;
  scroll_events_per_sec: number;
  pointer_avg_velocity: number;
  pointer_max_velocity: number;
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
    ml_anomaly_score?: number;
    // additional dimensions from EPIC 5 are allowed but not explicitly typed here
    [key: string]: number | undefined;
  };
  explanations: string[];
  session_id: string;
  tls_fp?: string;
  tls_meta?: string;
  model_version?: string;
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

export interface TlsFingerprintStats {
  tlsFp: string;
  profiles: number;
  users: number;
  firstSeen: string;
  lastSeen: string;
}

export async function fetchTlsFingerprintStats(fp: string): Promise<TlsFingerprintStats | null> {
  if (!fp || fp === "none") {
    return null;
  }
  const params = new URLSearchParams({ fp });
  const res = await fetch(`${API_BASE}/showcase/tls-fp?${params.toString()}`);
  if (res.status === 404) {
    return null;
  }
  if (!res.ok) {
    throw new Error(`Failed to fetch TLS stats: ${res.status}`);
  }
  return (await res.json()) as TlsFingerprintStats;
}


export type TlsFingerprintSummary = TlsFingerprintStats;

export async function fetchAllTlsFingerprintSummaries(limit = 100): Promise<TlsFingerprintSummary[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  const res = await fetch(`${API_BASE}/admin/tls-fps?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch TLS fingerprints: ${res.status}`);
  }
  return (await res.json()) as TlsFingerprintSummary[];
}


export interface BehaviorBaseline {
  userId: string;
  feature: string;
  mean: number;
  stdDev: number;
  variance: number;
  decay: number;
  updatedAt: string;
}

export async function fetchBehaviorBaselines(limit: number = 200): Promise<BehaviorBaseline[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  const res = await fetch(`${API_BASE}/admin/behavior/baselines?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch behavior baselines: ${res.status}`);
  }
  return (await res.json()) as BehaviorBaseline[];
}
export interface ModelInfo {
  ready: boolean;
  modelVersion: string;
  registryName: string | null;
  registryFormat: string | null;
  registryVersion: string | null;
  lastTrainedAt: string | null;
}

export async function fetchModelInfo(): Promise<ModelInfo> {
  const res = await fetch(`${API_BASE}/admin/model`);
  if (!res.ok) {
    throw new Error(`Failed to load model info: ${res.status}`);
  }
  return (await res.json()) as ModelInfo;
}

export async function retrainModel(limit: number = 500): Promise<ModelInfo> {
  const params = new URLSearchParams({ limit: String(limit) });
  const res = await fetch(`${API_BASE}/admin/model/retrain?${params.toString()}`, {
    method: "POST"
  });
  if (!res.ok) {
    throw new Error(`Failed to retrain model: ${res.status}`);
  }
  return (await res.json()) as ModelInfo;
}
