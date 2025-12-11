import React, { useEffect, useState } from "react";
import type { AdminUserSummary, AdminUserDetail } from "../api";
import { fetchAdminUsers, fetchAdminUserDetail } from "../api";

export function AdminUsersView() {
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [detail, setDetail] = useState<AdminUserDetail | null>(null);
  const [loadingList, setLoadingList] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoadingList(true);
      setError(null);
      try {
        const data = await fetchAdminUsers(50);
        if (!cancelled) {
          setUsers(data);
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e?.message ?? String(e));
        }
      } finally {
        if (!cancelled) {
          setLoadingList(false);
        }
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleSelectUser(userId: string) {
    setSelectedUserId(userId);
    setDetail(null);
    setLoadingDetail(true);
    setError(null);
    try {
      const d = await fetchAdminUserDetail(userId);
      setDetail(d);
    } catch (e: any) {
      setError(e?.message ?? String(e));
    } finally {
      setLoadingDetail(false);
    }
  }

  return (
    <div style={containerStyle}>
      <section style={leftPaneStyle}>
        <h2>Admin / Users</h2>
        <p style={{ fontSize: "0.85rem", color: "#4b5563" }}>
          This view aggregates <code>decision_log</code> by user and overlays the user-level reputation
          and account sharing heuristics. Click a row to see the detailed reputation breakdown.
        </p>
        {error && (
          <p style={{ color: "#b91c1c", fontSize: "0.85rem" }}>
            Error: {error}
          </p>
        )}
        <div style={{ marginTop: "0.75rem" }}>
          {loadingList && <p style={{ fontSize: "0.85rem" }}>Loading users...</p>}
          {!loadingList && users.length === 0 && (
            <p style={{ fontSize: "0.85rem", color: "#6b7280" }}>
              No users yet. Generate some traffic via the showcase first.
            </p>
          )}
          {users.length > 0 && (
            <table style={tableStyle}>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Sessions</th>
                  <th>Devices</th>
                  <th>Avg conf</th>
                  <th>Trust</th>
                  <th>Sharing risk</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr
                    key={u.userId}
                    onClick={() => handleSelectUser(u.userId)}
                    style={
                      u.userId === selectedUserId
                        ? { ...rowStyle, background: "#eef2ff", cursor: "pointer" }
                        : { ...rowStyle, cursor: "pointer" }
                    }
                  >
                    <td>{u.userId}</td>
                    <td>{u.sessions}</td>
                    <td>{u.devices}</td>
                    <td>{u.avgConfidence.toFixed(2)}</td>
                    <td>{u.userTrustScore.toFixed(2)}</td>
                    <td>{u.userAccountSharingRisk.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section style={rightPaneStyle}>
        <h3>User reputation</h3>
        {!selectedUserId && (
          <p style={{ fontSize: "0.85rem", color: "#6b7280" }}>
            Select a user on the left to see their reputation breakdown.
          </p>
        )}
        {selectedUserId && loadingDetail && (
          <p style={{ fontSize: "0.85rem" }}>Loading reputation for <code>{selectedUserId}</code>...</p>
        )}
        {selectedUserId && !loadingDetail && detail && (
          <div style={detailCardStyle}>
            <p style={{ margin: 0 }}>
              <strong>User:</strong> <code>{detail.userId}</code>
            </p>
            <p style={{ margin: "0.25rem 0" }}>
              <strong>Trust score:</strong> {detail.reputation.trustScore.toFixed(2)}
            </p>
            <p style={{ margin: "0.25rem 0" }}>
              <strong>Account sharing risk:</strong> {detail.reputation.accountSharingRisk.toFixed(2)}{" "}
              {detail.sharing.suspicious && (
                <span style={{ color: "#b91c1c" }}>(suspicious)</span>
              )}
            </p>
            <p style={{ margin: "0.25rem 0" }}>
              <strong>Devices:</strong> {detail.reputation.deviceCount} | TLS fingerprints:{" "}
              {detail.reputation.tlsFingerprintCount} | Countries: {detail.reputation.countryCount}
            </p>
            <p style={{ margin: "0.25rem 0" }}>
              <strong>Sessions (30d):</strong> {detail.reputation.sessionsLast30d} | Recent avg confidence:{" "}
              {detail.reputation.avgConfidenceRecent.toFixed(2)}
            </p>
            <p style={{ marginTop: "0.75rem", fontSize: "0.8rem", color: "#4b5563" }}>
              Sharing heuristics: TLS fingerprints={detail.sharing.tlsFingerprintCount}, countries=
              {detail.sharing.countryCount}.
            </p>
          </div>
        )}
      </section>
    </div>
  );
}

const containerStyle: React.CSSProperties = {
  display: "flex",
  gap: "1.5rem",
  alignItems: "flex-start"
};

const leftPaneStyle: React.CSSProperties = {
  flex: 1.3,
  minWidth: 0
};

const rightPaneStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  fontSize: "0.85rem"
};

const rowStyle: React.CSSProperties = {
  borderBottom: "1px solid #e5e7eb"
};

const detailCardStyle: React.CSSProperties = {
  borderRadius: 8,
  border: "1px solid #e5e7eb",
  padding: "0.75rem 1rem",
  background: "#f9fafb",
  fontSize: "0.85rem"
};
