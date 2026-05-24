import { apiFetch, buildQuery } from "@/lib/api";

export function getCarVersions(params = {}) {
  return apiFetch(`/car/catalog/car-versions${buildQuery(params)}`, { cache: "no-store" });
}

export function getCarVersion(id) {
  return apiFetch(`/car/catalog/car-versions/${id}`, { cache: "no-store" });
}

export function getCarVersionSpecification(id) {
  return apiFetch(`/car/catalog/car-versions/${id}/specification`, { cache: "no-store" });
}

export function compareCarVersions(ids) {
  const query = ids.map((id) => `ids=${encodeURIComponent(id)}`).join("&");
  return apiFetch(`/car/catalog/car-versions/compare?${query}`, { cache: "no-store" });
}

export function getAccessories(params = {}) {
  return apiFetch(`/car/accessories${buildQuery(params)}`, { cache: "no-store" });
}
