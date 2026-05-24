import { API_BASE_URL, apiFetch } from "@/lib/api";

export function getCurrentChatSession() {
  return apiFetch("/user/chat/sessions/current", {
    method: "POST",
    cache: "no-store",
  });
}

export function getCurrentChatMessages() {
  return apiFetch("/user/chat/sessions/current/messages", { cache: "no-store" });
}

export function sendCustomerChatMessage(content) {
  return apiFetch("/user/chat/messages", {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

export function getAssistantChatSessions(status = "WAITING") {
  return apiFetch(`/user/assistant/chat/sessions?status=${encodeURIComponent(status)}`, {
    cache: "no-store",
  });
}

export function getAssistantChatMessages(sessionId) {
  return apiFetch(`/user/assistant/chat/sessions/${sessionId}/messages`, { cache: "no-store" });
}

export function assignAssistantChatSession(sessionId) {
  return apiFetch(`/user/assistant/chat/sessions/${sessionId}/assign`, { method: "PATCH" });
}

export function resolveAssistantChatSession(sessionId) {
  return apiFetch(`/user/assistant/chat/sessions/${sessionId}/resolve`, { method: "PATCH" });
}

export function closeAssistantChatSession(sessionId) {
  return apiFetch(`/user/assistant/chat/sessions/${sessionId}/close`, { method: "PATCH" });
}

export function sendAssistantChatMessage(sessionId, content) {
  return apiFetch(`/user/assistant/chat/sessions/${sessionId}/messages`, {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

export function sendAiChatMessage({ message, sessionId }) {
  return apiFetch("/ai/api/v1/chat", {
    method: "POST",
    headers: sessionId ? { "X-AI-Session-Id": sessionId } : undefined,
    body: JSON.stringify({ message }),
  });
}

export function getChatWebSocketUrl() {
  return `${API_BASE_URL.replace(/^http/, "ws")}/user/chat/ws`;
}
