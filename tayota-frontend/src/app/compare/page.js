import { apiFetch } from "@/lib/api";
import { getVehicleId, getVehicleName, unwrapList } from "@/lib/format";
import { getCompareGroups } from "@/lib/vehicle-labels";
import ComparePicker from "@/components/vehicles/ComparePicker";

async function getCompare(ids) {
  if (!ids.length) return { vehicles: [], error: null };
  try {
    const query = ids.map((id) => `ids=${encodeURIComponent(id)}`).join("&");
    const data = await apiFetch(`/car/catalog/car-versions/compare?${query}`, { cache: "no-store" });
    return { vehicles: unwrapList(data), error: null };
  } catch (error) {
    return { vehicles: [], error: error.message };
  }
}

export default async function ComparePage({ searchParams }) {
  const params = await searchParams;
  const ids = Array.isArray(params?.ids) ? params.ids : params?.ids ? [params.ids] : [];
  const selectedIds = ids.slice(0, 3);
  const { vehicles, error } = await getCompare(selectedIds);

  return (
    <section className="compare-page">
      <ComparePicker selectedIds={selectedIds} selectedVehicles={vehicles} />

      <div className="shell-container compare-results">
        {error ? <div className="status-box">{error}</div> : null}
        {vehicles.length ? (
          <div className="compare-specifications">
            <h2>Thông số đối chiếu</h2>
            {getCompareGroups(vehicles[0] || {}).map((group) => (
              <details className="compare-group" key={group.title} open={group.title === "Tổng quan"}>
                <summary>{group.title}</summary>
                <div className="compare-row header" style={{ "--compare-columns": vehicles.length }}>
                  <span>Tiêu chí</span>
                  {vehicles.map((vehicle) => <strong key={getVehicleId(vehicle)}>{getVehicleName(vehicle)}</strong>)}
                </div>
                {group.rows.map(([label]) => (
                  <div className="compare-row" style={{ "--compare-columns": vehicles.length }} key={`${group.title}-${label}`}>
                    <span>{label}</span>
                    {vehicles.map((vehicle) => {
                      const matchingGroup = getCompareGroups(vehicle).find((item) => item.title === group.title);
                      const value = matchingGroup?.rows.find(([rowLabel]) => rowLabel === label)?.[1];
                      return <strong key={`${getVehicleId(vehicle)}-${label}`}>{value || "Đang cập nhật"}</strong>;
                    })}
                  </div>
                ))}
              </details>
            ))}
          </div>
        ) : null}
      </div>
    </section>
  );
}
