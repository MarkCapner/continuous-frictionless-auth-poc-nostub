import React, { useEffect, useMemo, useState } from "react";
import type { PolicyRule, PolicyRuleUpsert, PolicyScope, PolicySimulationRequest, PolicySimulationResult } from "../api";
import {
  fetchPolicyRules,
  createPolicyRule,
  updatePolicyRule,
  deletePolicyRule,
  setPolicyEnabled,
  fetchEffectivePolicies,
  simulatePolicy
} from "../api";
import { JsonOptIn } from "../ui/JsonOptIn";

function safeParseJson(text: string): { ok: true; value: any } | { ok: false; error: string } {
  try {
    const trimmed = (text ?? "").trim();
    if (!trimmed) return { ok: true, value: {} };
    return { ok: true, value: JSON.parse(trimmed) };
  } catch (e: any) {
    return { ok: false, error: e?.message || "Invalid JSON" };
  }
}

function asPrettyJson(v: any): string {
  try {
    return JSON.stringify(v ?? {}, null, 2);
  } catch {
    return "{}";
  }
}

type EditorState = {
  id?: number;
  scope: PolicyScope;
  scopeRef: string | null;
  priority: number;
  enabled: boolean;
  description: string | null;
  conditionText: string;
  actionText: string;
};

function toEditor(p?: PolicyRule): EditorState {
  if (!p) {
    return {
      scope: "GLOBAL",
      scopeRef: null,
      priority: 100,
      enabled: true,
      description: "",
      conditionText: "{\n  \n}",
      actionText: "{\n  \n}"
    };
  }
  return {
    id: p.id,
    scope: p.scope,
    scopeRef: p.scopeRef ?? null,
    priority: p.priority,
    enabled: p.enabled,
    description: p.description ?? "",
    conditionText: asPrettyJson(p.conditionJson),
    actionText: asPrettyJson(p.actionJson)
  };
}

function toUpsert(e: EditorState): { ok: true; value: PolicyRuleUpsert } | { ok: false; error: string } {
  const c = safeParseJson(e.conditionText);
  if (!c.ok) return { ok: false, error: `Condition JSON: ${c.error}` };
  const a = safeParseJson(e.actionText);
  if (!a.ok) return { ok: false, error: `Action JSON: ${a.error}` };

  return {
    ok: true,
    value: {
      scope: e.scope,
      scopeRef: e.scopeRef,
      priority: e.priority,
      enabled: e.enabled,
      description: e.description ?? null,
      conditionJson: c.value,
      actionJson: a.value
    }
  };
}

