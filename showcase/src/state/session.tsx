import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { SessionSummary } from "../api";
import { fetchSessions } from "../api";

type SessionContextValue = {
  userHint: string;
  setUserHint: (v: string) => void;

  selectedSessionId: string | null;
  setSelectedSessionId: (v: string | null) => void;

  sessions: SessionSummary[];
  sessionsLoading: boolean;
  sessionsError: string | null;
  refreshSessions: () => Promise<void>;
};

const SessionContext = createContext<SessionContextValue | null>(null);

const LS_USER_HINT = "cfa.userHint";
const LS_SESSION_ID = "cfa.selectedSessionId";

export function SessionProvider(props: { children: React.ReactNode }) {
  const [userHint, setUserHintState] = useState<string>(() => {
    try {
      return localStorage.getItem(LS_USER_HINT) || "demo-user";
    } catch {
      return "demo-user";
    }
  });

  const [selectedSessionId, setSelectedSessionIdState] = useState<string | null>(() => {
    try {
      return localStorage.getItem(LS_SESSION_ID) || null;
    } catch {
      return null;
    }
  });

  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [sessionsError, setSessionsError] = useState<string | null>(null);

  const setUserHint = (v: string) => {
    setUserHintState(v);
    try {
      localStorage.setItem(LS_USER_HINT, v);
    } catch {
      // ignore
    }
  };

  const setSelectedSessionId = (v: string | null) => {
    setSelectedSessionIdState(v);
    try {
      if (v) localStorage.setItem(LS_SESSION_ID, v);
      else localStorage.removeItem(LS_SESSION_ID);
    } catch {
      // ignore
    }
  };

  const refreshSessions = async () => {
    const hint = (userHint || "demo-user").trim() || "demo-user";
    setSessionsLoading(true);
    setSessionsError(null);
    try {
      const data = await fetchSessions(hint, 60);
      setSessions(data);

      // If the currently selected session isn't in the list anymore, keep it (it may belong to another user),
      // but if there's no selection, default to the newest.
      if (!selectedSessionId && data.length > 0) {
        setSelectedSessionId(data[0].sessionId);
      }
    } catch (e: any) {
      setSessionsError(e?.message ?? String(e));
      setSessions([]);
    } finally {
      setSessionsLoading(false);
    }
  };

  useEffect(() => {
    void refreshSessions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userHint]);

  const value = useMemo<SessionContextValue>(
    () => ({
      userHint,
      setUserHint,
      selectedSessionId,
      setSelectedSessionId,
      sessions,
      sessionsLoading,
      sessionsError,
      refreshSessions
    }),
    [userHint, selectedSessionId, sessions, sessionsLoading, sessionsError]
  );

  return <SessionContext.Provider value={value}>{props.children}</SessionContext.Provider>;
}

export function useSessionContext(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSessionContext must be used within SessionProvider");
  return ctx;
}
