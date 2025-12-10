import type { DeviceTelemetry } from "../api";

interface DeviceCardProps {
  device: DeviceTelemetry | null;
}

export function DeviceCard({ device }: DeviceCardProps) {
  if (!device) {
    return (
      <div style={cardStyle}>
        <h2>Device</h2>
        <p>No telemetry yet. Click <strong>Run profile check</strong> to capture device info.</p>
      </div>
    );
  }

  const ua = device.ua;
  const uaFamily = ua.split(" ")[0] ?? ua;
  const screen = `${device.screen.w}×${device.screen.h} @${device.screen.pixel_ratio}x`;
  const tz = `${device.tz_offset} min`;

  return (
    <div style={cardStyle}>
      <h2>Device</h2>
      <p><strong>UA family:</strong> {uaFamily}</p>
      <p><strong>Platform:</strong> {device.platform ?? "unknown"}</p>
      <p><strong>Screen:</strong> {screen}</p>
      <p><strong>Timezone offset:</strong> {tz}</p>
      <p><strong>Languages:</strong> {device.langs.join(", ")}</p>
      <p><strong>Canvas hash:</strong> {device.canvas_hash ?? "n/a"}</p>
      <p><strong>WebGL hash:</strong> {device.webgl_hash ?? "n/a"}</p>
    </div>
  );
}

const cardStyle = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  flex: 1,
  minWidth: 260
};
