import React, { useEffect, useState } from "react";
import type { DeviceProfileSummary } from "../api";
import { fetchDeviceHistory } from "../api";

export interface DeviceHistoryChartsProps {
  userHint: string;
}

/**
 * Mini-Epic 8C: device history wiring.
 *
 * This component fetches the list of devices for a given user and shows a
 * simple "chart" of how often each device has been seen, alongside basic
 * metadata (UA, screen, country).
 */
export function DeviceHistoryCharts({ userHint }: DeviceHistoryChartsProps) {
  const [devices, setDevices] = useState<DeviceProfileSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const hint = (userHint || "demo-user").trim() || "demo-user";
      setLoading(true);
      setError(null);
      try {
        const data = await fetchDeviceHistory(hint);
        if (!cancelled) {
          setDevices(data);
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e?.message ?? String(e));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [userHint]);

  const maxSeen = devices.reduce((m, d) => Math.max(m, d.seenCount || 0), 0) || 1;

  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Device history</h3>
      <p style={bodyStyle}>
        Devices observed for <code>{(userHint || "demo-user").trim() || "demo-user"}</code>. Each bar corresponds to a
        stored device profile (UA + screen + TLS fingerprint) and its approximate frequency of use.
      </p>
      {loading && <p style={bodyStyle}>Loading device history…</p>}
      {error && (
        <p style={{ ...bodyStyle, color: "#b91c1c" }}>
          Error while loading device history: {error}
        </p>
      )}
      {!loading && !error && devices.length === 0 && (
        <p style={placeholderStyle}>No devices recorded yet. Run a few profile checks for this user.</p>
      )}
      {!loading && !error && devices.length > 0 && (
        <div style={{ display: "flex", flexDirection: "column", gap: "0.4rem", marginTop: "0.5rem" }}>
          {devices.map((dev) => {
            const pct = (dev.seenCount / maxSeen) * 100;
            const label =
              (dev.uaFamily || "Unknown") +
              (dev.uaVersion ? " " + dev.uaVersion : "") +
              ` · ${dev.screenW}×${dev.screenH}`;
            return (
              <div
                key={dev.id}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem"
                }}
              >
                <div style={{ width: 70, fontSize: "0.75rem", color: "#4b5563" }}>#{dev.id}</div>
                <div style={{ flex: 1 }}>
                  <div
                    style={{
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
                        background: "#4f46e5"
                      }}
                    />
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.75rem", marginTop: 2 }}>
                    <span style={{ color: "#374151" }}>{label}</span>
                    <span style={{ color: "#6b7280" }}>
                      {dev.seenCount} seen
                      {dev.lastCountry ? ` · ${dev.lastCountry}` : ""}
                    </span>
                  </div>
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

const placeholderStyle: React.CSSProperties = {
  margin: "0.25rem 0 0",
  fontStyle: "italic",
  color: "#6b7280"
};
