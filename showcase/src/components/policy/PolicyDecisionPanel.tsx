
export function PolicyDecisionPanel({ decision }: any) {
  return (
    <div>
      <h3>Policy Decision</h3>
      <p>Action: {decision.action}</p>
      <p>{decision.reason}</p>
    </div>
  );
}
