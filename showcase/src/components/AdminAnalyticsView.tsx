import React, { useEffect, useState } from "react";
import type {
  AdminRiskStatsRow,
  AdminSessionDailyStatsRow,
  AdminModelConfusionRow
} from "../api";
import {
  fetchRiskStats,
  fetchSessionDailyStats,
  fetchModelConfusion
} from "../api";

export function AdminAnalyticsView() {
  const [riskStats, setRiskStats] = useState<AdminRiskStatsRow[]>([]);
  const [dailyStats, setDailyStats] = useState<AdminSessionDailyStatsRow[]>([]);
  const [confusion, setConfusion] = useState<AdminModelConfusionRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const [risk, daily, conf] = await Promise.all([
          fetchRiskStats(),
          fetchSessionDailyStats(30),
          fetchModelConfusion()
        ]);
        if (!cancelled) {
          setRiskStats(risk);
          setDailyStats(daily);
          setConfusion(conf);
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
  }, []);

  const maxSessions = dailyStats.reduce((m, d) => Math.max(m, d.sessions), 0) || 1;

  return (
    <div style={containerStyle}>
      <section style={sectionStyle}>
        <h2>Daily sessions</h2>
        <p style={hintStyle}>
          Aggregated from <code>v_session_daily_stats</code>. Bars show total sessions per day with breakdown fields
          available for more advanced charts.
        </p>
        {loading && <p style={hintStyle}>Loading analytics…</p>}
        {error && <p style={{ ...hintStyle, color: "#b91c1c" }}>Error: {error}</p>}
        {!loading && dailyStats.length === 0 && !error && (
          <p style={hintStyle}>No session data yet. Run a few profile checks first.</p>
        )}
        {!loading && dailyStats.length > 0 && (
          <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem" }}>
            {dailyStats.map((d) => {
              const pct = (d.sessions / maxSessions) * 100;
              return (
                <div key={d.day} style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                  <div style={{ width: 90, fontSize: "0.8rem", color: "#4b5563" }}>{d.day}</div>
                  <div style={{ flex: 1, background: "#e5e7eb", borderRadius: 999, overflow: "hidden" }}>
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
                  <div style={{ width: 80, textAlign: "right", fontSize: "0.8rem" }}>{d.sessions} sess</div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      <section style={sectionStyle}>
        <h2>Risk breakdown</h2>
        <p style={hintStyle}>
          Aggregated from <code>v_risk_decision_stats</code>. Shows how often each decision outcome is used and the
          average confidence, including activity in the last 24 hours and 7 days.
        </p>
        {!loading && riskStats.length === 0 && !error && (
          <p style={hintStyle}>No risk stats yet.</p>
        )}
        {!loading && riskStats.length > 0 && (
          <table style={tableStyle}>
            <thead>
              <tr>
                <th>Decision</th>
                <th>Total</th>
                <th>Avg conf</th>
                <th>Last 24h</th>
                <th>Last 7d</th>
              </tr>
            </thead>
            <tbody>
              {riskStats.map((r) => (
                <tr key={r.decision}>
                  <td>{r.decision}</td>
                  <td>{r.total}</td>
                  <td>{r.avgConfidence.toFixed(2)}</td>
                  <td>{r.last24h}</td>
                  <td>{r.last7d}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section style={sectionStyle}>
        <h2>Model confusion matrix</h2>
        <p style={hintStyle}>
          Built from <code>v_model_confusion</code>. This is a simple confusion matrix over labeled sessions – useful
          for spotting systematic errors such as over-aggressive <code>DENY</code> decisions.
        </p>
        {!loading && confusion.length === 0 && !error && (
          <p style={hintStyle}>No labeled sessions yet.</p>
        )}
        {!loading && confusion.length > 0 && (
          <table style={tableStyle}>
            <thead>
              <tr>
                <th>Decision</th>
                <th>Label</th>
                <th>Sessions</th>
              </tr>
            </thead>
            <tbody>
              {confusion.map((c, idx) => (
                <tr key={idx}>
                  <td>{c.decision}</td>
                  <td>{c.label ?? "∅"}</td>
                  <td>{c.sessions}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  gap: "1.25rem",
  maxWidth: 1100,
  margin: "0 auto"
};

const sectionStyle: React.CSSProperties = {
  borderRadius: 8,
  borderWidth: 1,
  borderStyle: "solid",
  borderColor: "#e5e7eb",
  background: "#fff",
  padding: "0.75rem 1rem"
};

const hintStyle: React.CSSProperties = {
  fontSize: "0.8rem",
  color: "#6b7280",
  margin: "0.35rem 0"
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.85rem"
};
