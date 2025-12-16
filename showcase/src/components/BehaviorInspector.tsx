import React, { useEffect, useState } from "react";
import type { BehaviorHistoryItem } from "../api";
import { fetchBehaviorHistory } from "../api";
import { JsonOptIn } from "../ui/JsonOptIn";
import { ExpandablePanel } from "../ui/ExpandablePanel";

export interface BehaviorInspectorProps {
  userHint: string;
}

/**
 * Mini-Epic 8D: Behavioural inspector wiring.
 *
 * This component fetches recent behavioural history for the selected user and shows:
 * - a small decision timeline (AUTO_LOGIN / STEP_UP / DENY)
 * - a table of recent sessions
 * - details for the selected session, including a skim of behavioural features
 */
export function BehaviorInspector({ userHint }: BehaviorInspectorProps) {
  const [items, setItems] = useState<BehaviorHistoryItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      const hint = (userHint || "demo-user").trim() || "demo-user";
      setLoading(true);
      setError(null);
      setItems([]);
      setSelectedId(null);
      try {
        const data = await fetchBehaviorHistory(hint, 50);
        if (!cancelled) {
          setItems(data);
          if (data.length > 0) {
            setSelectedId(data[0].id);
          }
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

  const selected = selectedId != null ? items.find((i) => i.id === selectedId) ?? null : null;

  return (
    <div style={cardStyle}>
      <h3 style={titleStyle}>Behavioural inspector</h3>
      <p style={bodyStyle}>
        Recent sessions for <code>{(userHint || "demo-user").trim() || "demo-user"}</code>. This view focuses on the
        behavioural feature vector and how it aligns with decisions over time.
      </p>

      {loading && <p style={hintStyle}>Loading behavioural history…</p>}
      {error && (
        <p style={{ ...hintStyle, color: "#b91c1c" }}>
          Error loading behaviour history: {error}
        </p>
      )}

      {!loading && !error && items.length === 0 && (
        <p style={hintStyle}>No behavioural sessions yet. Run a few profile checks first.</p>
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div style={{ marginTop: "0.5rem", marginBottom: "0.5rem" }}>
            <div style={timelineLabelStyle}>Decision timeline (most recent on the left)</div>
            <div style={timelineRowStyle}>
              {items.map((item) => {
                const colour = decisionColour(item.decision);
                const isSelected = item.id === selectedId;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setSelectedId(item.id)}
                    style={{
                      ...timelineDotStyle,
                      background: colour,
                      opacity: isSelected ? 1 : 0.7,
                      outline: isSelected ? "2px solid var(--accent)" : "none"
                    }}
                    title={`${item.decision} – ${(item.confidence * 100).toFixed(1)}% at ${formatDateTime(
                      item.occurredAt
                    )}`}
                  />
                );
              })}
            </div>
          </div>

          <div style={twoColLayoutStyle}>
            <div style={leftColStyle}>
              <h4 style={subTitleStyle}>Recent sessions</h4>

              {/* Summary cards first (default) */}
              <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 6 }}>
                {items.slice(0, 8).map((item) => {
                  const active = item.id === selectedId;
                  return (
                    <div
                      key={item.id}
                      onClick={() => setSelectedId(item.id)}
                      style={{
                        border: "1px solid var(--border)",
                        borderRadius: 8,
                        padding: "0.5rem 0.6rem",
                        cursor: "pointer",
                        background: active ? "rgba(99,102,241,0.10)" : "rgba(255,255,255,0.03)"
                      }}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                          <span style={{ fontSize: "0.8rem", color: "#6b7280" }}>{formatTime(item.occurredAt)}</span>
                          <DecisionTag decision={item.decision} />
                          <span style={{ fontSize: "0.8rem" }}>{(item.confidence * 100).toFixed(1)}%</span>
                        </div>
                        <span style={{ fontSize: "0.75rem", color: "#6b7280" }}>{shortenFp(item.tlsFp)}</span>
                      </div>

                      <ExpandablePanel title="Details" hint="TLS FP, label, timestamp" defaultOpen={false}>
                        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                          <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                            <span style={{ fontSize: "0.8rem", color: "#6b7280" }}>Occurred</span>
                            <span style={{ fontSize: "0.8rem" }}>{formatDateTime(item.occurredAt)}</span>
                          </div>
                          <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                            <span style={{ fontSize: "0.8rem", color: "#6b7280" }}>TLS FP</span>
                            <span style={{ fontSize: "0.8rem" }}><code>{item.tlsFp || "n/a"}</code></span>
                          </div>
                          {item.label ? (
                            <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                              <span style={{ fontSize: "0.8rem", color: "#6b7280" }}>Label</span>
                              <span style={{ fontSize: "0.8rem" }}><code>{item.label}</code></span>
                            </div>
                          ) : null}
                        </div>
                      </ExpandablePanel>
                    </div>
                  );
                })}
              </div>

              {/* Dense table becomes opt-in */}
              <ExpandablePanel title="Show table" hint="Exact rows (top 25)" defaultOpen={false}>
                <div style={{ marginTop: "0.5rem" }}>
                  <table style={tableStyle}>
                    <thead>
                      <tr>
                        <th>When</th>
                        <th>Decision</th>
                        <th>Conf.</th>
                        <th>TLS FP</th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.slice(0, 25).map((item) => (
                        <tr
                          key={item.id}
                          style={item.id === selectedId ? selectedRowStyle : undefined}
                          onClick={() => setSelectedId(item.id)}
                        >
                          <td>{formatTime(item.occurredAt)}</td>
                          <td>
                            <DecisionTag decision={item.decision} />
                          </td>
                          <td>{(item.confidence * 100).toFixed(1)}%</td>
                          <td style={{ fontSize: "0.75rem" }}>{shortenFp(item.tlsFp)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </ExpandablePanel>
            </div>

            <div style={rightColStyle}>
              <h4 style={subTitleStyle}>Session detail</h4>
              {!selected && <p style={hintStyle}>Select a session from the timeline or table.</p>}
              {selected && (
                <div>
                  <p style={bodyStyle}>
                    <strong>Decision:</strong>{" "}
                    <DecisionTag decision={selected.decision} /> ·{" "}
                    <strong>{(selected.confidence * 100).toFixed(1)}%</strong> confidence
                  </p>
                  <p style={bodyStyle}>
                    <strong>Occurred:</strong> {formatDateTime(selected.occurredAt)}
                  </p>
                  <p style={bodyStyle}>
                    <strong>TLS FP:</strong> <code>{selected.tlsFp}</code>
                  </p>
                  {selected.label && (
                    <p style={bodyStyle}>
                      <strong>Label:</strong> <code>{selected.label}</code>
                    </p>
                  )}

                  <h5 style={subSubTitleStyle}>Behavioural feature snapshot</h5>
                  <p style={hintStyle}>
                    Parsed from <code>behavior_json</code>. This is a skim of the key numeric features for quick visual
                    inspection.
                  </p>
                  <FeatureSummary behaviorJson={selected.behaviorJson} />

                  <div style={{ marginTop: "0.75rem" }}>
                    <JsonOptIn title="Raw behaviour_json" value={selected.behaviorJson ?? null} />
                  </div>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function decisionColour(decision: string): string {
  if (decision === "AUTO_LOGIN") return "#16a34a";
  if (decision === "STEP_UP") return "#f97316";
  if (decision === "DENY") return "#dc2626";
  return "#6b7280";
}

function shortenFp(fp: string | null | undefined): string {
  if (!fp) return "n/a";
  if (fp.length <= 12) return fp;
  return fp.slice(0, 6) + "…" + fp.slice(-4);
}

function formatDateTime(value: string): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    return value;
  }
  return d.toLocaleString();
}

function formatTime(value: string): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) {
    return value;
  }
  return d.toLocaleTimeString();
}

interface FeatureSummaryProps {
  behaviorJson: string | null;
}

function FeatureSummary({ behaviorJson }: FeatureSummaryProps) {
  if (!behaviorJson) {
    return <p style={bodyStyle}>No behavioural JSON stored for this session.</p>;
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(behaviorJson);
  } catch {
    return (
      <p style={bodyStyle}>
        Behaviour JSON is not valid JSON; raw value:
        <br />
        <code>{behaviorJson}</code>
      </p>
    );
  }

  if (typeof parsed !== "object" || parsed === null) {
    return <p style={bodyStyle}>Behaviour JSON has unexpected structure.</p>;
  }

  const entries: { key: string; value: number }[] = [];
  for (const [key, value] of Object.entries(parsed as Record<string, unknown>)) {
    if (typeof value === "number") {
      entries.push({ key, value });
    }
  }

  if (entries.length === 0) {
    return <p style={bodyStyle}>No numeric behavioural features found in this JSON payload.</p>;
  }

  const limited = entries.sort((a, b) => a.key.localeCompare(b.key)).slice(0, 12);
  const maxAbs = limited.reduce((m, e) => Math.max(m, Math.abs(e.value)), 0) || 1;

  return (
    <div style={{ marginTop: "0.25rem", display: "flex", flexDirection: "column", gap: "0.25rem" }}>
      {limited.map((e) => {
        const pct = (Math.abs(e.value) / maxAbs) * 100;
        return (
          <div key={e.key} style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <div style={{ width: 150, fontSize: "0.75rem", color: "#374151" }}>{e.key}</div>
            <div style={{ flex: 1, background: "rgba(255,255,255,0.08)", borderRadius: 999, overflow: "hidden" }}>
              <div
                style={{
                  width: `${pct}%`,
                  minWidth: pct > 0 ? "3px" : "0",
                  height: 8,
                  borderRadius: 999,
                  background: e.value >= 0 ? "#4f46e5" : "#dc2626"
                }}
              />
            </div>
            <div style={{ width: 80, textAlign: "right", fontSize: "0.75rem", color: "#374151" }}>
              {e.value.toFixed(3)}
            </div>
          </div>
        );
      })}
    </div>
  );
}

interface DecisionTagProps {
  decision: string;
}

function DecisionTag({ decision }: DecisionTagProps) {
  const colour = decisionColour(decision);
  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: "0.25rem",
        padding: "0.05rem 0.4rem",
        borderRadius: 999,
        borderWidth: 1,
        borderStyle: "solid",
        borderColor: "var(--border)",
        fontSize: "0.75rem",
        background: "rgba(255,255,255,0.04)"
      }}
    >
      <span
        style={{
          width: 8,
          height: 8,
          borderRadius: 999,
          background: colour
        }}
      />
      <span>{decision}</span>
    </span>
  );
}

const cardStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "var(--border)",
  background: "var(--panel)",
  padding: "0.75rem 1rem",
  minHeight: 220,
  display: "flex",
  flexDirection: "column"
};

const titleStyle: React.CSSProperties = {
  margin: "0 0 0.35rem"
};

const subTitleStyle: React.CSSProperties = {
  margin: "0.5rem 0 0.25rem",
  fontSize: "0.9rem"
};

const subSubTitleStyle: React.CSSProperties = {
  margin: "0.5rem 0 0.25rem",
  fontSize: "0.85rem"
};

const bodyStyle: React.CSSProperties = {
  margin: "0.25rem 0"
};

const hintStyle: React.CSSProperties = {
  margin: "0.25rem 0",
  fontSize: "0.8rem",
  color: "#6b7280"
};

const twoColLayoutStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "minmax(0, 1.1fr) minmax(0, 0.9fr)",
  gap: "0.75rem",
  marginTop: "0.5rem"
};

const leftColStyle: React.CSSProperties = {
  overflowX: "auto"
};

const rightColStyle: React.CSSProperties = {
  overflowX: "auto"
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.8rem"
};

const selectedRowStyle: React.CSSProperties = {
  background: "#eef2ff"
};

const timelineLabelStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  color: "#6b7280",
  marginBottom: "0.25rem"
};

const timelineRowStyle: React.CSSProperties = {
  display: "flex",
  flexWrap: "wrap",
  gap: "0.25rem"
};

const timelineDotStyle: React.CSSProperties = {
  width: 14,
  height: 14,
  borderRadius: 999,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "var(--border)",
  padding: 0,
  cursor: "pointer"
};
