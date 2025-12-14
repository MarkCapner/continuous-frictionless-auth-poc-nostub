import React, { useEffect, useState } from "react";
import type { UserSummary } from "../api";
import { fetchUserSummaries } from "../api";

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
                <td>{(u.avgConfidence * 100).toFixed(1)}%</td>
                <td>{new Date(u.lastSeen).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
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
