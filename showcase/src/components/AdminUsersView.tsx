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
        const data = await fetchAdminUsers(100);
        if (!cancelled) {
          setUsers(data);
          if (!selectedUserId && data.length > 0) setSelectedUserId(data[0].userId);
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setLoadingList(false);
      }
    };
    void run();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!selectedUserId) return;
    let cancelled = false;
    const run = async () => {
      setDetail(null);
      setLoadingDetail(true);
      setError(null);
      try {
        const d = await fetchAdminUserDetail(selectedUserId);
        if (!cancelled) setDetail(d);
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? String(e));
      } finally {
        if (!cancelled) setLoadingDetail(false);
      }
    };
    void run();
    return () => { cancelled = true; };
  }, [selectedUserId]);

  return (
    <div className="stack">
      <div className="pageHeader">
        <div>
          <h2>Admin / Users</h2>
          <p>
            Aggregates <span className="mono">decision_log</span> by user and overlays user-level trust
            and account-sharing heuristics. Select a user to view the breakdown.
          </p>
        </div>
        <div />
      </div>

      {error && (
        <div className="card cardDanger">
          <div className="muted textDanger">Error: {error}</div>
        </div>
      )}

      <div className="grid2">
        <div className="card">
          <div className="cardTitle">
            <h3>Users</h3>
            <span className="muted">{users.length} total</span>
          </div>

          {loadingList && <p className="muted">Loading users…</p>}
          {!loadingList && users.length === 0 && (
            <p className="muted">No users yet. Generate some traffic via the Showcase first.</p>
          )}

          {users.length > 0 && (
            <div className="tableWrap" style={{ maxHeight: 420 }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>User</th>
                    <th>Sessions</th>
                    <th>Devices</th>
                    <th>Avg conf</th>
                    <th>Trust</th>
                    <th>Sharing</th>
                    <th>Last seen</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    const active = u.userId === selectedUserId;
                    return (
                      <tr
                        key={u.userId}
                        onClick={() => setSelectedUserId(u.userId)}
                        className={`rowBtn ${active ? "rowActive" : ""}`}
                      >
                        <td className="mono">{u.userId}</td>
                        <td>{u.sessions}</td>
                        <td>{u.devices}</td>
                        <td className="mono">{Number(u.avgConfidence).toFixed(3)}</td>
                        <td className="mono">{Number(u.userTrustScore).toFixed(2)}</td>
                        <td className="mono">{Number(u.userAccountSharingRisk).toFixed(2)}</td>
                        <td className="muted">{u.lastSeen ? new Date(u.lastSeen).toLocaleString() : "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <div className="cardTitle">
            <h3>User detail</h3>
            <span className="muted">{selectedUserId ? <span className="mono">{selectedUserId}</span> : "—"}</span>
          </div>

          {!selectedUserId && <p className="muted">Select a user to view detail.</p>}
          {selectedUserId && loadingDetail && <p className="muted">Loading detail…</p>}
          {selectedUserId && !loadingDetail && !detail && <p className="muted">No detail returned.</p>}

          {detail && (
            <div className="stack" style={{ gap: 12 }}>
              <div className="grid2Tight">
                <div className="card cardFlat">
                  <div className="muted">Trust score</div>
                  <div style={{ fontSize: 22, fontWeight: 700 }}>{detail.reputation.trustScore.toFixed(2)}</div>
                </div>
                <div className="card cardFlat">
                  <div className="muted">Sharing risk</div>
                  <div style={{ fontSize: 22, fontWeight: 700 }}>{detail.reputation.accountSharingRisk.toFixed(2)}</div>
                </div>
              </div>

              <div className="card cardFlat">
                <div className="muted" style={{ marginBottom: 8 }}>Reputation signals</div>
                <KV label="Device count" value={String(detail.reputation.deviceCount)} />
                <KV label="TLS fingerprint count" value={String(detail.reputation.tlsFingerprintCount)} />
                <KV label="Country count" value={String(detail.reputation.countryCount)} />
                <KV label="Avg confidence (recent)" value={detail.reputation.avgConfidenceRecent.toFixed(3)} mono />
                <KV label="Sessions (last 30d)" value={String(detail.reputation.sessionsLast30d)} />
              </div>

              <div className="card cardFlat">
                <div className="muted" style={{ marginBottom: 8 }}>Sharing heuristic</div>
                <KV label="Suspicious" value={detail.sharing.suspicious ? "Yes" : "No"} />
                <KV label="TLS fingerprint count" value={String(detail.sharing.tlsFingerprintCount)} />
                <KV label="Country count" value={String(detail.sharing.countryCount)} />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function KV(props: { label: string; value: string; mono?: boolean }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", gap: 14, marginBottom: 6 }}>
      <span className="muted">{props.label}</span>
      <span className={props.mono ? "mono" : ""}>{props.value}</span>
    </div>
  );
}
