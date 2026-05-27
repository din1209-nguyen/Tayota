import { apiFetch, buildQuery } from "@/lib/api";

export function getAvailableSlots(params) {
  return apiFetch(`/operation/appointments/available-slots${buildQuery(params)}`, {
    cache: "no-store",
  });
}

export function getAvailabilityCalendar(params) {
  return apiFetch(`/operation/appointments/availability-calendar${buildQuery(params)}`, {
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

export function validateServiceVin(vinId) {
  return apiFetch(`/operation/appointments/service/vin-validation${buildQuery({ vinId })}`, {
    cache: "no-store",
  });
}

export function getMyAppointments() {
  return apiFetch("/operation/appointments/my", { cache: "no-store" });
}

export function getMyAppointmentDetail(id) {
  return apiFetch(`/operation/appointments/my/${id}`, { cache: "no-store" });
}

export function getAdvisorAppointments(status = "PENDING") {
  return apiFetch(`/operation/appointments/advisor${buildQuery({ status })}`, {
    cache: "no-store",
  });
}

export function getAdvisorDealership() {
  return apiFetch("/operation/appointments/advisor/dealership", { cache: "no-store" });
}

export function createAdvisorAppointment(payload) {
  return apiFetch("/operation/appointments/advisor", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function getAdvisorTimeSlots() {
  return apiFetch("/operation/appointments/advisor/time-slots", { cache: "no-store" });
}

export function getAdvisorHolidays() {
  return apiFetch("/operation/appointments/advisor/holidays", { cache: "no-store" });
}

export function getAdvisorAppointmentDetail(id) {
  return apiFetch(`/operation/appointments/${id}`, { cache: "no-store" });
}

export function updateAdvisorAppointment(id, payload) {
  return apiFetch(`/operation/appointments/advisor/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function checkInTestDriveAppointment(id) {
  return apiFetch(`/operation/appointments/advisor/test-drive/${id}/check-in`, { method: "PATCH" });
}

export function checkInServiceAppointment(id, payload) {
  return apiFetch(`/operation/appointments/advisor/service/${id}/check-in`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function createAdvisorTimeSlot(payload) {
  return apiFetch("/operation/appointments/advisor/time-slots", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateAdvisorTimeSlot(id, payload) {
  return apiFetch(`/operation/appointments/advisor/time-slots/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function deleteAdvisorTimeSlot(id) {
  return apiFetch(`/operation/appointments/advisor/time-slots/${id}`, { method: "DELETE" });
}

export function createAdvisorHoliday(payload) {
  return apiFetch("/operation/appointments/advisor/holidays", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateAdvisorHoliday(id, payload) {
  return apiFetch(`/operation/appointments/advisor/holidays/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function deleteAdvisorHoliday(id) {
  return apiFetch(`/operation/appointments/advisor/holidays/${id}`, { method: "DELETE" });
}
