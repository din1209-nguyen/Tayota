import { apiFetch, buildQuery } from "@/lib/api";

export function createAdminUser(payload) {
  return apiFetch("/user/create-account", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getAdminUsers(params = {}) {
  return apiFetch(`/user/admin/users${buildQuery(params)}`, { cache: "no-store" });
}

export function updateAdminUserStatus(userId, payload) {
  const action = payload?.status === "ACTIVE" ? "unban" : "ban";
  return apiFetch(`/user/${action}/${userId}`, { method: "PATCH" });
}

export function resetAdminUserPassword(userId, payload) {
  return apiFetch(`/user/admin/users/${userId}/password`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function getAiDocuments(params = {}) {
  return apiFetch(`/ai/api/v1/documents${buildQuery(params)}`, { cache: "no-store" });
}

export function uploadAiDocument(file) {
  const body = new FormData();
  body.append("file", file);
  return apiFetch("/ai/api/v1/documents", { method: "POST", body });
}

export function getAiDocumentJob(jobId) {
  return apiFetch(`/ai/api/v1/documents/jobs/${jobId}`, { cache: "no-store" });
}

export function deleteAiDocument(documentId) {
  return apiFetch(`/ai/api/v1/documents/${documentId}`, { method: "DELETE" });
}
