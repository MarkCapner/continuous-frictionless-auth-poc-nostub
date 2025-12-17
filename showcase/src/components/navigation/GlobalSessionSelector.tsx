
export function GlobalSessionSelector({ sessions, onSelect }: any) {
  return (
    <select onChange={e => onSelect(e.target.value)}>
      {sessions.map((s: any) => (
        <option key={s.id} value={s.id}>
          Session {s.id}
        </option>
      ))}
    </select>
  );
}
