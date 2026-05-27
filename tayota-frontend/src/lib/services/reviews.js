import { apiFetch, buildQuery } from "@/lib/api";

export function getReviewByToken(token) {
  return apiFetch(`/operation/reviews/token/${encodeURIComponent(token)}`, { cache: "no-store" });
}

export function submitReviewByToken(token, payload) {
  return apiFetch(`/operation/reviews/token/${encodeURIComponent(token)}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function getMyReviews() {
  return apiFetch("/operation/reviews/my", { cache: "no-store" });
}

export function getMyMechanicReviews() {
  return apiFetch("/operation/reviews/mechanic/my", { cache: "no-store" });
}

export function getMyMechanicReviewSummary() {
  return apiFetch("/operation/reviews/mechanic/my/summary", { cache: "no-store" });
}

export function getAdvisorReviewSummary(params = {}) {
  return apiFetch(`/operation/reviews/advisor/summary${buildQuery(params)}`, { cache: "no-store" });
}
