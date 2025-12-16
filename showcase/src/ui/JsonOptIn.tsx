import React, { useMemo, useState } from "react";

type Props = {
  title?: string;
  /** Preferred prop */
  value?: unknown;
  /** Back-compat alias (older components used `data`) */
  data?: unknown;
  defaultOpen?: boolean;
};

function safeStringify(value: unknown) {
  try {
    if (typeof value === "string") {
      // If it looks like JSON, pretty print it.
      const t = value.trim();
      if ((t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))) {
        return JSON.stringify(JSON.parse(value), null, 2);
      }
      return value;
    }
    return JSON.stringify(value ?? {}, null, 2);
  } catch {
    return typeof value === "string" ? value : String(value);
  }
}

export function JsonOptIn({ title = "Raw JSON", value, data, defaultOpen }: Props) {
  const [open, setOpen] = useState(Boolean(defaultOpen));
  const payload = value !== undefined ? value : data;
  const text = useMemo(() => safeStringify(payload), [payload]);

  return (
    <div>
      <div className="jsonToggleRow">
        <div className="muted" style={{ fontSize: 12 }}>{title} (opt-in)</div>
        <button className="btn" type="button" onClick={() => setOpen((v) => !v)}>
          {open ? "Hide" : "Show"}
        </button>
      </div>
      {open ? (
        <div className="jsonBlock">
          <pre style={{ margin: 0, fontSize: 12, whiteSpace: "pre-wrap" }}>{text}</pre>
        </div>
      ) : null}
    </div>
  );
}
