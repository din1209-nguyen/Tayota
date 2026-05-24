export default function SpecificationTable({ specification }) {
  const entries = Object.entries(specification || {}).filter(
    ([, value]) => value !== null && value !== undefined && typeof value !== "object",
  );

  if (!entries.length) {
    return <div className="status-box">Chưa có thông số kỹ thuật cho phiên bản này.</div>;
  }

  return (
    <dl className="spec-table">
      {entries.map(([key, value]) => (
        <div key={key}>
          <dt>{key}</dt>
          <dd>{String(value)}</dd>
        </div>
      ))}
    </dl>
  );
}
