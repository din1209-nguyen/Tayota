import { apiFetch } from "@/lib/api";

export function login(payload) {
  return apiFetch("/user/login", {
    method: "POST",
    body: JSON.stringify(payload),
    skipAuthRefresh: true,
  });
}

export function register(payload) {
  return apiFetch("/user/register", {
    method: "POST",
    body: JSON.stringify(payload),
    skipAuthRefresh: true,
  });
}

export function verifyAccount(payload) {
  return apiFetch("/user/verify-account", {
    method: "POST",
    body: JSON.stringify(payload),
    skipAuthRefresh: true,
  });
}

export function sendForgotPasswordOtp(email) {
  const query = new URLSearchParams({ email }).toString();
  return apiFetch(`/user/forgot-password/send-otp?${query}`, {
    method: "POST",
    skipAuthRefresh: true,
    skipAuthToken: true,
  });
}

export function verifyForgotPasswordOtp(payload) {
  return apiFetch("/user/forgot-password/verify-otp", {
    method: "POST",
    body: JSON.stringify(payload),
    skipAuthRefresh: true,
    skipAuthToken: true,
  });
}

export function resetForgotPassword(payload) {
  return apiFetch("/user/forgot-password/reset-password", {
    method: "PATCH",
    body: JSON.stringify(payload),
    skipAuthRefresh: true,
    skipAuthToken: true,
  });
}

export function logout() {
  return apiFetch("/user/logout", {
    method: "POST",
    skipAuthRefresh: true,
  });
}

export function getMe() {
  return apiFetch("/user/me", { cache: "no-store" });
}
