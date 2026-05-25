export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:9090";
const TOKEN_KEY = "tayota_access_token";

export class ApiError extends Error {
  constructor(message, { status, data } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

function canUseBrowserStorage() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function getStoredAccessToken() {
  if (!canUseBrowserStorage()) return "";
  return window.localStorage.getItem(TOKEN_KEY) || "";
}

function setStoredAccessToken(token) {
  if (!canUseBrowserStorage()) return;
  if (token) window.localStorage.setItem(TOKEN_KEY, token);
}

function clearStoredAccessToken() {
  if (!canUseBrowserStorage()) return;
  window.localStorage.removeItem(TOKEN_KEY);
}

function isFormDataBody(body) {
  return typeof FormData !== "undefined" && body instanceof FormData;
}

function buildHeaders({ body, headers, token }) {
  const requestHeaders = new Headers(headers || {});

  if (body !== undefined && !isFormDataBody(body) && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  return requestHeaders;
}

async function parseResponse(response) {
  const text = await response.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function unwrapResponse(data) {
  if (data && typeof data === "object" && ("result" in data || "isSuccess" in data || "success" in data)) {
    return data.result ?? null;
  }

  return data;
}

function getErrorMessage(data, status) {
  if (data && typeof data === "object") {
    return data.message || data.error || `Yêu cầu thất bại: ${status}`;
  }

  return data || `Yêu cầu thất bại: ${status}`;
}

async function refreshAccessToken() {
  if (!canUseBrowserStorage()) return "";

  const response = await fetch(`${API_BASE_URL}/user/refresh-token`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
  });
  const data = await parseResponse(response);
  const okByBody = data?.success ?? data?.isSuccess;

  if (!response.ok || okByBody === false) {
    clearStoredAccessToken();
    throw new ApiError(getErrorMessage(data, response.status), { status: response.status, data });
  }

  const accessToken = data?.result?.accessToken || data?.accessToken || "";
  if (accessToken) setStoredAccessToken(accessToken);
  return accessToken;
}

async function send(path, options = {}, token) {
  const { headers, ...fetchOptions } = options;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...fetchOptions,
    credentials: "include",
    headers: buildHeaders({
      body: fetchOptions.body,
      headers,
      token,
    }),
  });
  const data = await parseResponse(response);
  const okByBody = data?.success ?? data?.isSuccess;

  return { response, data, okByBody };
}

export async function apiFetch(path, options = {}) {
  const { token: explicitToken, skipAuthRefresh = false, ...fetchOptions } = options;
  const initialToken = explicitToken ?? getStoredAccessToken();
  let result = await send(path, fetchOptions, initialToken);

  if (
    result.response.status === 401 &&
    !skipAuthRefresh &&
    path !== "/user/refresh-token" &&
    canUseBrowserStorage()
  ) {
    const refreshedToken = await refreshAccessToken();
    if (refreshedToken) {
      result = await send(path, fetchOptions, refreshedToken);
    }
  }

  if (!result.response.ok || result.okByBody === false) {
    if (result.response.status === 401) clearStoredAccessToken();
    throw new ApiError(getErrorMessage(result.data, result.response.status), {
      status: result.response.status,
      data: result.data,
    });
  }

  return unwrapResponse(result.data);
}

export function buildQuery(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, value);
    }
  });
  const text = query.toString();
  return text ? `?${text}` : "";
}
