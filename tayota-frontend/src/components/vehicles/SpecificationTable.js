import { getSpecificationRows } from "@/lib/vehicle-labels";

export default function SpecificationTable({ specification }) {
  const rows = getSpecificationRows(specification);

  if (!rows.length) {
    return <div className="status-box">Chưa có thông số kỹ thuật cho phiên bản này.</div>;
  }

  return (
    <dl className="spec-table">
      {rows.map((row) => (
        <div key={row.key}>
          <dt>{row.label}</dt>
          <dd>{row.value}</dd>
        </div>
      ))}
    </dl>
  );
}
