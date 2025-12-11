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

function App() {
  const [telemetry, setTelemetry] = useState<TelemetryPayload | null>(null);
  const [decision, setDecision] = useState<DecisionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [demoUser, setDemoUser] = useState<string>("demo-user");
  const [view, setView] = useState<"showcase" | "admin-tls">("showcase");

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
      setError(e.message ?? String(e));
    } finally {
      setLoading(false);
    }
  };

  
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
          <h1 style={{ marginBottom: "0.25rem" }}>
            {view === "showcase"
              ? "Continuous Frictionless Auth – Showcase"
              : "Admin / TLS fingerprints"}
          </h1>
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
                <h3>Score breakdown</h3>
                <pre style={preStyle}>{JSON.stringify(decision.breakdown, null, 2)}</pre>
                <h3>Explanations</h3>
                <ul>
                  {decision.explanations.map((e, idx) => (
                    <li key={idx}>{e}</li>
                  ))}
                </ul>
                <p>Session ID: {decision.session_id}</p>
              </section>
            )}
          </section>
        </div>
      ) : (
        <AdminTlsView />
      )}
    </div>
  );
}

const pageStyle = {
  fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  padding: "1.5rem",
  background: "#f3f4f6",
  minHeight: "100vh",
  boxSizing: "border-box"
};

const mainStyle = {
  maxWidth: 1100,
  margin: "0 auto"
};

const demoRowStyle = {
  display: "flex",
  flexDirection: "column" as const,
  gap: "1.25rem"
};

const userRowStyle = {
  display: "flex",
  flexDirection: "column" as const,
  gap: "0.25rem",
  marginBottom: "0.75rem"
};

const gridTwoCols = {
  display: "grid",
  gridTemplateColumns: "1.4fr 1fr",
  gap: "1.25rem",
  alignItems: "flex-start",
  marginTop: "1.25rem"
};

const inputStyle = {
  display: "block",
  marginTop: "0.25rem",
  padding: "0.35rem 0.5rem",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  fontSize: "0.9rem",
  minWidth: 260
};

const buttonStyle = {
  padding: "0.5rem 1rem",
  borderRadius: 999,
  border: "none",
  background: "#4f46e5",
  color: "#fff",
  cursor: "pointer",
  fontSize: "0.95rem"
};

const tabButtonStyle = {
  padding: "0.35rem 0.75rem",
  borderRadius: 999,
  border: "1px solid #d1d5db",
  background: "#fff",
  color: "#111827",
  fontSize: "0.8rem",
  cursor: "pointer"
};

const tabButtonActiveStyle = {
  ...tabButtonStyle,
  borderColor: "#4f46e5",
  background: "#eef2ff",
  color: "#312e81"
};

const preStyle = {
  background: "#111827",
  color: "#e5e7eb",
  padding: "0.75rem",
  borderRadius: 8,
  fontSize: "0.8rem",
  overflowX: "auto"
};

export default App;
