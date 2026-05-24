import { apiFetch, buildQuery } from "@/lib/api";
import { unwrapList } from "@/lib/format";
import VehicleCard from "@/components/vehicles/VehicleCard";
import VehicleFilters from "@/components/vehicles/VehicleFilters";

async function getVehicles(searchParams) {
  const query = buildQuery({ ...searchParams, page: searchParams.page || 0, size: 12 });
  try {
    const data = await apiFetch(`/car/catalog/car-versions${query}`, { cache: "no-store" });
    return { vehicles: unwrapList(data), error: null };
  } catch (error) {
    return { vehicles: [], error: error.message };
  }
}

export default async function VehiclesPage({ searchParams }) {
  const params = await searchParams;
  const { vehicles, error } = await getVehicles(params || {});

  return (
    <section className="section catalog-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Catalogue</p>
        <h1>Dòng xe TAYOTA</h1>
      </div>
      <div className="shell-container catalog-layout">
        <VehicleFilters searchParams={params} />
        <div className="vehicle-grid catalog-grid">
          {error ? <div className="status-box wide">{error}</div> : null}
          {!error && vehicles.length === 0 ? (
            <div className="status-box wide">Không tìm thấy xe phù hợp với bộ lọc hiện tại.</div>
          ) : null}
          {vehicles.map((vehicle, index) => (
            <VehicleCard key={vehicle?.id || vehicle?.carVersionId || index} vehicle={vehicle} />
          ))}
        </div>
      </div>
    </section>
  );
}
