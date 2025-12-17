
import { useState } from "react";

export function EvidenceCard({ title, summary, children }: any) {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ border: "1px solid #333", marginBottom: 8 }}>
      <div
        style={{ padding: 12, cursor: "pointer" }}
        onClick={() => setOpen(!open)}
      >
        <strong>{title}</strong> — {summary}
      </div>
      {open && <div style={{ padding: 12 }}>{children}</div>}
    </div>
  );
}
