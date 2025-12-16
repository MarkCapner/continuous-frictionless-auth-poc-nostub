import React, { useEffect, useState } from "react";
import type { UserSummary } from "../api";
import { fetchUserSummaries } from "../api";
import { SummaryCards } from "../ui/SummaryCards";
import { ExpandablePanel } from "../ui/ExpandablePanel";

export function UsersOverview() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchUserSummaries(20);
        if (!cancelled) {
          setUsers(data);
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
    <div style={cardStyle}>
      <h2>Demo users overview</h2>
      <p style={{ fontSize: "0.85rem", marginTop: 0, marginBottom: "0.5rem" }}>
        Each handle here comes from <code>user_id_hint</code>. Use this to see how many sessions and devices
        each demo user has generated.
      </p>
      {loading && <p>Loading users…</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}
      {!loading && !error && users.length === 0 && (
        <p style={{ fontSize: "0.9rem" }}>No sessions yet. Run a profile check to populate this table.</p>
      )}
      {!loading && !error && users.length > 0 && (
        <div>
          <SummaryCards
            cards={[
              { label: "Users", value: users.length, hint: "handles observed" },
              { label: "Sessions", value: users.reduce((a, u) => a + (u.sessions ?? 0), 0), hint: "total" },
              { label: "Devices", value: users.reduce((a, u) => a + (u.devices ?? 0), 0), hint: "total" },
              {
                label: "Avg conf",
                value: users.length ? ((users.reduce((a, u) => a + (u.avgConfidence ?? 0), 0) / users.length) * 100).toFixed(1) + "%" : "—",
                hint: "mean",
              }
            ]}
          />

          <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 10 }}>
            {users.slice(0, 6).map((u) => (
              <div key={u.userId} className="summaryCard" style={{ padding: "0.55rem 0.7rem" }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 10, alignItems: "center" }}>
                  <div className="mono" style={{ fontWeight: 700 }}>{u.userId}</div>
                  <div style={{ display: "flex", gap: 6, flexWrap: "wrap", justifyContent: "flex-end" }}>
                    <span className="chip">sessions · {u.sessions}</span>
                    <span className="chip">devices · {u.devices}</span>
                  </div>
                </div>
                <ExpandablePanel title="Details" hint="confidence + last seen" defaultOpen={false}>
                  <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                      <span className="muted">Avg confidence</span>
                      <span className="muted">{((u.avgConfidence ?? 0) * 100).toFixed(1)}%</span>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}>
                      <span className="muted">Last seen</span>
                      <span className="muted">{u.lastSeen ? new Date(u.lastSeen).toLocaleString() : "—"}</span>
                    </div>
                  </div>
                </ExpandablePanel>
              </div>
            ))}
          </div>

          <ExpandablePanel title="Show full table" hint={`All rows (${users.length})`} defaultOpen={false}>
            <table style={tableStyle}>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Sessions</th>
                  <th>Devices</th>
                  <th>Avg confidence</th>
                  <th>Last seen</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.userId}>
                    <td>{u.userId}</td>
                    <td>{u.sessions}</td>
                    <td>{u.devices}</td>
                    <td>{((u.avgConfidence ?? 0) * 100).toFixed(1)}%</td>
                    <td>{u.lastSeen ? new Date(u.lastSeen).toLocaleString() : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </ExpandablePanel>
        </div>
      )}
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "var(--panel)",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  minWidth: 260,
  flex: 1
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.85rem"
};
