import React, { useState } from "react";
import { DeviceDiffPanel } from "./DeviceDiffPanel";
import { BehaviorInspector } from "./BehaviorInspector";
import { RiskTimeline } from "./RiskTimeline";
import { DeviceHistoryCharts } from "./DeviceHistoryCharts";
import { TlsFpVisualizer } from "./TlsFpVisualizer";
import { useSessionContext } from "../state/session";

/**
 * EPIC 8 – Showcase Dashboard shell.
 *
 * This component wires together the major panels required for the full dashboard:
 * - device diff viewer
 * - behavioural inspector
 * - risk explanation timeline
 * - device history charts
 * - TLS fingerprint visualiser
 *
 * Mini-Epic 8B only provides layout + placeholders. The actual data wiring will be added
 * in later mini-epics.
 */
export function ShowcaseDashboard() {
  const { userHint, setUserHint } = useSessionContext();
  const [tlsFpHint, setTlsFpHint] = useState<string>("");

  return (
    <div style={containerStyle}>
      <section style={headerCardStyle}>
        <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem" }}>
          <h2 style={{ margin: 0 }}>Showcase dashboard</h2>
          <p style={subtitleStyle}>
            This dashboard brings together device, behavioural, risk, and TLS fingerprint signals for a single
            demo user handle. Later mini-epics will wire these panels to the backend analytics & history APIs.
          </p>
        </div>
        <div style={userSwitcherStyle}>
          <label style={{ fontSize: "0.85rem", fontWeight: 500 }}>
            Demo user handle
            <input
              type="text"
              value={userHint}
              onChange={(e) => setUserHint(e.target.value)}
              placeholder="e.g. demo-user, alice-laptop"
              style={userInputStyle}
            />
          </label>
          <p style={userHintHelpStyle}>
            Global selector — updates Showcase, Analyst and Admin views.
          </p>
        </div>
      </section>

      <section style={gridTwoCols}>
        <DeviceHistoryCharts userHint={userHint} onTlsSelect={setTlsFpHint} />
        <DeviceDiffPanel userHint={userHint} />
      </section>

      <section style={gridTwoCols}>
        <BehaviorInspector userHint={userHint} />
        <RiskTimeline userHint={userHint} />
      </section>

      <section style={singleRowStyle}>
        <TlsFpVisualizer userHint={userHint} preselectedFp={tlsFpHint} />
      </section>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "1rem",
  maxWidth: 1100,
  margin: "0 auto"
};

const headerCardStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "flex-start",
  gap: "1.5rem",
  padding: "0.75rem 1rem",
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "var(--border)",
  background: "var(--panel)"
};

const subtitleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: "0.85rem",
  color: "#4b5563",
  maxWidth: 640
};

const userSwitcherStyle: React.CSSProperties = {
  minWidth: 260,
  maxWidth: 320,
  fontSize: "0.85rem"
};

const userInputStyle: React.CSSProperties = {
  display: "block",
  marginTop: "0.25rem",
  width: "100%",
  padding: "0.35rem 0.5rem",
  borderRadius: 4,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#d1d5db",
  fontSize: "0.9rem"
};

const userHintHelpStyle: React.CSSProperties = {
  margin: "0.35rem 0 0",
  fontSize: "0.75rem",
  color: "#6b7280"
};

const gridTwoCols: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1.1fr) minmax(0, 0.9fr)",
  gap: "1rem",
  alignItems: "flex-start"
};

const singleRowStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.75rem"
};