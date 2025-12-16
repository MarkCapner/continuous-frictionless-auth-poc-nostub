import React from "react";

export type SummaryCard = {
  label: string;
  value: React.ReactNode;
  hint?: string;
  right?: React.ReactNode;
  danger?: boolean;
};

export function SummaryCards({ cards }: { cards: SummaryCard[] }) {
  return (
    <div className="cardsGrid">
      {cards.map((c) => (
        <div key={c.label} className={`card cardFlat ${c.danger ? "cardDanger" : ""}`.trim()}>
          <div className="cardTitle">
            <div>
              <div className="summaryLabel">{c.label}</div>
              <div className="summaryValue">{c.value}</div>
            </div>
            {c.right}
          </div>
          {c.hint ? <div className="muted" style={{ fontSize: 12 }}>{c.hint}</div> : null}
        </div>
      ))}
    </div>
  );
}
