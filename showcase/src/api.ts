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

// -------------------------
// EPIC 9: TLS families
// -------------------------

export interface TlsFamilySummary {
  familyId: string;
  familyKey: string;
  sampleTlsFp: string | null;
  createdAt: string;
  lastSeen: string;
  seenCount: number;
  variants: number;
}

export interface TlsFamilyDetails {
  familyId: string;
  familyKey: string;
  sampleTlsFp: string | null;
  users: number;
  seenCount: number;
  createdAt: string;
  lastSeen: string;
  variants: string[];
  subject: Record<string, string>;
  issuer: Record<string, string>;
}

/**
 * Showcase-safe TLS family lookup response.
 *
 * EPIC 9.1.1: The showcase endpoint must never 404 when a TLS FP hasn't been clustered yet.
 */
export interface TlsFamilyShowcaseResponse {
  fp: string;
  notObserved: boolean;
  message: string | null;
  variants: string[];
  familyId: string | null;
  familyKey: string | null;
  sampleTlsFp: string | null;
  users: number | null;
  seenCount: number | null;
  createdAt: string | null;
  lastSeen: string | null;
  subject: Record<string, string>;
  issuer: Record<string, string>;
  confidence: number | null;
  stability: number | null;
}

export async function fetchTlsFamilyDetailsByFp(fp: string, variantsLimit: number = 10): Promise<TlsFamilyShowcaseResponse | null> {
  if (!fp || fp === "none") return null;
  const params = new URLSearchParams({ fp, variants_limit: String(variantsLimit) });
  const res = await fetch(`${API_BASE}/showcase/tls-fp/family?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch TLS family: ${res.status}`);
  }
  return (await res.json()) as TlsFamilyShowcaseResponse;
}

/**
 * EPIC 9.1.3: Admin-only force normalise & classify a TLS FP into a family.
 *
 * This is idempotent. For safety, tls_meta should include certificate subject/issuer.
 */
export async function forceClassifyTlsFamily(
  fp: string,
  tlsMeta: string | null,
  adminToken: string,
  variantsLimit: number = 12
): Promise<TlsFamilyShowcaseResponse> {
  const params = new URLSearchParams({ fp, variants_limit: String(variantsLimit) });
  if (tlsMeta && tlsMeta.trim().length > 0) {
    params.set("tls_meta", tlsMeta);
  }
  const res = await fetch(`${API_BASE}/admin/tls-families/force-classify?${params.toString()}`, {
    method: "POST",
    headers: {
      "X-Admin-Token": adminToken
    }
  });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(`Force classify failed: ${res.status}${txt ? ` - ${txt}` : ""}`);
  }
  return (await res.json()) as TlsFamilyShowcaseResponse;
}

export interface TlsFamilyBackfillResponse {
  processed: number;
  classified: number;
  batches: number;
  complete: boolean;
  lastFp: string | null;
}

/**
 * EPIC 9.1.4 / 9.1.5: Admin-triggered backfill of TLS families for historical TLS fingerprints.
 */
export async function backfillTlsFamilies(
  adminToken: string,
  batchSize: number = 500,
  maxBatches: number = 20
): Promise<TlsFamilyBackfillResponse> {
  const params = new URLSearchParams({ batchSize: String(batchSize), maxBatches: String(maxBatches) });
  const res = await fetch(`${API_BASE}/admin/tls-families/backfill?${params.toString()}`, {
    method: "POST",
    headers: {
      "X-Admin-Token": adminToken
    }
  });
  if (!res.ok) {
    const txt = await res.text().catch(() => "");
    throw new Error(`Backfill failed: ${res.status}${txt ? ` - ${txt}` : ""}`);
  }
  return (await res.json()) as TlsFamilyBackfillResponse;
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

export interface AdminUserSummary {
  userId: string;
  sessions: number;
  devices: number;
  lastSeen: string | null;
  avgConfidence: number;
  userTrustScore: number;
  userAccountSharingRisk: number;
}

export interface AdminUserReputation {
  trustScore: number;
  accountSharingRisk: number;
  deviceCount: number;
  tlsFingerprintCount: number;
  countryCount: number;
  avgConfidenceRecent: number;
  sessionsLast30d: number;
}

export interface AdminUserSharingInfo {
  suspicious: boolean;
  tlsFingerprintCount: number;
  countryCount: number;
}

export interface AdminUserDetail {
  userId: string;
  reputation: AdminUserReputation;
  sharing: AdminUserSharingInfo;
}

export async function fetchAdminUsers(limit = 50): Promise<AdminUserSummary[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  const res = await fetch(`${API_BASE}/admin/users?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch admin users: ${res.status}`);
  }
  return (await res.json()) as AdminUserSummary[];
}

export async function fetchAdminUserDetail(userId: string): Promise<AdminUserDetail> {
  const res = await fetch(`${API_BASE}/admin/users/${encodeURIComponent(userId)}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch user detail: ${res.status}`);
  }
  return (await res.json()) as AdminUserDetail;
}

