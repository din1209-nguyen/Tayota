import { apiFetch, buildQuery } from "@/lib/api";

export function getManagerVehicles() {
  return apiFetch("/car/manager/car-versions", { cache: "no-store" });
}

export function getManagerVehicleDetail(id) {
  return apiFetch(`/car/manager/car-versions/${id}`, { cache: "no-store" });
}

export function createVehicle(payload) {
  return apiFetch("/car/car-versions", { method: "POST", body: JSON.stringify(payload) });
}

export function createVehicleStyle(payload) {
  return apiFetch("/car/car-styles", { method: "POST", body: JSON.stringify(payload) });
}

export function createVehicleSeries(payload) {
  return apiFetch("/car/car-series", { method: "POST", body: JSON.stringify(payload) });
}

export function updateVehicle(id, payload) {
  return apiFetch(`/car/car-versions/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function hideVehicle(id) {
  return apiFetch(`/car/car-versions/${id}`, { method: "DELETE" });
}

export function getVehicleContent(id) {
  return apiFetch(`/car/catalog/car-versions/${id}`, { cache: "no-store" });
}

export function saveVehicleSpecification(id, payload) {
  return apiFetch(`/car/car-versions/${id}/specification`, { method: "PUT", body: JSON.stringify(payload) });
}

export function saveVehiclePrice(id, payload) {
  return apiFetch(`/car/car-versions/${id}/prices`, { method: "PUT", body: JSON.stringify(payload) });
}

export function deleteVehiclePrice(id, exteriorColorId, interiorColorId) {
  return apiFetch(`/car/car-versions/${id}/prices${buildQuery({ exteriorColorId, interiorColorId })}`, { method: "DELETE" });
}

export function addVehicleGallery(id, payload) {
  return apiFetch(`/car/car-versions/${id}/galleries`, { method: "POST", body: JSON.stringify(payload) });
}

export function updateVehicleGallery(id, galleryId, payload) {
  return apiFetch(`/car/car-versions/${id}/galleries/${galleryId}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function deleteVehicleGallery(id, galleryId) {
  return apiFetch(`/car/car-versions/${id}/galleries/${galleryId}`, { method: "DELETE" });
}

export function getManagerArticles() {
  return apiFetch("/car/manager/articles", { cache: "no-store" });
}

export function createArticle(payload) {
  return apiFetch("/car/manager/articles", { method: "POST", body: JSON.stringify(payload) });
}

export function updateArticle(id, payload) {
  return apiFetch(`/car/manager/articles/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function hideArticle(id) {
  return apiFetch(`/car/manager/articles/${id}`, { method: "DELETE" });
}

export function getManagerDealerships() {
  return apiFetch("/car/manager/dealerships", { cache: "no-store" });
}

export function createDealership(payload) {
  return apiFetch("/car/manager/dealerships", { method: "POST", body: JSON.stringify(payload) });
}

export function updateDealership(id, payload) {
  return apiFetch(`/car/manager/dealerships/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function deactivateDealership(id) {
  return apiFetch(`/car/manager/dealerships/${id}`, { method: "DELETE" });
}

export function getManagerAccessories(params = {}) {
  return apiFetch(`/car/manager/accessories${buildQuery(params)}`, { cache: "no-store" });
}

export function createAccessory(payload) {
  return apiFetch("/car/accessories", { method: "POST", body: JSON.stringify(payload) });
}

export function updateAccessory(id, payload) {
  return apiFetch(`/car/accessories/${id}`, { method: "PUT", body: JSON.stringify(payload) });
}

export function hideAccessory(id) {
  return apiFetch(`/car/accessories/${id}`, { method: "DELETE" });
}

export function attachAccessoryToVehicle(accessoryId, carVersionId) {
  return apiFetch("/car/accessories/car-versions", {
    method: "POST",
    body: JSON.stringify({ accessoryId, carVersionId }),
  });
}

export function detachAccessoryFromVehicle(accessoryId, carVersionId) {
  return apiFetch("/car/accessories/car-versions", {
    method: "DELETE",
    body: JSON.stringify({ accessoryId, carVersionId }),
  });
}

export function getManagerUsers(params = {}) {
  return apiFetch(`/user/manager/users${buildQuery(params)}`, { cache: "no-store" });
}

export function getManagerUserStats() {
  return apiFetch("/user/manager/users/stats", { cache: "no-store" });
}

export function getManagerUserProfile(id) {
  return apiFetch(`/user/profile/${id}`, { cache: "no-store" });
}

export function updateManagerUserProfile(payload) {
  return apiFetch("/user/profile", { method: "PUT", body: JSON.stringify(payload) });
}

export function getPublishedNews() {
  return apiFetch("/car/news", { cache: "no-store" });
}

export function getPublishedArticle(id) {
  return apiFetch(`/car/news/${id}`, { cache: "no-store" });
}
