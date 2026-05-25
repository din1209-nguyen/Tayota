"use client";

const TOKEN_KEY = "tayota_access_token";
const USER_KEY = "tayota_current_user";
const SESSION_EVENT = "tayota:session-changed";

function canUseStorage() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

export function getAccessToken() {
  if (!canUseStorage()) return "";
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setAccessToken(token) {
  if (!canUseStorage()) return;
  if (token) localStorage.setItem(TOKEN_KEY, token);
  window.dispatchEvent(new Event(SESSION_EVENT));
}

export function clearAccessToken() {
  if (!canUseStorage()) return;
  localStorage.removeItem(TOKEN_KEY);
  window.dispatchEvent(new Event(SESSION_EVENT));
}

export function getCurrentUser() {
  if (!canUseStorage()) return null;
  const value = localStorage.getItem(USER_KEY);
  if (!value) return null;
  try {
    return JSON.parse(value);
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function setCurrentUser(user) {
  if (!canUseStorage()) return;
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  window.dispatchEvent(new Event(SESSION_EVENT));
}

export function clearCurrentUser() {
  if (!canUseStorage()) return;
  localStorage.removeItem(USER_KEY);
  window.dispatchEvent(new Event(SESSION_EVENT));
}

export function clearSession() {
  if (!canUseStorage()) return;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.dispatchEvent(new Event(SESSION_EVENT));
}

export function getDashboardPath(role) {
  if (role === "ADMIN" || role === "MANAGER") return "/dashboard/admin";
  if (role === "SERVICE_ADVISOR") return "/dashboard/advisor";
  if (role === "ASSISTANT") return "/dashboard/assistant";
  if (role === "MECHANIC") return "/dashboard/mechanic";
  return "/dashboard/user";
}

export function onSessionChange(callback) {
  if (typeof window === "undefined") return () => {};
  window.addEventListener(SESSION_EVENT, callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener(SESSION_EVENT, callback);
    window.removeEventListener("storage", callback);
  };
}
