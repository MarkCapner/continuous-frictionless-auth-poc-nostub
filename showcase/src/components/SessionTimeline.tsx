import { useEffect, useMemo, useRef, useState } from "react";
import type { SessionSummary, TrustSnapshot, TrustSignal } from "../api";
import { fetchSessions, getTrustSnapshot } from "../api";
import { TrustSnapshotPanel } from "./TrustSnapshotPanel";

interface SessionTimelineProps {
  userHint: string;
  limit?: number;
}

/**
 * EPIC 12.5 — Trust Timeline & Session History
 * - Shows recent sessions for the user
 * - Hover shows a lightweight trust explanation (riskSummary + signal statuses)
 * - Click expands the full TrustSnapshotPanel for the selected session
 */
export function SessionTimeline({ userHint, limit = 20 }: SessionTimelineProps) {
  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const [hoveredId, setHoveredId] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [snapshots, setSnapshots] = useState<Record<string, TrustSnapshot | null>>({});
  const [snapshotErrors, setSnapshotErrors] = useState<Record<string, string>>({});

  const hoverTimer = useRef<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!userHint) return;
      setLoading(true);
      setError(null);
      try {
        const data = await fetchSessions(userHint, limit);
        if (!cancelled) setSessions(data);
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [userHint, limit]);

  const title = useMemo(() => {
    if (!userHint) return "Session history";
    return `Session history — ${userHint}`;
  }, [userHint]);

  async function ensureSnapshot(sessionId: string) {
    if (snapshots[sessionId] !== undefined) return; // cached (including null)
    try {
      const s = await getTrustSnapshot(sessionId);
      setSnapshots((prev) => ({ ...prev, [sessionId]: s }));
    } catch (e: any) {
      setSnapshots((prev) => ({ ...prev, [sessionId]: null }));
      setSnapshotErrors((prev) => ({ ...prev, [sessionId]: e?.message ?? String(e) }));
    }
  }

  function decisionChip(decision: string) {
    const d = (decision || "").toUpperCase();
    if (d === "ALLOW" || d === "TRUST" || d === "PASS") return { label: "TRUSTED", className: "chip chipAccent" };
    if (d.includes("STEP")) return { label: "CHALLENGE", className: "chip chipAccent2" };
    if (d === "BLOCK" || d === "DENY") return { label: "BLOCKED", className: "chip chipDanger" };
    return { label: d || "UNKNOWN", className: "chip" };
  }

  function signalIcon(s: TrustSignal): string {
    const cat = (s.category || "").toUpperCase();
    const status = (s.status || "").toUpperCase();
    const bang = status === "WARN" || status === "RISK" ? "⚠️" : "";
    if (cat === "DEVICE") return `🖥️${bang}`;
    if (cat === "BEHAVIOUR" || cat === "BEHAVIOR") return `⌨️${bang}`;
    if (cat === "TLS") return `🔒${bang}`;
    if (cat === "CONTEXT") return `🌐${bang}`;
    return `ℹ️${bang}`;
  }

  const cardStyle = {
    border: "1px solid rgba(255,255,255,0.10)",
    borderRadius: 12,
    padding: "1rem",
    background: "rgba(0,0,0,0.22)",
    boxShadow: "0 1px 3px rgba(0,0,0,0.10)",
    minWidth: 260
  };

  const itemStyle = {
    padding: "0.65rem 0",
    borderBottom: "1px solid rgba(255,255,255,0.08)",
    position: "relative",
    cursor: "pointer"
  };

  const metaStyle = {
    display: "flex",
    gap: 10,
    alignItems: "center",
    flexWrap: "wrap"
  };

  const tooltipStyle = {
    position: "absolute",
    top: "100%",
    left: 0,
    zIndex: 20,
    marginTop: 8,
    width: "min(520px, 92vw)",
    background: "rgba(0,0,0,0.85)",
    border: "1px solid rgba(255,255,255,0.14)",
    borderRadius: 12,
    padding: 12,
    boxShadow: "0 10px 30px rgba(0,0,0,0.35)"
  };

  return (
    <div style={cardStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 12 }}>
        <h3 style={{ margin: 0 }}>{title}</h3>
        <span className="muted" style={{ fontSize: 12 }}>
          {loading ? "Loading…" : `${sessions.length} shown`}
        </span>
      </div>

      {error ? (
        <div style={{ marginTop: 10, color: "rgba(255,120,120,0.95)" }}>{error}</div>
      ) : null}

      {!loading && sessions.length === 0 ? (
        <div className="muted" style={{ marginTop: 10 }}>
          No sessions yet — run the Showcase a few times.
        </div>
      ) : null}

      <ul style={{ listStyle: "none", padding: 0, margin: "0.75rem 0 0 0" }}>
        {sessions.map((s) => {
          const chip = decisionChip(s.decision);
          const sid = (s.sessionId || "").toString();
          const hovered = hoveredId === sid;

          const snapshot = snapshots[sid];
          const snapErr = snapshotErrors[sid];

          return (
            <li
              key={s.id}
              style={itemStyle}
              onMouseEnter={() => {
                // Small delay to avoid spamming fetches when moving the mouse
                if (hoverTimer.current) window.clearTimeout(hoverTimer.current);
                setHoveredId(sid);
                hoverTimer.current = window.setTimeout(() => {
                  ensureSnapshot(sid);
                }, 180);
              }}
              onMouseLeave={() => {
                if (hoverTimer.current) window.clearTimeout(hoverTimer.current);
                setHoveredId(null);
              }}
              onClick={() => setSelectedId((prev) => (prev === sid ? null : sid))}
              title="Click to expand the full trust snapshot"
            >
              <div style={metaStyle}>
                <span className={chip.className}>{chip.label}</span>
                <span className="chip">confidence {(s.confidence * 100).toFixed(1)}%</span>
                <span className="muted" style={{ fontSize: 12 }}>
                  {new Date(s.createdAt).toLocaleString()}
                </span>
                <span className="muted" style={{ fontSize: 12 }}>
                  session {sid.slice(0, 8)}…
                </span>
              </div>

              {hovered ? (
                <div style={tooltipStyle}>
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "baseline" }}>
                    <div style={{ fontWeight: 700 }}>Trust snapshot</div>
                    <div className="muted" style={{ fontSize: 12 }}>hover preview</div>
                  </div>

                  {snapErr ? (
                    <div style={{ marginTop: 8, color: "rgba(255,120,120,0.95)" }}>
                      Could not load snapshot: {snapErr}
                    </div>
                  ) : snapshot === undefined ? (
                    <div className="muted" style={{ marginTop: 8 }}>Loading…</div>
                  ) : snapshot === null ? (
                    <div className="muted" style={{ marginTop: 8 }}>No snapshot available for this session.</div>
                  ) : (
                    <>
                      <div style={{ marginTop: 8, color: "rgba(255,255,255,0.92)" }}>
                        {snapshot.riskSummary}
                      </div>

                      <div style={{ marginTop: 10, display: "flex", gap: 10, flexWrap: "wrap" }}>
                        {(snapshot.signals || []).map((sig, idx) => (
                          <span key={idx} className="chip" title={sig.label}>
                            {signalIcon(sig)} {sig.label}
                          </span>
                        ))}
                      </div>

                      {snapshot.changes && snapshot.changes.length > 0 ? (
                        <div className="muted" style={{ marginTop: 10, fontSize: 12 }}>
                          What changed: {snapshot.changes.length} item{snapshot.changes.length === 1 ? "" : "s"}
                        </div>
                      ) : (
                        <div className="muted" style={{ marginTop: 10, fontSize: 12 }}>
                          What changed: nothing meaningful
                        </div>
                      )}
                    </>
                  )}
                </div>
              ) : null}

              {selectedId === sid ? (
                <div style={{ marginTop: 12 }}>
                  <TrustSnapshotPanel sessionId={sid} />
                </div>
              ) : null}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
