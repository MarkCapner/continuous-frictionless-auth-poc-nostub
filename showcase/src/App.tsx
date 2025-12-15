import { useEffect, useMemo, useState } from "react";
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
import { AdminAnalyticsView } from "./components/AdminAnalyticsView";
import { AdminPolicyView } from "./components/AdminPolicyView";
import { ShowcaseDashboard } from "./components/ShowcaseDashboard";
import { TrustSnapshotPanel } from "./components/TrustSnapshotPanel";
import { Shell, type NavItem } from "./ui/Shell";

type ViewKey =
  | "showcase"
  | "showcase-dashboard"
  | "admin-tls"
  | "admin-behavior"
  | "admin-users"
  | "admin-analytics"
  | "admin-policy"
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

  const navItems: NavItem[] = useMemo(
    () => [
      { key: "showcase", label: "Showcase", section: "Explore" },
      { key: "showcase-dashboard", label: "Dashboard", section: "Explore" },
      { key: "admin-tls", label: "TLS fingerprints", section: "Admin" },
      { key: "admin-behavior", label: "Behaviour baselines", section: "Admin" },
      { key: "admin-users", label: "Users", section: "Admin" },
      { key: "admin-analytics", label: "Analytics", section: "Admin" },
      { key: "admin-policy", label: "Policy", section: "Admin" },
      { key: "admin-ml", label: "ML model", section: "Admin" }
    ],
    []
  );

  const header = useMemo(() => {
    switch (view) {
      case "showcase":
        return {
          title: "Showcase",
          subtitle:
            "Device profile, TLS fingerprinting, behavioural signals, and risk decisions. No cookies or local storage; everything is computed in-memory."
        };
      case "showcase-dashboard":
        return { title: "Dashboard", subtitle: "Explainability views across sessions, devices and risk." };
      case "admin-tls":
        return { title: "Admin · TLS", subtitle: "Inspect fingerprints, families, and clustering metadata." };
      case "admin-behavior":
        return { title: "Admin · Behaviour", subtitle: "Per-user behavioural baselines and z-scores." };
      case "admin-users":
        return { title: "Admin · Users", subtitle: "User and device summaries." };
      case "admin-analytics":
        return { title: "Admin · Analytics", subtitle: "Session stats, risk breakdown and trends." };
      case "admin-policy":
        return { title: "Admin · Policy", subtitle: "Create and manage policy rules (scope, conditions, actions)." };
      case "admin-ml":
      default:
        return { title: "Admin · ML", subtitle: "Model status and re-training controls." };
    }
  }, [view]);

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

  return (
    <Shell
      title={`Continuous Frictionless Auth · ${header.title}`}
      subtitle={header.subtitle}
      items={navItems}
      activeKey={view}
      onNavigate={(k) => setView(k as ViewKey)}
      topRight={
        view === "showcase" ? (
          <button className={`btn btnPrimary`} onClick={runProfileCheck} disabled={loading}>
            {loading ? "Running…" : "Run profile check"}
          </button>
        ) : null
      }
    >
      {view === "showcase" ? (
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          <div className="card">
            <div className="cardTitle">
              <h3>Session controls</h3>
              <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                {error ? <span className="chip chipDanger">{error}</span> : null}
              </div>
            </div>

            <div className="grid2Equal">
              <label style={{ display: "block" }}>
                <span className="muted">Demo user handle</span>
                <input
                  className="input"
                  type="text"
                  value={demoUser}
                  onChange={(e) => setDemoUser(e.target.value)}
                  placeholder="e.g. mark-demo, alice-laptop"
                />
                <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
                  Sent as <span className="mono">user_id_hint</span> so you can compare devices per handle.
                </div>
              </label>

              <div>
                <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>
                  Controls
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 10 }}>
                  <button className="btn btnPrimary" onClick={runProfileCheck} disabled={loading}>
                    {loading ? "Running…" : "Run profile check"}
                  </button>
                  <span className="chip chipAccent">No cookies</span>
                  <span className="chip chipAccent2">No local storage</span>
                </div>
              </div>
            </div>
          </div>

          <div className="grid2">
            <DeviceCard device={telemetry ? telemetry.device : null} />
            <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <TlsPanel decision={decision} />
              <TlsFingerprintInspector tlsFp={decision?.tls_fp} />
            </div>
          </div>

          <div className="grid2">
            <BehaviorPanel behavior={telemetry ? telemetry.behavior : null} />
            <ChaosToggles />
          </div>

          <div className="grid2">
            <SessionTimeline userHint={demoUser.trim() || "demo-user"} />
            <UsersOverview />
          </div>

          {decision ? (
            <>
            <div className="card">
              <div className="cardTitle">
                <h3>Decision details</h3>
                <span className="chip chipAccent">
                  {decision.decision} · {(decision.confidence * 100).toFixed(1)}%
                </span>
              </div>

              <div className="muted" style={{ fontSize: 12, marginBottom: 10 }}>
                Model: <span className="mono">{decision.model_version ?? "synthetic / unknown"}</span>
              </div>

              {typeof decision.breakdown.ml_anomaly_score === "number" ? (
                <div className="muted" style={{ fontSize: 12, marginBottom: 10 }}>
                  Anomaly score: <span className="mono">{decision.breakdown.ml_anomaly_score!.toFixed(3)}</span> (0 = normal, 1 = very unusual)
                </div>
              ) : null}

              <div className="divider" />

              <div className="grid2Equal">
                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>
                    Score breakdown
                  </div>
                  <pre
                    style={{
                      margin: 0,
                      background: "rgba(0,0,0,0.35)",
                      border: "1px solid rgba(255,255,255,0.10)",
                      borderRadius: 12,
                      padding: 12,
                      overflowX: "auto"
                    }}
                  >
                    {JSON.stringify(decision.breakdown, null, 2)}
                  </pre>
                </div>

                <div>
                  <div className="muted" style={{ fontSize: 12, marginBottom: 8 }}>
                    Explanations
                  </div>
                  <ul style={{ margin: 0, paddingLeft: 18, color: "rgba(255,255,255,0.84)" }}>
                    {decision.explanations.map((e, idx) => (
                      <li key={idx} style={{ marginBottom: 6 }}>
                        {e}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>

            <TrustSnapshotPanel
              sessionId={
                (decision as any).session_id ??
                (decision as any).sessionId ??
                (decision as any).request_id ??
                (decision as any).requestId
              }
            />
          </>
          ) : null}
        </div>
      ) : view === "showcase-dashboard" ? (
        <ShowcaseDashboard />
      ) : view === "admin-tls" ? (
        <AdminTlsView />
      ) : view === "admin-behavior" ? (
        <AdminBehaviorView />
      ) : view === "admin-users" ? (
        <AdminUsersView />
      ) : view === "admin-analytics" ? (
        <AdminAnalyticsView />
      ) : view === "admin-policy" ? (
        <AdminPolicyView />
      ) : (
        <AdminMlView />
      )}
    </Shell>
  );
}

export default App;