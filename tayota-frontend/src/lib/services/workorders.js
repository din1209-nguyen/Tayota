import { apiFetch } from "@/lib/api";

export function getMyServiceTickets() {
  return apiFetch("/operation/workorders/mechanic/my", { cache: "no-store" });
}

export function getServiceTicketDetail(id) {
  return apiFetch(`/operation/workorders/mechanic/${id}`, { cache: "no-store" });
}

export function receiveServiceTicket(id) {
  return apiFetch(`/operation/workorders/mechanic/${id}/receive`, { method: "PATCH" });
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
