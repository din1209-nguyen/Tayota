import { apiFetch, buildQuery } from "@/lib/api";

export function getAvailableSlots(params) {
  return apiFetch(`/operation/appointments/available-slots${buildQuery(params)}`, {
    cache: "no-store",
  });
}

export function createAppointment({ type, authenticated, payload }) {
  const appointmentType = type === "service" ? "service" : "test-drive";
  const suffix = authenticated ? "" : "/guest";

  return apiFetch(`/operation/appointments/${appointmentType}${suffix}`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getMyAppointments() {
  return apiFetch("/operation/appointments/my", { cache: "no-store" });
}

export function getAdvisorAppointments(status = "PENDING") {
  return apiFetch(`/operation/appointments/advisor${buildQuery({ status })}`, {
    cache: "no-store",
  });
}

export function getAdvisorTimeSlots() {
  return apiFetch("/operation/appointments/advisor/time-slots", { cache: "no-store" });
}

export function getAdvisorHolidays() {
  return apiFetch("/operation/appointments/advisor/holidays", { cache: "no-store" });
}
