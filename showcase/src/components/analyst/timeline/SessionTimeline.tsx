
export function SessionTimeline({ sessions, onSelect }: any) {
  return (
    <div style={{ display: "flex", gap: 6, overflowX: "auto" }}>
      {sessions.map((s: any) => (
        <div
          key={s.id}
          style={{
            width: 10,
            height: 40,
            background: s.risk > 0.7 ? "#c33" : "#3c3",
            cursor: "pointer"
          }}
          title={`Risk ${s.risk}`}
          onClick={() => onSelect(s)}
        />
      ))}
    </div>
  );
}