export interface AdminRiskStatsRow {
  decision: string;
  total: number;
  avgConfidence: number;
  last24h: number;
  last7d: number;
}

export interface AdminSessionDailyStatsRow {
  day: string; // ISO date
  sessions: number;
  autoLogin: number;
  stepUp: number;
  deny: number;
  avgConfidence: number;
}

export interface AdminModelConfusionRow {
  decision: string;
  label: string | null;
  sessions: number;
}

export async function fetchRiskStats(): Promise<AdminRiskStatsRow[]> {
  const res = await fetch(`${API_BASE}/admin/analytics/risk`);
  if (!res.ok) {
    throw new Error(`Failed to fetch risk stats: ${res.status}`);
  }
  return (await res.json()) as AdminRiskStatsRow[];
}

export async function fetchSessionDailyStats(limit = 30): Promise<AdminSessionDailyStatsRow[]> {
  const params = new URLSearchParams({ limit: String(limit) });
  const res = await fetch(`${API_BASE}/admin/analytics/sessions/daily?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch session daily stats: ${res.status}`);
  }
  return (await res.json()) as AdminSessionDailyStatsRow[];
}

export async function fetchModelConfusion(): Promise<AdminModelConfusionRow[]> {
  const res = await fetch(`${API_BASE}/admin/analytics/model/confusion`);
  if (!res.ok) {
    throw new Error(`Failed to fetch model confusion: ${res.status}`);
  }
  return (await res.json()) as AdminModelConfusionRow[];
}

export interface DeviceProfileSummary {
  id: number;
  userId: string;
  tlsFp: string;
  uaFamily: string;
  uaVersion: string;
  screenW: number;
  screenH: number;
  pixelRatio: number;
  tzOffset: number;
  canvasHash: string | null;
  webglHash: string | null;
  firstSeen: string | null;
  lastSeen: string | null;
  seenCount: number;
  lastCountry: string | null;
}

export interface DeviceDiffChange {
  field: string;
  kind: string;
  leftValue: string | null;
  rightValue: string | null;
}

export interface DeviceDiffResponse {
  left: DeviceProfileSummary;
  right: DeviceProfileSummary;
  changes: DeviceDiffChange[];
}

export async function fetchDeviceHistory(userHint: string): Promise<DeviceProfileSummary[]> {
  const hint = (userHint || "demo-user").trim() || "demo-user";
  const params = new URLSearchParams({ user_hint: hint });
  const res = await fetch(`${API_BASE}/showcase/devices/history?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch device history: ${res.status}`);
  }
  return (await res.json()) as DeviceProfileSummary[];
}

export async function fetchDeviceDiff(leftId: number, rightId: number): Promise<DeviceDiffResponse> {
  const params = new URLSearchParams({
    left_id: String(leftId),
    right_id: String(rightId)
  });
  const res = await fetch(`${API_BASE}/showcase/devices/diff?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch device diff: ${res.status}`);
  }
  return (await res.json()) as DeviceDiffResponse;
}

export interface BehaviorHistoryItem {
  id: number;
  occurredAt: string;
  tlsFp: string;
  decision: string;
  confidence: number;
  behaviorJson: string | null;
  featureVector: string | null;
  label: string | null;
}

export async function fetchBehaviorHistory(userHint: string, limit = 50): Promise<BehaviorHistoryItem[]> {
  const hint = (userHint || "demo-user").trim() || "demo-user";
  const params = new URLSearchParams({ user_hint: hint, limit: String(limit) });
  const res = await fetch(`${API_BASE}/showcase/behavior/history?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch behaviour history: ${res.status}`);
  }
  return (await res.json()) as BehaviorHistoryItem[];
}


export interface RiskTimelineItem {
  id: number;
  occurredAt: string;
  decision: string;
  confidence: number;
  behaviorScore: number;
  deviceScore: number;
  contextScore: number;
  explanations: string[];
}

export async function fetchRiskTimeline(
  userHint: string,
  limit = 50
): Promise<RiskTimelineItem[]> {
  const hint = (userHint || "demo-user").trim() || "demo-user";
  const params = new URLSearchParams({ user_hint: hint, limit: String(limit) });
  const res = await fetch(`${API_BASE}/showcase/risk/timeline?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch risk timeline: ${res.status}`);
  }
  return (await res.json()) as RiskTimelineItem[];
}

export interface TlsFpDeviceRow {
  id: number;
  userId: string;
  uaFamily: string;
  uaVersion: string;
  lastCountry: string | null;
  firstSeen: string | null;
  lastSeen: string | null;
}

export interface TlsFpOverview {
  tlsFp: string;
  profiles: number;
  users: number;
  firstSeen: string | null;
  lastSeen: string | null;
  devices: TlsFpDeviceRow[];
}

export async function fetchTlsFpOverview(tlsFp: string): Promise<TlsFpOverview> {
  const params = new URLSearchParams({ tls_fp: tlsFp });
  const res = await fetch(`${API_BASE}/showcase/tls-fp/overview?${params.toString()}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch TLS FP overview: ${res.status}`);
  }
  return (await res.json()) as TlsFpOverview;
}