export function AdminPolicyView() {
  const [policies, setPolicies] = useState<PolicyRule[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [editor, setEditor] = useState<EditorState | null>(null);
  const [editorError, setEditorError] = useState<string | null>(null);

  const [effectiveTenant, setEffectiveTenant] = useState("");
  const [effectiveUser, setEffectiveUser] = useState("");
  const [effective, setEffective] = useState<PolicyRule[] | null>(null);

  const [simulateSessionId, setSimulateSessionId] = useState("");
  const [simulateTenant, setSimulateTenant] = useState("");
  const [simulateUser, setSimulateUser] = useState("");
  const [simulateResult, setSimulateResult] = useState<PolicySimulationResult | null>(null);
  const [simulateError, setSimulateError] = useState<string | null>(null);

  const sortedPolicies = useMemo(() => {
    return [...policies].sort((a, b) => (b.priority ?? 0) - (a.priority ?? 0));
  }, [policies]);

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const list = await fetchPolicyRules();
      setPolicies(list);
    } catch (e: any) {
      setError(e?.message || "Failed to load policy rules");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function onSave() {
    if (!editor) return;
    setEditorError(null);
    setSimulateResult(null);
    setSimulateError(null);

    const up = toUpsert(editor);
    if (!up.ok) {
      setEditorError(up.error);
      return;
    }

    try {
      if (editor.id != null) {
        await updatePolicyRule(editor.id, up.value);
      } else {
        await createPolicyRule(up.value);
      }
      setEditor(null);
      await refresh();
    } catch (e: any) {
      setEditorError(e?.message || "Failed to save policy");
    }
  }

  async function onDelete(id: number) {
    if (!confirm(`Delete policy ${id}?`)) return;
    try {
      await deletePolicyRule(id);
      await refresh();
    } catch (e: any) {
      setError(e?.message || "Failed to delete policy");
    }
  }

  async function onToggleEnabled(p: PolicyRule) {
    try {
      await setPolicyEnabled(p.id, !p.enabled);
      await refresh();
    } catch (e: any) {
      setError(e?.message || "Failed to update enabled flag");
    }
  }

  async function loadEffective() {
    setEffective(null);
    setError(null);
    try {
      const tenantId = effectiveTenant.trim() || undefined;
      const userId = effectiveUser.trim() || undefined;
      const res = await fetchEffectivePolicies(tenantId, userId);
      setEffective(res);
    } catch (e: any) {
      setError(e?.message || "Failed to load effective policies");
    }
  }

  async function runSimulate() {
    setSimulateError(null);
    setSimulateResult(null);

    if (!editor) {
      setSimulateError("Open a policy in the editor first.");
      return;
    }
    const sessionId = simulateSessionId.trim();
    if (!sessionId) {
      setSimulateError("Enter a sessionId / request_id to simulate against.");
      return;
    }

    const up = toUpsert(editor);
    if (!up.ok) {
      setSimulateError(up.error);
      return;
    }

    const req: PolicySimulationRequest = {
      sessionId,
      tenantId: simulateTenant.trim() || undefined,
      userId: simulateUser.trim() || undefined,
      conditionJson: up.value.conditionJson,
      actionJson: up.value.actionJson
    };

    try {
      const res = await simulatePolicy(req);
      setSimulateResult(res);
    } catch (e: any) {
      setSimulateError(e?.message || "Simulation failed");
    }
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <div className="card">
        <div className="cardTitle">
          <h3>Admin / Policy</h3>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn" onClick={() => setEditor(toEditor())}>
              New policy
            </button>
            <button className="btn btnGhost" onClick={refresh} disabled={loading}>
              {loading ? "Refreshing..." : "Refresh"}
            </button>
          </div>
        </div>

        {error ? <div style={{ color: "salmon", marginTop: 8 }}>{error}</div> : null}

        <div style={{ overflowX: "auto", marginTop: 10 }}>
          <table style={{ width: "100%" }}>
            <thead>
              <tr>
                <th align="left">ID</th>
                <th align="left">Scope</th>
                <th align="left">Scope Ref</th>
                <th align="left">Priority</th>
                <th align="left">Enabled</th>
                <th align="left">Description</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {sortedPolicies.map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.scope}</td>
                  <td>{p.scopeRef ?? ""}</td>
                  <td>{p.priority}</td>
                  <td>{p.enabled ? "true" : "false"}</td>
                  <td>{p.description ?? ""}</td>
                  <td style={{ whiteSpace: "nowrap" }}>
                    <button className="btn btnGhost" onClick={() => setEditor(toEditor(p))}>
                      Edit
                    </button>{" "}
                    <button className="btn btnGhost" onClick={() => onToggleEnabled(p)}>
                      {p.enabled ? "Disable" : "Enable"}
                    </button>{" "}
                    <button className="btn btnDanger" onClick={() => onDelete(p.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {sortedPolicies.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ padding: 10, opacity: 0.8 }}>
                    No policy rules yet.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <div className="cardTitle">
          <h3>Effective policies preview</h3>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <input
              className="input"
              placeholder="tenant_id (optional)"
              value={effectiveTenant}
              onChange={(e) => setEffectiveTenant(e.target.value)}
            />
            <input
              className="input"
              placeholder="user_id (optional)"
              value={effectiveUser}
              onChange={(e) => setEffectiveUser(e.target.value)}
            />
            <button className="btn" onClick={loadEffective}>
              Load
            </button>
          </div>
        </div>

        {effective ? (
          <details>
            <summary style={{ cursor: "pointer" }}>Show raw JSON</summary>
            <div style={{ marginTop: 10 }}>
              <JsonOptIn title="Effective policies (raw JSON)" data={effective} />
            </div>
          </details>
        ) : (
          <div style={{ opacity: 0.8 }}>Enter tenant/user ids to preview the resolved policy order.</div>
        )}
      </div>

      {editor ? (
        <div className="card">
          <div className="cardTitle">
            <h3>{editor.id != null ? `Edit policy #${editor.id}` : "Create policy"}</h3>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="btn" onClick={onSave}>
                Save
              </button>
              <button
                className="btn btnGhost"
                onClick={() => {
                  setEditor(null);
                  setEditorError(null);
                  setSimulateResult(null);
                  setSimulateError(null);
                }}
              >
                Close
              </button>
            </div>
          </div>

          {editorError ? <div style={{ color: "salmon", marginTop: 8 }}>{editorError}</div> : null}

          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <div style={{ display: "flex", flexDirection: "column", gap: 6, minWidth: 220 }}>
              <label style={{ fontSize: 12, opacity: 0.85 }}>Scope</label>
              <select
                className="input"
                value={editor.scope}
                onChange={(e) => setEditor({ ...editor, scope: e.target.value })}
              >
                <option value="GLOBAL">GLOBAL</option>
                <option value="TENANT">TENANT</option>
                <option value="USER">USER</option>
              </select>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 6, minWidth: 260 }}>
              <label style={{ fontSize: 12, opacity: 0.85 }}>Scope Ref</label>
              <input
                className="input"
                placeholder="tenant_id / user_id (blank for GLOBAL)"
                value={editor.scopeRef ?? ""}
                onChange={(e) => setEditor({ ...editor, scopeRef: e.target.value || null })}
              />
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 6, minWidth: 160 }}>
              <label style={{ fontSize: 12, opacity: 0.85 }}>Priority</label>
              <input
                className="input"
                type="number"
                value={editor.priority}
                onChange={(e) => setEditor({ ...editor, priority: Number(e.target.value) })}
              />
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 6, minWidth: 160 }}>
              <label style={{ fontSize: 12, opacity: 0.85 }}>Enabled</label>
              <select
                className="input"
                value={editor.enabled ? "true" : "false"}
                onChange={(e) => setEditor({ ...editor, enabled: e.target.value === "true" })}
              >
                <option value="true">true</option>
                <option value="false">false</option>
              </select>
            </div>
          </div>

          <div style={{ marginTop: 10 }}>
            <label style={{ fontSize: 12, opacity: 0.85 }}>Description</label>
            <input
              className="input"
              value={editor.description ?? ""}
              onChange={(e) => setEditor({ ...editor, description: e.target.value })}
            />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginTop: 12 }}>
            <div>
              <div style={{ fontSize: 12, opacity: 0.85, marginBottom: 6 }}>Condition JSON</div>
              <textarea
                className="input"
                style={{ width: "100%", minHeight: 220, fontFamily: "monospace" }}
                value={editor.conditionText}
                onChange={(e) => setEditor({ ...editor, conditionText: e.target.value })}
              />
            </div>

            <div>
              <div style={{ fontSize: 12, opacity: 0.85, marginBottom: 6 }}>Action JSON</div>
              <textarea
                className="input"
                style={{ width: "100%", minHeight: 220, fontFamily: "monospace" }}
                value={editor.actionText}
                onChange={(e) => setEditor({ ...editor, actionText: e.target.value })}
              />
            </div>
          </div>

          <div style={{ marginTop: 12 }}>
            <div style={{ fontSize: 12, opacity: 0.85, marginBottom: 6 }}>
              Simulate this draft policy on a historical session (sessionId / request_id)
            </div>
            <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
              <input
                className="input"
                style={{ minWidth: 320 }}
                placeholder="sessionId / request_id"
                value={simulateSessionId}
                onChange={(e) => setSimulateSessionId(e.target.value)}
              />
              <input
                className="input"
                style={{ minWidth: 200 }}
                placeholder="tenantId (optional)"
                value={simulateTenant}
                onChange={(e) => setSimulateTenant(e.target.value)}
              />
              <input
                className="input"
                style={{ minWidth: 200 }}
                placeholder="userId (optional)"
                value={simulateUser}
                onChange={(e) => setSimulateUser(e.target.value)}
              />
              <button className="btn" onClick={runSimulate}>
                Simulate
              </button>
              {simulateError ? <span style={{ color: "salmon" }}>{simulateError}</span> : null}
            </div>

            {simulateResult ? (
              <details style={{ marginTop: 10 }}>
                <summary style={{ cursor: "pointer" }}>Show raw JSON</summary>
                <div style={{ marginTop: 10 }}>
                  <JsonOptIn title="Simulation result (raw JSON)" data={simulateResult} />
                </div>
              </details>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
