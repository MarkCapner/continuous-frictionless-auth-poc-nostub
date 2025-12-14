import type { DeviceTelemetry } from "../api";

interface DeviceCardProps {
  device: DeviceTelemetry | null;
}

export function DeviceCard({ device }: DeviceCardProps) {
  if (!device) {
    return (
      <div className="card">
        <div className="cardTitle">
          <h3>Device profile</h3>
          <span className="chip">no telemetry</span>
        </div>
        <p className="muted" style={{ margin: 0 }}>
          No telemetry yet. Click <strong>Run profile check</strong> to capture device info.
        </p>
      </div>
    );
  }

  const ua = device.ua;
  const uaFamily = ua.split(" ")[0] ?? ua;
  const screen = `${device.screen.w}×${device.screen.h} @${device.screen.pixel_ratio}x`;
  const tz = `${device.tz_offset} min`;

  return (
    <div className="card">
      <div className="cardTitle">
        <h3>Device profile</h3>
        <span className="chip chipAccent">live</span>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "minmax(0,1fr) minmax(0,1fr)", gap: 10 }}>
        <div>
          <div className="muted" style={{ fontSize: 12 }}>UA family</div>
          <div style={{ marginTop: 4 }}>{uaFamily}</div>
        </div>
        <div>
          <div className="muted" style={{ fontSize: 12 }}>Platform</div>
          <div style={{ marginTop: 4 }}>{device.platform ?? "unknown"}</div>
        </div>
        <div>
          <div className="muted" style={{ fontSize: 12 }}>Screen</div>
          <div style={{ marginTop: 4 }}>{screen}</div>
        </div>
        <div>
          <div className="muted" style={{ fontSize: 12 }}>Timezone offset</div>
          <div style={{ marginTop: 4 }}>{tz}</div>
        </div>
      </div>

      <div className="divider" />

      <div className="muted" style={{ fontSize: 12 }}>Languages</div>
      <div style={{ marginTop: 4 }}>{device.langs.join(", ")}</div>

      <div style={{ marginTop: 10 }}>
        <div className="muted" style={{ fontSize: 12 }}>Canvas hash</div>
        <div className="mono" style={{ marginTop: 4, wordBreak: "break-all" }}>{device.canvas_hash ?? "n/a"}</div>
      </div>
      <div style={{ marginTop: 10 }}>
        <div className="muted" style={{ fontSize: 12 }}>WebGL hash</div>
        <div className="mono" style={{ marginTop: 4, wordBreak: "break-all" }}>{device.webgl_hash ?? "n/a"}</div>
      </div>
    </div>
  );
}
