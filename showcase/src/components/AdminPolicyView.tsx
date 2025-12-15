import React, { useEffect, useMemo, useState } from "react";
import {
  PolicyRule,
  PolicyRuleUpsert,
  PolicyScope,
  createPolicyRule,
  deletePolicyRule,
  fetchEffectivePolicies,
  fetchPolicyRules,
  setPolicyEnabled,
  updatePolicyRule,
} from "../api";

function prettyJson(v: any): string {
  try {
    return JSON.stringify(v ?? {}, null, 2);
  } catch {
    return "{}";
  }
}

function parseJson(text: string): { ok: true; value: any } | { ok: false; error: string } {
  try {
    if (!text || !text.trim()) return { ok: true, value: {} };
    return { ok: true, value: JSON.parse(text) };
  } catch (e: any) {
    return { ok: false, error: e?.message ?? "Invalid JSON" };
  }
}

const sectionStyle: React.CSSProperties = {
  border: "1px solid rgba(255,255,255,0.12)",
  borderRadius: 12,
  padding: 14,
  background: "rgba(255,255,255,0.04)",
};

const tableStyle: React.CSSProperties = { width: "100%", borderCollapse: "collapse" };
const thtd: React.CSSProperties = {
  textAlign: "left",
  padding: "8px 6px",
  borderBottom: "1px solid rgba(255,255,255,0.12)",
  verticalAlign: "top",
};

