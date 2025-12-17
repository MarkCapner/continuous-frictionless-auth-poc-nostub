
export function SessionInvestigationPanel({ data }: any) {
  return (
    <div>
      <h3>Session Investigation</h3>
      <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>
  );
}
