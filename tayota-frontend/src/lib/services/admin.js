import { apiFetch } from "@/lib/api";

export function createAdminUser(payload) {
  return apiFetch("/user/create-account", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getAiDocuments() {
  return apiFetch("/ai/api/v1/documents", { cache: "no-store" });
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
