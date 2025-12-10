import { useEffect, useState } from "react";
import type { SessionSummary } from "../api";
import { fetchSessions } from "../api";

interface SessionTimelineProps {
  userHint: string;
}

export function SessionTimeline({ userHint }: SessionTimelineProps) {
  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchSessions(userHint, 20);
        if (!cancelled) {
          setSessions(data);
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
    const id = setInterval(run, 5000);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [userHint]);

  return (
    <div style={cardStyle}>
      <h2>Session timeline</h2>
      {loading && sessions.length === 0 && <p>Loading...</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}
      {sessions.length === 0 && !loading && !error && <p>No sessions yet.</p>}
      {sessions.length > 0 && (
        <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
          {sessions.map(s => (
            <li key={s.id} style={itemStyle}>
              <div>
                <strong>{s.decision}</strong>{" "}
                <span style={{ fontSize: "0.85rem", color: "#555" }}>
                  ({(s.confidence * 100).toFixed(1)}%)
                </span>
              </div>
              <div style={{ fontSize: "0.8rem", color: "#666" }}>
                {new Date(s.createdAt).toLocaleString()} — TLS FP: <code>{s.tlsFp}</code>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

const cardStyle = {
  border: "1px solid #ddd",
  borderRadius: 8,
  padding: "1rem",
  background: "#fff",
  boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
  minWidth: 260
};

const itemStyle = {
  padding: "0.5rem 0",
  borderBottom: "1px solid #eee"
};
