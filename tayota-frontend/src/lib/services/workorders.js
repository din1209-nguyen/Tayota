import { apiFetch, buildQuery } from "@/lib/api";

export function getMyServiceTickets() {
  return apiFetch("/operation/workorders/mechanic/my", { cache: "no-store" });
}

export function getServiceTicketDetail(id) {
  return apiFetch(`/operation/workorders/mechanic/${id}`, { cache: "no-store" });
}

export function receiveServiceTicket(id) {
  return apiFetch(`/operation/workorders/mechanic/${id}/receive`, { method: "PATCH" });
}

export function rejectServiceTicket(id, payload) {
  return apiFetch(`/operation/workorders/mechanic/${id}/reject`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function startServiceTicket(id) {
  return apiFetch(`/operation/workorders/mechanic/${id}/start`, { method: "PATCH" });
}

export function completeServiceTicket(id) {
  return apiFetch(`/operation/workorders/mechanic/${id}/complete`, { method: "PATCH" });
}

export function addServiceItem(id, payload) {
  return apiFetch(`/operation/workorders/mechanic/${id}/items`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateServiceItem(ticketId, itemId, payload) {
  return apiFetch(`/operation/workorders/mechanic/${ticketId}/items/${itemId}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function deleteServiceItem(ticketId, itemId) {
  return apiFetch(`/operation/workorders/mechanic/${ticketId}/items/${itemId}`, {
    method: "DELETE",
  });
}

export function getRecommendedAccessories(id) {
  return apiFetch(`/operation/workorders/mechanic/${id}/recommended-accessories`, { cache: "no-store" });
}

export function getServiceInvoice(id) {
  return apiFetch(`/operation/workorders/${id}/invoice`, { cache: "no-store" });
}

export function getUserServiceTickets() {
  return apiFetch("/operation/workorders/user/my", { cache: "no-store" });
}

export function getUserServiceTicketDetail(id) {
  return apiFetch(`/operation/workorders/user/${id}`, { cache: "no-store" });
}

export function getAdvisorServiceTickets(params = {}) {
  return apiFetch(`/operation/workorders/advisor${buildQuery(params)}`, { cache: "no-store" });
}

export function getAdvisorServiceTicketDetail(id) {
  return apiFetch(`/operation/workorders/advisor/${id}`, { cache: "no-store" });
}

export function createWalkInServiceTicket(payload) {
  return apiFetch("/operation/workorders/advisor/walk-in", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function assignTicketMechanic(id, payload) {
  return apiFetch(`/operation/workorders/advisor/${id}/assign-mechanic`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function cancelAdvisorServiceTicket(id, payload) {
  return apiFetch(`/operation/workorders/advisor/${id}/cancel`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}
