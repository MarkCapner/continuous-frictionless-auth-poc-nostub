import React, { useEffect, useState } from "react";
import type { BehaviorBaseline } from "../api";
import { fetchBehaviorBaselines } from "../api";

export function AdminBehaviorView() {
  const [rows, setRows] = useState<BehaviorBaseline[]>([]);
  const [selected, setSelected] = useState<BehaviorBaseline | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchBehaviorBaselines(200);
        if (!cancelled) {
          setRows(data);
          if (!selected && data.length > 0) {
            setSelected(data[0]);
          }
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e.message ?? String(e));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    run();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div style={pageStyle}>
      <header style={headerStyle}>
        <div>
          <h1 style={{ margin: 0 }}>Admin / Behaviour baselines</h1>
          <p style={{ margin: 0, fontSize: "0.9rem", color: "#4b5563" }}>
            Per-user behavioural baselines derived from keystroke timing, scroll cadence and pointer dynamics.
            Each row comes from <code>behavior_profile_stats</code>.
          </p>
        </div>
      </header>

      <div style={gridStyle}>
        <div style={tableCardStyle}>
          <h2 style={{ marginTop: 0 }}>Baselines</h2>
          {loading && <p>Loading baselines…</p>}
          {error && (
            <p style={{ color: "red" }}>
              Failed to load baselines: {error}
            </p>
          )}
          {!loading && !error && rows.length === 0 && (
            <p style={{ fontSize: "0.9rem", color: "#6b7280" }}>
              No behavioural baselines yet. Generate some sessions via the Showcase view first.
            </p>
          )}
          {!loading && !error && rows.length > 0 && (
            <div style={{ maxHeight: 420, overflow: "auto", borderRadius: 8, border: "1px solid #e5e7eb" }}>
              <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.85rem" }}>
                <thead>
                  <tr>
                    <th style={thStyle}>User</th>
                    <th style={thStyle}>Feature</th>
                    <th style={thStyle}>Mean</th>
                    <th style={thStyle}>Std dev</th>
                    <th style={thStyle}>Decay</th>
                    <th style={thStyle}>Updated</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row, idx) => (
                    <tr
                      key={idx}
                      style={{
                        ...trStyle,
                        background:
                          selected && selected.userId === row.userId && selected.feature === row.feature
                            ? "#eef2ff"
                            : "transparent"
                      }}
                      onClick={() => setSelected(row)}
                    >
                      <td style={tdStyle}>
                        <code>{row.userId}</code>
                      </td>
                      <td style={tdStyle}>
                        <code>{row.feature}</code>
                      </td>
                      <td style={tdStyle}>{row.mean.toFixed(3)}</td>
                      <td style={tdStyle}>{row.stdDev.toFixed(3)}</td>
                      <td style={tdStyle}>{row.decay.toFixed(2)}</td>
                      <td style={tdStyle}>
                        {new Date(row.updatedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div style={detailCardStyle}>
          {!selected && (
            <p style={{ fontSize: "0.9rem", color: "#6b7280" }}>
              Select a baseline row on the left to see details.
            </p>
          )}
          {selected && (
            <>
              <h2 style={{ marginTop: 0 }}>Baseline details</h2>
              <p style={{ fontSize: "0.9rem" }}>
                <strong>User:</strong> <code>{selected.userId}</code>
                <br />
                <strong>Feature:</strong> <code>{selected.feature}</code>
              </p>
              <ul style={{ fontSize: "0.9rem" }}>
                <li>
                  Mean: <strong>{selected.mean.toFixed(3)}</strong>
                </li>
                <li>
                  Std dev: <strong>{selected.stdDev.toFixed(3)}</strong>
                </li>
                <li>
                  Variance: <strong>{selected.variance.toFixed(3)}</strong>
                </li>
                <li>
                  Decay (EMA factor): <strong>{selected.decay.toFixed(2)}</strong>
                </li>
                <li>
                  Last updated: {new Date(selected.updatedAt).toLocaleString()}
                </li>
              </ul>
              <p style={{ fontSize: "0.85rem", color: "#4b5563" }}>
                For each session, we compute a z-score for this feature as{" "}
                <code>(value - mean) / stdDev</code>. A large absolute z-score means the behaviour
                for that session is unusual compared to this baseline. These per-feature z-scores
                are exposed in the <code>feature_vector</code> JSON as{" "}
                <code>behavior_z_{"{feature}"}</code>.
              </p>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

const pageStyle: React.CSSProperties = {
  maxWidth: 1100,
  margin: "0 auto",
  padding: "0.5rem 0 1.5rem 0"
};

const headerStyle: React.CSSProperties = {
  marginBottom: "1rem"
};

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1.7fr 1.3fr",
  gap: "1rem",
  alignItems: "flex-start"
};

const tableCardStyle: React.CSSProperties = {
  background: "#ffffff",
  borderRadius: 12,
  padding: "0.75rem",
  boxShadow: "0 10px 15px -3px rgba(15, 23, 42, 0.07)",
  border: "1px solid #e5e7eb"
};

const detailCardStyle: React.CSSProperties = {
  background: "#ffffff",
  borderRadius: 12,
  padding: "0.75rem",
  boxShadow: "0 10px 15px -3px rgba(15, 23, 42, 0.07)",
  border: "1px solid #e5e7eb",
  minHeight: 260
};

const thStyle: React.CSSProperties = {
  textAlign: "left",
  fontWeight: 600,
  fontSize: "0.75rem",
  padding: "0.25rem 0.35rem",
  borderBottom: "1px solid #e5e7eb",
  position: "sticky",
  top: 0,
  background: "#f9fafb",
  zIndex: 1
};

const trStyle: React.CSSProperties = {
  cursor: "pointer"
};

const tdStyle: React.CSSProperties = {
  padding: "0.25rem 0.35rem",
  borderBottom: "1px solid #f3f4f6",
  verticalAlign: "top"
};
