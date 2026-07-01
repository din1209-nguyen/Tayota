import { apiFetch, buildQuery } from "@/lib/api";
import { unwrapList } from "@/lib/format";

export function getCarVersions(params = {}) {
  return apiFetch(`/car/catalog/car-versions${buildQuery(params)}`, { cache: "no-store" });
}

export async function getAllCarVersions(params = {}) {
  const firstPage = await getCarVersions({ ...params, page: 0, size: 50 });
  const totalPages = Number(firstPage?.totalPages || 1);
  const remainingPages = totalPages > 1
    ? await Promise.all(
        Array.from({ length: totalPages - 1 }, (_, index) => getCarVersions({ ...params, page: index + 1, size: 50 }))
      )
    : [];
  return [firstPage, ...remainingPages].flatMap((page) => unwrapList(page));
}

export function getCarStylesWithVersions() {
  return apiFetch("/car/catalog/car-styles-with-versions", { cache: "no-store" });
}

export function getCarVersion(id) {
  return apiFetch(`/car/catalog/car-versions/${id}`, { cache: "no-store" });
}

export function getCarVersionSpecification(id) {
  return apiFetch(`/car/catalog/car-versions/${id}/specification`, { cache: "no-store" });
}

export function getDealerships() {
  return apiFetch("/car/dealerships", { cache: "no-store" });
}

export function compareCarVersions(ids) {
  const query = ids.map((id) => `ids=${encodeURIComponent(id)}`).join("&");
  return apiFetch(`/car/catalog/car-versions/compare?${query}`, { cache: "no-store" });
}

export function getAccessories(params = {}) {
  return apiFetch(`/car/accessories${buildQuery(params)}`, { cache: "no-store" });
}

export function getMyVehicles() {
  return apiFetch("/operation/customer-vehicles/my", { cache: "no-store" });
}
