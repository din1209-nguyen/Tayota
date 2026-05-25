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

export function logout() {
  return apiFetch("/user/logout", {
    method: "POST",
    skipAuthRefresh: true,
  });
}

export function getMe() {
  return apiFetch("/user/me", { cache: "no-store" });
}