export function AdminPolicyView() {
  const [rules, setRules] = useState<PolicyRule[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const selected = useMemo(() => rules.find((r) => r.id === selectedId) ?? null, [rules, selectedId]);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  // editor fields
  const [scope, setScope] = useState<PolicyScope>("GLOBAL");
  const [scopeRef, setScopeRef] = useState<string>("");
  const [priority, setPriority] = useState<string>("100");
  const [enabled, setEnabled] = useState<boolean>(true);
  const [description, setDescription] = useState<string>("");
  const [conditionText, setConditionText] = useState<string>("{}\n");
  const [actionText, setActionText] = useState<string>("{}\n");

  // effective policies preview
  const [effTenant, setEffTenant] = useState<string>("");
  const [effUser, setEffUser] = useState<string>("");
  const [effective, setEffective] = useState<PolicyRule[] | null>(null);

  function setFromRule(r: PolicyRule) {
    setScope(r.scope);
    setScopeRef(r.scopeRef ?? "");
    setPriority(String(r.priority));
    setEnabled(Boolean(r.enabled));
    setDescription(r.description ?? "");
    setConditionText(prettyJson(r.conditionJson) + "\n");
    setActionText(prettyJson(r.actionJson) + "\n");
  }

  function clearEditor() {
    setSelectedId(null);
    setScope("GLOBAL");
    setScopeRef("");
    setPriority("100");
    setEnabled(true);
    setDescription("");
    setConditionText("{}\n");
    setActionText("{}\n");
  }

  async function refresh() {
    setErr(null);
    setNotice(null);
    const list = await fetchPolicyRules();
    // backend should already be ordered; keep stable ordering in UI
    setRules(list);
  }

  useEffect(() => {
    refresh().catch((e) => setErr(e?.message ?? String(e)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (selected) setFromRule(selected);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId]);

  async function save() {
    setErr(null);
    setNotice(null);
    const cond = parseJson(conditionText);
    if (!cond.ok) return setErr(`Condition JSON invalid: ${cond.error}`);
    const act = parseJson(actionText);
    if (!act.ok) return setErr(`Action JSON invalid: ${act.error}`);

    const pr = Number(priority);
    if (!Number.isFinite(pr)) return setErr("Priority must be a number");

    const req: PolicyRuleUpsert = {
      scope,
      scopeRef: scope === "GLOBAL" ? null : (scopeRef.trim() || null),
      priority: pr,
      enabled,
      description: description.trim() ? description.trim() : null,
      conditionJson: cond.value,
      actionJson: act.value,
    };

    setBusy(true);
    try {
      if (selectedId == null) {
        const created = await createPolicyRule(req);
        setNotice(`Created policy #${created.id}`);
        await refresh();
        setSelectedId(created.id);
      } else {
        await updatePolicyRule(selectedId, req);
        setNotice(`Updated policy #${selectedId}`);
        await refresh();
      }
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setBusy(false);
    }
  }

  async function toggleRule(id: number, newEnabled: boolean) {
    setBusy(true);
    setErr(null);
    setNotice(null);
    try {
      await setPolicyEnabled(id, newEnabled);
      await refresh();
      if (selectedId === id) {
        const updated = rules.find((r) => r.id === id);
        if (updated) setEnabled(newEnabled);
      }
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setBusy(false);
    }
  }

  async function remove(id: number) {
    if (!confirm(`Delete policy #${id}?`)) return;
    setBusy(true);
    setErr(null);
    setNotice(null);
    try {
      await deletePolicyRule(id);
      setNotice(`Deleted policy #${id}`);
      await refresh();
      if (selectedId === id) clearEditor();
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setBusy(false);
    }
  }

  async function loadEffective() {
    setBusy(true);
    setErr(null);
    setNotice(null);
    try {
      const list = await fetchEffectivePolicies(effTenant.trim() || undefined, effUser.trim() || undefined);
      setEffective(list);
    } catch (e: any) {
      setErr(e?.message ?? String(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ display: "grid", gap: 14 }}>
      <div style={sectionStyle}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
          <div>
            <h2 style={{ margin: 0 }}>Admin / Policy</h2>
            <div style={{ opacity: 0.8, marginTop: 4 }}>
              EPIC 13 · Policy rules stored in Postgres (scope + priority) with deterministic evaluation.
            </div>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <button onClick={() => refresh()} disabled={busy}>Refresh</button>
            <button onClick={() => clearEditor()} disabled={busy}>New policy</button>
            <button onClick={save} disabled={busy}>{selectedId == null ? "Create" : "Save"}</button>
          </div>
        </div>

        {err ? (
          <div style={{ marginTop: 10, padding: 10, borderRadius: 10, border: "1px solid rgba(255,80,80,0.6)" }}>
            Error: {err}
          </div>
        ) : null}
        {notice ? (
          <div style={{ marginTop: 10, padding: 10, borderRadius: 10, border: "1px solid rgba(120,255,120,0.35)" }}>
            {notice}
          </div>
        ) : null}
      </div>

      <div className="grid2" style={{ gap: 14 }}>
        <div style={sectionStyle}>
          <h3 style={{ marginTop: 0 }}>Policies</h3>
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thtd}>id</th>
                <th style={thtd}>scope</th>
                <th style={thtd}>ref</th>
                <th style={thtd}>priority</th>
                <th style={thtd}>enabled</th>
                <th style={thtd}>description</th>
                <th style={thtd}></th>
              </tr>
            </thead>
            <tbody>
              {rules.map((r) => (
                <tr key={r.id} style={{ opacity: r.enabled ? 1 : 0.6 }}>
                  <td style={thtd}>
                    <button
                      className="btn btnGhost"
                      style={{ padding: "4px 8px" }}
                      onClick={() => setSelectedId(r.id)}
                    >
                      #{r.id}
                    </button>
                  </td>
                  <td style={thtd}><span className="mono">{r.scope}</span></td>
                  <td style={thtd}><span className="mono">{r.scopeRef ?? "—"}</span></td>
                  <td style={thtd}><span className="mono">{r.priority}</span></td>
                  <td style={thtd}>
                    <button
                      className={r.enabled ? "btn btnPrimary" : "btn"}
                      style={{ padding: "4px 8px" }}
                      onClick={() => toggleRule(r.id, !r.enabled)}
                      disabled={busy}
                    >
                      {r.enabled ? "On" : "Off"}
                    </button>
                  </td>
                  <td style={thtd}>{r.description ?? ""}</td>
                  <td style={thtd}>
                    <button className="btn btnDanger" style={{ padding: "4px 8px" }} onClick={() => remove(r.id)} disabled={busy}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 10, opacity: 0.75, fontSize: 12 }}>
            Effective policy order at runtime: <b>USER → TENANT → GLOBAL</b>, within each scope by <b>priority (desc)</b>.
          </div>
        </div>

        <div style={sectionStyle}>
          <h3 style={{ marginTop: 0 }}>{selectedId == null ? "Create policy" : `Edit policy #${selectedId}`}</h3>

          <div className="grid2Equal" style={{ gap: 12 }}>
            <label style={{ display: "block" }}>
              <span className="muted">Scope</span>
              <select className="input" value={scope} onChange={(e) => setScope(e.target.value as PolicyScope)}>
                <option value="GLOBAL">GLOBAL</option>
                <option value="TENANT">TENANT</option>
                <option value="USER">USER</option>
              </select>
              <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
                GLOBAL applies to everyone. TENANT/USER require a scope ref.
              </div>
            </label>

            <label style={{ display: "block" }}>
              <span className="muted">Scope ref</span>
              <input
                className="input"
                value={scopeRef}
                onChange={(e) => setScopeRef(e.target.value)}
                placeholder={scope === "TENANT" ? "tenant-id" : scope === "USER" ? "user-id" : "(none)"}
                disabled={scope === "GLOBAL"}
              />
            </label>

            <label style={{ display: "block" }}>
              <span className="muted">Priority</span>
              <input className="input" value={priority} onChange={(e) => setPriority(e.target.value)} />
              <div className="muted" style={{ fontSize: 12, marginTop: 6 }}>
                Higher priority wins within the same scope.
              </div>
            </label>

            <label style={{ display: "block" }}>
              <span className="muted">Enabled</span>
              <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
                <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
                <span className="mono">{enabled ? "true" : "false"}</span>
              </div>
            </label>

            <label style={{ display: "block", gridColumn: "1 / -1" }}>
              <span className="muted">Description</span>
              <input className="input" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Human-readable reason" />
            </label>
          </div>

          <div style={{ marginTop: 12 }}>
            <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>
              Condition JSON (examples: <span className="mono">{"{"} "device.new": true {"}"}</span>, <span className="mono">{"{"} "risk.score": {"{"}"gt":0.7{"}"} {"}"}</span>)
            </div>
            <textarea
              className="input"
              style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace", minHeight: 140 }}
              value={conditionText}
              onChange={(e) => setConditionText(e.target.value)}
            />
          </div>

          <div style={{ marginTop: 12 }}>
            <div className="muted" style={{ fontSize: 12, marginBottom: 6 }}>
              Action JSON (examples: <span className="mono">{"{"} "decision": "STEP_UP", "confidence_cap": 0.6, "reason": "…" {"}"}</span>)
            </div>
            <textarea
              className="input"
              style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace", minHeight: 140 }}
              value={actionText}
              onChange={(e) => setActionText(e.target.value)}
            />
          </div>

          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 12 }}>
            <button onClick={save} disabled={busy}>{selectedId == null ? "Create" : "Save"}</button>
            {selectedId != null ? (
              <button onClick={() => toggleRule(selectedId, !enabled)} disabled={busy}>
                {enabled ? "Disable" : "Enable"}
              </button>
            ) : null}
          </div>
        </div>
      </div>

      <div style={sectionStyle}>
        <h3 style={{ marginTop: 0 }}>Effective policies preview</h3>
        <div className="grid2Equal" style={{ gap: 12, alignItems: "end" }}>
          <label style={{ display: "block" }}>
            <span className="muted">Tenant id (optional)</span>
            <input className="input" value={effTenant} onChange={(e) => setEffTenant(e.target.value)} placeholder="tenant-1" />
          </label>
          <label style={{ display: "block" }}>
            <span className="muted">User id (optional)</span>
            <input className="input" value={effUser} onChange={(e) => setEffUser(e.target.value)} placeholder="demo-user" />
          </label>
          <div>
            <button onClick={loadEffective} disabled={busy}>Load effective policies</button>
          </div>
        </div>

        {effective ? (
          <div style={{ marginTop: 12 }}>
            <table style={tableStyle}>
              <thead>
                <tr>
                  <th style={thtd}>scope</th>
                  <th style={thtd}>id</th>
                  <th style={thtd}>priority</th>
                  <th style={thtd}>enabled</th>
                  <th style={thtd}>description</th>
                </tr>
              </thead>
              <tbody>
                {effective.map((r) => (
                  <tr key={r.id} style={{ opacity: r.enabled ? 1 : 0.6 }}>
                    <td style={thtd}><span className="mono">{r.scope}</span></td>
                    <td style={thtd}><span className="mono">#{r.id}</span></td>
                    <td style={thtd}><span className="mono">{r.priority}</span></td>
                    <td style={thtd}><span className="mono">{String(r.enabled)}</span></td>
                    <td style={thtd}>{r.description ?? ""}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div style={{ marginTop: 10, opacity: 0.75, fontSize: 12 }}>
              This list reflects the scope resolution order used by the policy engine.
            </div>
          </div>
        ) : (
          <div style={{ marginTop: 10, opacity: 0.75 }}>
            Enter tenant/user to preview which policies would be considered.
          </div>
        )}
      </div>
    </div>
  );
}
