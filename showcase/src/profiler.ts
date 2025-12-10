import type { TelemetryPayload, DeviceTelemetry, BehaviorTelemetry } from "./api";

let behavior: BehaviorTelemetry = {
  mouse_moves: 0,
  mouse_distance: 0,
  key_presses: 0,
  avg_key_interval_ms: 0,
  scroll_events: 0
};

let lastMouseX = 0;
let lastMouseY = 0;
let lastKeyTime: number | null = null;
let keyIntervals: number[] = [];

type ChaosState = {
  vpn: boolean;
  highRiskAction: boolean;
  nightTime: boolean;
};

let chaosState: ChaosState = {
  vpn: false,
  highRiskAction: false,
  nightTime: false
};

export function setChaosVpn(enabled: boolean) {
  chaosState.vpn = enabled;
}

export function setChaosHighRiskAction(enabled: boolean) {
  chaosState.highRiskAction = enabled;
}

export function setChaosNightTime(enabled: boolean) {
  chaosState.nightTime = enabled;
}

let canvasHash: string | undefined;
let webglHash: string | undefined;

export function startProfiler() {
  window.addEventListener("mousemove", onMouseMove);
  window.addEventListener("keydown", onKeyDown);
  window.addEventListener("scroll", onScroll);
  computeCanvasHash();
  computeWebglHash();
}

function onMouseMove(e: MouseEvent) {
  behavior.mouse_moves += 1;
  if (lastMouseX !== 0 || lastMouseY !== 0) {
    const dx = e.clientX - lastMouseX;
    const dy = e.clientY - lastMouseY;
    behavior.mouse_distance += Math.sqrt(dx * dx + dy * dy);
  }
  lastMouseX = e.clientX;
  lastMouseY = e.clientY;
}

function onKeyDown() {
  const now = performance.now();
  behavior.key_presses += 1;
  if (lastKeyTime != null) {
    keyIntervals.push(now - lastKeyTime);
    const sum = keyIntervals.reduce((a, b) => a + b, 0);
    behavior.avg_key_interval_ms = sum / keyIntervals.length;
  }
  lastKeyTime = now;
}

function onScroll() {
  behavior.scroll_events += 1;
}

async function sha256Hex(str: string): Promise<string> {
  const enc = new TextEncoder().encode(str);
  const digest = await crypto.subtle.digest("SHA-256", enc);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

async function computeCanvasHash() {
  try {
    const canvas = document.createElement("canvas");
    canvas.width = 200;
    canvas.height = 50;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.textBaseline = "top";
    ctx.font = "16px 'Arial'";
    ctx.fillStyle = "#f60";
    ctx.fillRect(0, 0, 200, 50);
    ctx.fillStyle = "#069";
    ctx.fillText("Frictionless Auth PoC", 2, 2);
    const data = canvas.toDataURL();
    canvasHash = await sha256Hex(data);
  } catch {
    canvasHash = undefined;
  }
}

async function computeWebglHash() {
  try {
    const canvas = document.createElement("canvas");
    const gl = canvas.getContext("webgl") || canvas.getContext("experimental-webgl");
    if (!gl) return;
    const debugInfo = (gl as any).getExtension("WEBGL_debug_renderer_info");
    let vendor = "";
    let renderer = "";
    if (debugInfo) {
      vendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);
      renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);
    } else {
      vendor = gl.getParameter(gl.VENDOR);
      renderer = gl.getParameter(gl.RENDERER);
    }
    const info = `${vendor}|${renderer}|${gl.getParameter(gl.VERSION)}|${gl.getParameter(gl.SHADING_LANGUAGE_VERSION)}`;
    webglHash = await sha256Hex(info);
  } catch {
    webglHash = undefined;
  }
}

export function snapshotTelemetry(userIdHint?: string): TelemetryPayload {
  const nav = window.navigator as any;

  const device: DeviceTelemetry = {
    ua: navigator.userAgent,
    ua_ch: nav.userAgentData ?? undefined,
    platform: navigator.platform,
    cores: nav.hardwareConcurrency ?? undefined,
    memory_gb: nav.deviceMemory ?? undefined,
    screen: {
      w: window.screen.width,
      h: window.screen.height,
      pixel_ratio: window.devicePixelRatio ?? 1
    },
    tz_offset: new Date().getTimezoneOffset(),
    langs: navigator.languages ?? [navigator.language],
    canvas_hash: canvasHash,
    webgl_hash: webglHash
  };

  const now = new Date();
  const hour = chaosState.nightTime ? 2 : now.getHours();
  const context: Record<string, unknown> = {
    country: "GB", // in a real deployment you'd not hard-code this
    hour,
    vpn: chaosState.vpn,
    high_risk_action: chaosState.highRiskAction
  };

  return {
    user_id_hint: userIdHint,
    device,
    behavior,
    context
  };
}
