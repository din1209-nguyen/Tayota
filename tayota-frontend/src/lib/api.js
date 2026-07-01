import axios from "axios";
import { clearSession, getAccessToken, setAccessToken } from "@/lib/session";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:9090";
let activeRefreshPromise = null;

const NO_REFRESH_PATHS = [
  "/user/login",
  "/user/register",
  "/user/verify-account",
  "/user/refresh-token",
  "/user/logout",
];

export class ApiError extends Error {
  constructor(message, { status, data } = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

function canUseBrowserStorage() {
  return typeof window !== "undefined";
}

function leaveProtectedWorkspace() {
  if (canUseBrowserStorage() && window.location.pathname.startsWith("/dashboard")) {
    window.location.assign("/auth/login");
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

function toApiError(error) {
  if (error instanceof ApiError) return error;
  const status = error.response?.status;
  const data = error.response?.data;
  return new ApiError(getErrorMessage(data, status || 500), { status, data });
}

function shouldSkipRefresh(config = {}) {
  const requestPath = config.url || "";
  return config.skipAuthRefresh || NO_REFRESH_PATHS.some((path) => requestPath === path);
}

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
  const token = config.explicitToken ?? getAccessToken();
  if (token && !config.skipAuthToken) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

async function requestRefreshedAccessToken() {
  try {
    const response = await apiClient.post("/user/refresh-token", undefined, {
      skipAuthRefresh: true,
      skipAuthToken: true,
    });
    const data = response.data;
    const okByBody = data?.success ?? data?.isSuccess;
    if (okByBody === false) {
      throw new ApiError(getErrorMessage(data, response.status), { status: response.status, data });
    }
    const accessToken = data?.result?.accessToken || data?.accessToken || "";
    if (accessToken) setAccessToken(accessToken);
    return accessToken;
  } catch (error) {
    clearSession();
    leaveProtectedWorkspace();
    throw toApiError(error);
  }
}

function refreshAccessToken() {
  if (!activeRefreshPromise) {
    activeRefreshPromise = requestRefreshedAccessToken().finally(() => {
      activeRefreshPromise = null;
    });
  }
  return activeRefreshPromise;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config || {};
    if (
      error.response?.status !== 401 ||
      !canUseBrowserStorage() ||
      config._retriedAfterRefresh ||
      shouldSkipRefresh(config)
    ) {
      return Promise.reject(error);
    }

    config._retriedAfterRefresh = true;
    const accessToken = await refreshAccessToken();
    if (!accessToken) return Promise.reject(error);
    config.explicitToken = accessToken;
    return apiClient.request(config);
  },
);

export async function apiFetch(path, options = {}) {
  const {
    body,
    cache: _cache,
    token: explicitToken,
    skipAuthRefresh = false,
    headers,
    ...requestOptions
  } = options;
  const isFormDataBody = typeof FormData !== "undefined" && body instanceof FormData;
  void _cache;
  const requestHeaders = { ...(headers || {}) };
  if (body !== undefined && !isFormDataBody && !requestHeaders["Content-Type"]) {
    requestHeaders["Content-Type"] = "application/json";
  }

  try {
    const response = await apiClient.request({
      url: path,
      data: body,
      headers: requestHeaders,
      explicitToken,
      skipAuthRefresh,
      ...requestOptions,
    });
    const okByBody = response.data?.success ?? response.data?.isSuccess;
    if (okByBody === false) {
      throw new ApiError(getErrorMessage(response.data, response.status), {
        status: response.status,
        data: response.data,
      });
    }
    return unwrapResponse(response.data);
  } catch (error) {
    throw toApiError(error);
  }
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
