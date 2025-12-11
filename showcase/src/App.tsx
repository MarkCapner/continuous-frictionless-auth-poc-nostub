import { useEffect, useState } from "react";
import { startProfiler, snapshotTelemetry } from "./profiler";
import type { TelemetryPayload, DecisionResponse } from "./api";
import { postProfileCheck } from "./api";
import { DeviceCard } from "./components/DeviceCard";
import { TlsPanel } from "./components/TlsPanel";
import { TlsFingerprintInspector } from "./components/TlsFingerprintInspector";
import { BehaviorPanel } from "./components/BehaviorPanel";
import { SessionTimeline } from "./components/SessionTimeline";
import { UsersOverview } from "./components/UsersOverview";
import { ChaosToggles } from "./components/ChaosToggles";
import { AdminTlsView } from "./components/AdminTlsView";
import { AdminBehaviorView } from "./components/AdminBehaviorView";
import { AdminMlView } from "./components/AdminMlView";
import { AdminUsersView } from "./components/AdminUsersView";

type ViewKey =
  | "showcase"
  | "admin-tls"
  | "admin-behavior"
  | "admin-users"
  | "admin-ml";

function App() {
  const [telemetry, setTelemetry] = useState<TelemetryPayload | null>(null);
  const [decision, setDecision] = useState<DecisionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [demoUser, setDemoUser] = useState<string>("demo-user");
  const [view, setView] = useState<ViewKey>("showcase");

  useEffect(() => {
    startProfiler();
  }, []);

  const runProfileCheck = async () => {
    setError(null);
    setLoading(true);
    try {
      const userHint = demoUser.trim() || "demo-user";
      const payload = snapshotTelemetry(userHint);
      setTelemetry(payload);
      const resp = await postProfileCheck(payload);
      setDecision(resp);
    } catch (e: any) {
      setError(e?.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  const title =
    view === "showcase"
      ? "Continuous Frictionless Auth – Showcase"
      : view === "admin-tls"
      ? "Admin / TLS fingerprints"
      : view === "admin-behavior"
      ? "Admin / Behaviour baselines"
      : view === "admin-users"
      ? "Admin / Users"
      : "Admin / ML Model";

  return (
    <div style={pageStyle}>
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "baseline",
          gap: "1rem",
          marginBottom: "1.5rem"
        }}
      >
        <div>
          <h1 style={{ marginBottom: "0.25rem" }}>{title}</h1>
          {view === "showcase" && (
            <p style={{ maxWidth: 640, margin: 0 }}>
              This page visualises the device profile, TLS fingerprint, lightweight behavioural biometrics and
              risk decisions used by the PoC. No cookies or local storage are used; everything is computed in-memory.
            </p>
          )}
        </div>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button
            type="button"
            onClick={() => setView("showcase")}
            style={view === "showcase" ? tabButtonActiveStyle : tabButtonStyle}
          >
            Showcase
          </button>
          <button
            type="button"
            onClick={() => setView("admin-tls")}
            style={view === "admin-tls" ? tabButtonActiveStyle : tabButtonStyle}
          >
            Admin / TLS
          </button>
          <button
            type="button"
            onClick={() => setView("admin-behavior")}
            style={view === "admin-behavior" ? tabButtonActiveStyle : tabButtonStyle}
          >
            Admin / Behaviour
          </button>
          <button
            type="button"
            onClick={() => setView("admin-users")}
            style={view === "admin-users" ? tabButtonActiveStyle : tabButtonStyle}
          >
            Admin / Users
          </button>
          <button
            type="button"
            onClick={() => setView("admin-ml")}
            style={view === "admin-ml" ? tabButtonActiveStyle : tabButtonStyle}
          >
            Admin / ML Model
          </button>
        </div>
      </header>

      {view === "showcase" ? (
        <div style={mainStyle}>
          <section style={demoRowStyle}>
            <div style={userRowStyle}>
              <label style={{ fontWeight: 500 }}>
                Demo user handle:
                <input
                  type="text"
                  value={demoUser}
                  onChange={(e) => setDemoUser(e.target.value)}
                  placeholder="e.g. mark-demo, alice-laptop"
                  style={inputStyle}
                />
              </label>
              <span style={{ fontSize: "0.8rem", color: "#6b7280" }}>
                This value is sent as <code>user_id_hint</code> so you can compare devices per handle.
              </span>
            </div>

            <div style={{ marginBottom: "1rem" }}>
              <button onClick={runProfileCheck} disabled={loading} style={buttonStyle}>
                {loading ? "Running profile check..." : "Run profile check"}
              </button>
              {error && <span style={{ color: "red", marginLeft: "1rem" }}>{error}</span>}
            </div>

            <section style={gridTwoCols}>
              <DeviceCard device={telemetry ? telemetry.device : null} />
              <div style={{ display: "grid", gridTemplateRows: "min-content min-content", gap: "0.75rem" }}>
                <TlsPanel decision={decision} />
                <TlsFingerprintInspector tlsFp={decision?.tls_fp} />
              </div>
            </section>

            <section style={gridTwoCols}>
              <BehaviorPanel behavior={telemetry ? telemetry.behavior : null} />
              <ChaosToggles />
            </section>

            <section style={gridTwoCols}>
              <SessionTimeline userHint={demoUser.trim() || "demo-user"} />
              <UsersOverview />
            </section>

            {decision && (
              <section style={{ marginTop: "1.5rem" }}>
                <h2>Decision details</h2>
                <p>
                  Decision: <strong>{decision.decision}</strong>{" "}
                  (confidence: {(decision.confidence * 100).toFixed(1)}%)
                </p>
                <p>
                  Model: <code>{decision.model_version ?? "synthetic / unknown"}</code>
                </p>
                {typeof decision.breakdown.ml_anomaly_score === "number" && (
                  <p>
                    Anomaly score:{" "}
                    <strong>{decision.breakdown.ml_anomaly_score!.toFixed(3)}</strong>
                    {" "}
                    (0 = normal, 1 = very unusual)
                  </p>
                )}
                <h3>Score breakdown</h3>
                <pre style={preStyle}>{JSON.stringify(decision.breakdown, null, 2)}</pre>
                <h3>Explanations</h3>
                <ul>
                  {decision.explanations.map((e, idx) => (
                    <li key={idx}>{e}</li>
                  ))}
                </ul>
              </section>
            )}
          </section>
        </div>
      ) : view === "admin-tls" ? (
        <AdminTlsView />
      ) : view === "admin-behavior" ? (
        <AdminBehaviorView />
      ) : view === "admin-users" ? (
        <AdminUsersView />
      ) : (
        <AdminMlView />
      )}
    </div>
  );
}

const pageStyle: React.CSSProperties = {
  fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  padding: "1.5rem",
  background: "#f3f4f6",
  minHeight: "100vh",
  boxSizing: "border-box"
};

const mainStyle: React.CSSProperties = {
  maxWidth: 1100,
  margin: "0 auto"
};

const demoRowStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "1.25rem"
};

const userRowStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "0.25rem",
  marginBottom: "0.75rem"
};

const gridTwoCols: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1.1fr) minmax(0, 0.9fr)",
  gap: "1rem",
  alignItems: "flex-start"
};

const inputStyle: React.CSSProperties = {
  display: "block",
  marginTop: "0.35rem",
  padding: "0.35rem 0.5rem",
  borderRadius: 4,
  border: "1px solid #d1d5db",
  fontSize: "0.9rem",
  minWidth: 260
};

const buttonStyle: React.CSSProperties = {
  padding: "0.4rem 0.9rem",
  borderRadius: 999,
  border: "none",
  cursor: "pointer",
  background: "#4f46e5",
  color: "#fff",
  fontWeight: 500,
  fontSize: "0.9rem"
};

const tabButtonStyle: React.CSSProperties = {
  padding: "0.35rem 0.9rem",
  borderRadius: 999,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#fff",
  cursor: "pointer",
  fontSize: "0.85rem"
};

const tabButtonActiveStyle: React.CSSProperties = {
  ...tabButtonStyle,
  borderColor: "#4f46e5",
  background: "#eef2ff",
  color: "#312e81"
};


const preStyle: React.CSSProperties = {
  background: "#111827",
  color: "#e5e7eb",
  padding: "0.75rem",
  borderRadius: 8,
  fontSize: "0.8rem",
  overflowX: "auto"
};

export default App;
