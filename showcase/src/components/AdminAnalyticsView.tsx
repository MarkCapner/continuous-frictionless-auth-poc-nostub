import React, { useEffect, useState } from "react";
import type { AdminRiskStatsRow, AdminSessionDailyStatsRow, AdminModelConfusionRow } from "../api";
import { fetchRiskStats, fetchSessionDailyStats, fetchModelConfusion } from "../api";

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
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    void run();
    return () => { cancelled = true; };
  }, []);

  const maxSessions = dailyStats.reduce((m, d) => Math.max(m, d.sessions), 0) || 1;

  return (
    <div className="stack">
      <div className="pageHeader">
        <div>
          <h2>Admin / Analytics</h2>
          <p>
            Operational overview: daily sessions, risk breakdown, and model confusion (decision vs label).
          </p>
        </div>
        <div />
      </div>

      {loading && <p className="muted">Loading analytics…</p>}
      {error && (
        <div className="card cardDanger">
          <div className="textDanger">Error: {error}</div>
        </div>
      )}

      {!loading && !error && (
        <div className="stack">
          <div className="grid2">
            <div className="card">
              <div className="cardTitle">
                <h3>Daily sessions (last 30d)</h3>
                <span className="muted">{dailyStats.length} days</span>
              </div>
              {dailyStats.length === 0 && <p className="muted">No session data yet.</p>}
              {dailyStats.length > 0 && (
                <div className="stack" style={{ gap: 8 }}>
                  {dailyStats.map((d) => {
                    const pct = (d.sessions / maxSessions) * 100;
                    return (
                      <div key={d.day} style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div className="muted" style={{ width: 96 }}>{d.day}</div>
                        <div className="barBg">
                          <div className="barFill" style={{ width: `${pct}%`, minWidth: pct > 0 ? 4 : 0 }} />
                        </div>
                        <div className="barValue">{d.sessions}</div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="card">
              <div className="cardTitle">
                <h3>Risk breakdown</h3>
                <span className="muted">totals + recency</span>
              </div>

              {riskStats.length === 0 && <p className="muted">No decision data yet.</p>}
              {riskStats.length > 0 && (
                <div className="tableWrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Decision</th>
                        <th>Total</th>
                        <th>Avg conf</th>
                        <th>24h</th>
                        <th>7d</th>
                      </tr>
                    </thead>
                    <tbody>
                      {riskStats.map((r) => (
                        <tr key={r.decision}>
                          <td className="mono">{r.decision}</td>
                          <td>{r.total}</td>
                          <td className="mono">{Number(r.avgConfidence).toFixed(3)}</td>
                          <td>{r.last24h}</td>
                          <td>{r.last7d}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          <div className="card">
            <div className="cardTitle">
              <h3>Model confusion</h3>
              <span className="muted">decision vs label</span>
            </div>
            {confusion.length === 0 && <p className="muted">No confusion data yet.</p>}
            {confusion.length > 0 && (
              <div className="tableWrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Decision</th>
                      <th>Label</th>
                      <th>Sessions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {confusion.map((c, idx) => (
                      <tr key={`${c.decision}-${c.label ?? "null"}-${idx}`}>
                        <td className="mono">{c.decision}</td>
                        <td className="mono">{c.label ?? "—"}</td>
                        <td>{c.sessions}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
