
export function UserTransparencyPanel({ message }: { message: string }) {
  return (
    <div style={{ padding: 16 }}>
      <h3>Why was I asked?</h3>
      <p>{message}</p>
    </div>
  );
}
