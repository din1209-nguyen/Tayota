import { getSpecificationRows } from "@/lib/vehicle-labels";

export default function SpecificationTable({ specification }) {
  const rows = getSpecificationRows(specification);

  if (!rows.length) {
    return <div className="status-box">Chưa có thông số kỹ thuật cho phiên bản này.</div>;
  }

  const groups = rows.reduce((result, row) => {
    if (!result[row.group]) result[row.group] = [];
    result[row.group].push(row);
    return result;
  }, {});

  return (
    <div className="spec-card-grid">
      {Object.entries(groups).map(([group, groupRows]) => (
        <section className="spec-group-card" key={group}>
          <h3>{group}</h3>
          <dl>
            {groupRows.map((row) => (
              <div key={row.key}>
                <dt>{row.label}</dt>
                <dd>{row.value}</dd>
              </div>
            ))}
          </dl>
        </section>
      ))}
    </div>
  );
}
