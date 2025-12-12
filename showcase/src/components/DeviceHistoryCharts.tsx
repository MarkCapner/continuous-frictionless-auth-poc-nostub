import React, { useEffect, useState } from "react";
import type { DeviceProfileSummary } from "../api";
import { fetchDeviceHistory } from "../api";

export interface DeviceHistoryChartsProps {
  userHint: string;
  onTlsSelect?: (tlsFp: string) => void;
}

/**
 * Mini-Epic 8F: device history charts with TLS FP selection.
 *
 * This component fetches the list of devices for a given user and visualises:
 * - how often each device has been seen (bar length)
 * - rough recency (colour intensity based on lastSeen)
 * It also lets you click a TLS fingerprint to drive the TLS visualiser panel.
 */
export function DeviceHistoryCharts({ userHint, onTlsSelect }: DeviceHistoryChartsProps) {
  const [devices, setDevices] = useState<DeviceProfileSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const hint = (userHint || "demo-user").trim() || "demo-user";
      setLoading(true);
      setError(null);
      setDevices([]);
      try {
        const data = await fetchDeviceHistory(hint);
        if (!cancelled) {
          setDevices(data);
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [userHint]);

  const maxSeen = devices.reduce((m, d) => Math.max(m, d.seenCount || 0), 0) || 1;
  const newest = devices.reduce((m, d) => {
    if (!d.lastSeen) return m;
    const t = Date.parse(d.lastSeen);
    return Number.isNaN(t) ? m : Math.max(m, t);
  }, 0);

  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Device history</h3>
      <p style={bodyStyle}>
        Devices observed for <code>{(userHint || "demo-user").trim() || "demo-user"}</code>. Bar length shows how often a
        profile has been seen; colour intensity hints at recency (newer = deeper colour).
      </p>
      {loading && <p style={bodyStyle}>Loading devices…</p>}
      {error && (
        <p style={{ ...bodyStyle, color: "#b91c1c" }}>
          Error loading device history: {error}
        </p>
      )}
      {!loading && !error && devices.length === 0 && (
        <p style={bodyStyle}>No device profiles recorded yet. Generate some sessions first.</p>
      )}
      {!loading && !error && devices.length > 0 && (
        <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem", marginTop: "0.25rem" }}>
          {devices.map((dev) => {
            const pct = (dev.seenCount / maxSeen) * 100;
            let intensity = 0.6;
            if (newest && dev.lastSeen) {
              const t = Date.parse(dev.lastSeen);
              if (!Number.isNaN(t)) {
                const ageMs = newest - t;
                const thirtyDaysMs = 1000 * 60 * 60 * 24 * 30;
                const ratio = Math.min(Math.max(ageMs / thirtyDaysMs, 0), 1);
                intensity = 0.4 + (1 - ratio) * 0.6; // newer -> closer to 1.0
              }
            }
            const barColour = `rgba(79,70,229,${intensity.toFixed(2)})`;
            const uaLabel =
              (dev.uaFamily || "Unknown") + (dev.uaVersion ? " " + dev.uaVersion : "");
            const screenLabel = `${dev.screenW}×${dev.screenH}`;

            return (
              <div
                key={dev.id}
                style={{ display: "flex", flexDirection: "column", gap: 2, fontSize: "0.8rem" }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", gap: "0.5rem" }}>
                  <span style={{ color: "#374151" }}>
                    #{dev.id} · {uaLabel} · {screenLabel}
                  </span>
                  <span style={{ color: "#6b7280" }}>
                    {dev.seenCount} seen
                    {dev.lastCountry ? ` · ${dev.lastCountry}` : ""}
                  </span>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                  <div
                    style={{
                      flex: 1,
                      background: "#e5e7eb",
                      borderRadius: 999,
                      overflow: "hidden"
                    }}
                  >
                    <div
                      style={{
                        width: `${pct}%`,
                        minWidth: pct > 0 ? "4px" : "0",
                        height: 10,
                        borderRadius: 999,
                        background: barColour
                      }}
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => onTlsSelect && onTlsSelect(dev.tlsFp)}
                    style={tlsButtonStyle}
                  >
                    TLS FP
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#fff",
  padding: "0.75rem 1rem",
  minHeight: 160,
  boxSizing: "border-box"
};

const titleStyle: React.CSSProperties = {
  margin: "0 0 0.35rem"
};

const bodyStyle: React.CSSProperties = {
  margin: "0.25rem 0"
};

const tlsButtonStyle: React.CSSProperties = {
  fontSize: "0.7rem",
  padding: "0.15rem 0.4rem",
  borderRadius: 999,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#4f46e5",
  background: "#eef2ff",
  color: "#312e81",
  cursor: "pointer",
  whiteSpace: "nowrap"
};
