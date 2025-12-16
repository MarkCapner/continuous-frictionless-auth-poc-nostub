import React from "react";

type Props = {
  title: string;
  hint?: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
};

export function ExpandablePanel({ title, hint, defaultOpen, children }: Props) {
  return (
    <details className="details" open={defaultOpen}>
      <summary>
        <div>
          <div className="detailsSummaryTitle">{title}</div>
          {hint ? <div className="detailsSummaryHint">{hint}</div> : null}
        </div>
        <span className="chip">Details</span>
      </summary>
      <div className="detailsBody">{children}</div>
    </details>
  );
}
