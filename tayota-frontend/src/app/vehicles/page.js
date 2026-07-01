import { apiFetch, buildQuery } from "@/lib/api";
import { unwrapList } from "@/lib/format";
import { filterVehicleItems } from "@/lib/vehicle-filters";
import VehicleCatalogClient from "@/components/vehicles/VehicleCatalogClient";

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
    keyword: searchParams.versionKeyword,
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

  return (
    <section className="section catalog-page">
      <div className="shell-container page-title catalog-title">
        <p className="eyebrow">Danh mục</p>
        <h1>Dòng xe Tayota</h1>
        <p>Chọn kiểu dáng, khám phá từng dòng xe và mở phiên bản phù hợp hành trình của bạn.</p>
      </div>
      <div className="shell-container catalog-layout">
        <VehicleCatalogClient error={error} searchParams={params} styles={styles} vehicles={vehicles} />
      </div>
    </section>
  );
}
