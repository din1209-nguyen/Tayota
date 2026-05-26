import { apiFetch, buildQuery } from "@/lib/api";

export function searchAdvisorCustomers(params = {}) {
  return apiFetch(`/user/advisor/customers${buildQuery(params)}`, { cache: "no-store" });
}

export function assignCustomerVehicle(payload) {
  return apiFetch("/operation/customer-vehicles/advisor/assign", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getCustomerVehicles(userId) {
  return apiFetch(`/operation/customer-vehicles/advisor/customer/${userId}`, { cache: "no-store" });
}

export function removeCustomerVehicle(vinId) {
  return apiFetch(`/operation/customer-vehicles/advisor/${encodeURIComponent(vinId)}/inactive`, {
    method: "PATCH",
  });
}

export function getActiveAdvisorMechanics() {
  return apiFetch("/operation/mechanics/advisor/active", { cache: "no-store" });
}
