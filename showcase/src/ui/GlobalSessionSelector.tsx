import { useMemo, useState } from "react";
import { useSessionContext } from "../state/session";

/**
 * Global session selector shown in the Shell topbar.
 * - Works across Showcase / Analyst / Admin views
 * - Persists selected session + user hint in localStorage via SessionProvider
 */
export function GlobalSessionSelector() {
  const {
    userHint,
    setUserHint,
    selectedSessionId,
    setSelectedSessionId,
    sessions,
    sessionsLoading,
    sessionsError,
    refreshSessions
  } = useSessionContext();

  const [manualSession, setManualSession] = useState<string>("");

  const options = useMemo(() => {
    const seen = new Set<string>();
    const list = [] as { id: string; label: string }[];
    for (const s of sessions) {
      const sid = (s.sessionId || "").toString();
      if (!sid || seen.has(sid)) continue;
      seen.add(sid);
      const when = s.createdAt ? new Date(s.createdAt).toLocaleString() : "";
      list.push({ id: sid, label: `${sid.slice(0, 8)}… · ${s.decision} · ${(s.confidence * 100).toFixed(1)}% · ${when}` });
    }
    return list;
  }, [sessions]);

  return (
    <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap", justifyContent: "flex-end" }}>
      <div style={{ display: "flex", flexDirection: "column", gap: 4, minWidth: 220 }}>
        <div className="muted" style={{ fontSize: 11 }}>Global session selector</div>
        <input
          className="input"
          style={{ height: 32, padding: "0 10px" }}
          value={userHint}
          onChange={(e) => setUserHint(e.target.value)}
          placeholder="user handle (user_id_hint)"
          title="Used to load recent sessions"
        />
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: 4, minWidth: 340 }}>
        <div className="muted" style={{ fontSize: 11 }}>Recent sessions</div>
        <select
          className="input"
          style={{ height: 32, padding: "0 10px" }}
          value={selectedSessionId ?? ""}
          onChange={(e) => setSelectedSessionId(e.target.value || null)}
          disabled={sessionsLoading || options.length === 0}
        >
          <option value="" disabled>
            {sessionsLoading ? "Loading sessions…" : options.length === 0 ? "No sessions yet" : "Select a session"}
          </option>
          {options.map((o) => (
            <option key={o.id} value={o.id}>
              {o.label}
            </option>
          ))}
        </select>
      </div>

      <button className="btn" onClick={() => void refreshSessions()} disabled={sessionsLoading} title="Refresh sessions">
        {sessionsLoading ? "…" : "Refresh"}
      </button>

      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <input
          className="input"
          style={{ height: 32, padding: "0 10px", width: 180 }}
          value={manualSession}
          onChange={(e) => setManualSession(e.target.value)}
          placeholder="paste session id"
          title="Manually set a session id"
        />
        <button
          className="btn"
          onClick={() => {
            const sid = manualSession.trim();
            if (sid) {
              setSelectedSessionId(sid);
              setManualSession("");
            }
          }}
          disabled={!manualSession.trim()}
        >
          Set
        </button>
      </div>

      {sessionsError ? <span className="chip chipDanger">{sessionsError}</span> : null}
    </div>
  );
}
