
export function SessionNarrative({ text }: { text: string }) {
  return (
    <div style={{ padding: 16 }}>
      <h3>What happened?</h3>
      <p>{text}</p>
    </div>
  );
}
