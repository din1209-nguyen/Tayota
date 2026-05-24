import { apiFetch } from "@/lib/api";

export function getNotifications() {
  return apiFetch("/operation/notifications/my", { cache: "no-store" });
}

export function getUnreadNotificationCount() {
  return apiFetch("/operation/notifications/unread-count", { cache: "no-store" });
}

export function markNotificationAsRead(id) {
  return apiFetch(`/operation/notifications/${id}/read`, { method: "PATCH" });
}

export function markAllNotificationsAsRead() {
  return apiFetch("/operation/notifications/read-all", { method: "PATCH" });
}
