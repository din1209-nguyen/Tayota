import { apiFetch, buildQuery } from "@/lib/api";

export function getAdvisorReport(params = {}) {
  return apiFetch(`/operation/reports/advisor${buildQuery(params)}`, { cache: "no-store" });
}
