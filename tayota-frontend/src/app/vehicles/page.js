import { apiFetch, buildQuery } from "@/lib/api";
import { unwrapList } from "@/lib/format";
import { filterVehicleItems, groupVehiclesBySeries } from "@/lib/vehicle-filters";
import VehicleFilters from "@/components/vehicles/VehicleFilters";
import VehicleSeriesCard from "@/components/vehicles/VehicleSeriesCard";

function normalizeParam(value) {
  return Array.isArray(value) ? value[0] : value || "";
}

function parsePriceRange(value) {
  const normalized = normalizeParam(value);
  if (!normalized) return {};
  const [minPrice, maxPrice] = normalized.split("-");
  return {
    minPrice: minPrice || "",
    maxPrice: maxPrice || "",
  };
}

async function getVehicles(searchParams) {
  const priceRange = parsePriceRange(searchParams.priceRange);
  const filters = {
    keyword: searchParams.versionKeyword || searchParams.keyword,
    styleId: searchParams.styleId,
    seriesId: searchParams.seriesId,
    modelYear: searchParams.modelYear,
    minPrice: priceRange.minPrice || searchParams.minPrice,
    maxPrice: priceRange.maxPrice || searchParams.maxPrice,
  };
  const localFilters = {
    ...searchParams,
    ...priceRange,
  };
  try {
    const firstPage = await apiFetch(`/car/catalog/car-versions${buildQuery({ ...filters, page: 0, size: 50 })}`, { cache: "no-store" });
    const totalPages = Number(firstPage?.totalPages || 1);
    const nextPages = totalPages > 1
      ? await Promise.all(
          Array.from({ length: totalPages - 1 }, (_, index) =>
            apiFetch(`/car/catalog/car-versions${buildQuery({ ...filters, page: index + 1, size: 50 })}`, { cache: "no-store" })
          )
        )
      : [];
    const vehicles = [firstPage, ...nextPages].flatMap((page) => unwrapList(page));
    return { vehicles: filterVehicleItems(vehicles, localFilters), error: null };
  } catch (error) {
    return { vehicles: [], error: error.message };
  }
}

async function getStyles() {
  try {
    const data = await apiFetch("/car/catalog/car-styles-with-versions", { cache: "no-store" });
    return unwrapList(data);
  } catch {
    return [];
  }
}

export default async function VehiclesPage({ searchParams }) {
  const params = (await searchParams) || {};
  const [{ vehicles, error }, styles] = await Promise.all([getVehicles(params || {}), getStyles()]);
  const seriesGroups = groupVehiclesBySeries(vehicles);

  return (
    <section className="section catalog-page">
      <div className="shell-container page-title catalog-title">
        <p className="eyebrow">Catalogue</p>
        <h1>Dòng xe Tayota</h1>
        <p>Chọn kiểu dáng, khám phá từng dòng xe và mở phiên bản phù hợp hành trình của bạn.</p>
      </div>
      <div className="shell-container catalog-layout">
        <VehicleFilters searchParams={params} styles={styles} />
        <div className="series-grid catalog-grid">
          {error ? <div className="status-box wide">{error}</div> : null}
          {!error && vehicles.length === 0 ? <div className="status-box wide">Không tìm thấy xe phù hợp với bộ lọc hiện tại.</div> : null}
          {seriesGroups.map((group) => (
            <VehicleSeriesCard group={group} key={group.id} />
          ))}
        </div>
      </div>
    </section>
  );
}
