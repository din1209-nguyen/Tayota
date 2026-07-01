import { apiFetch } from "@/lib/api";

export function getUserProfile(userId) {
  return apiFetch(`/user/profile/${userId}`, { cache: "no-store" });
}

export function updateUserProfile(payload) {
  return apiFetch("/user/profile", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}
