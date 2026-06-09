"use client";

import { Client } from "@stomp/stompjs";
import { useEffect, useRef, useState } from "react";
import {
  getChatWebSocketUrl,
  getCurrentChatMessages,
  getCurrentChatSession,
  getAssistantChatMessages,
  sendAssistantChatMessage,
  sendCustomerChatMessage,
} from "@/lib/services/chat";
import { statusLabel } from "@/lib/format";
import { getCurrentUser, onSessionChange } from "@/lib/session";

const CUSTOMER_LIVE_CHAT_ROLES = new Set(["USER", "CUSTOMER"]);

function appendUnique(messages, nextMessage) {
  if (!nextMessage) return messages;
  if (nextMessage.id && messages.some((message) => message.id === nextMessage.id)) return messages;
  return [...messages, nextMessage];
}

function senderLabel(type) {
  if (type === "ASSISTANT" || type === "STAFF") return "Tayota";
  if (type === "SYSTEM") return "Hệ thống";
  return "Khách hàng";
}

function messageTone(senderType, isAssistant) {
  if (senderType === "SYSTEM") return "system";
  const sentByStaff = senderType === "ASSISTANT" || senderType === "STAFF";
  const sentByCustomer = senderType === "CUSTOMER";
  const isOwn = isAssistant ? sentByStaff : sentByCustomer;
  return isOwn ? "own" : "other";
}

function canUseCustomerLiveChat(user) {
  return !user || CUSTOMER_LIVE_CHAT_ROLES.has(user.role);
}

function statusClass(status) {
  return `status-${String(status || "idle").toLowerCase()}`;
}

export default function LiveChatPanel({
  mode = "customer",
  sessionId: providedSessionId = "",
  variant = "dashboard",
  readOnly = false,
  readOnlyMessage = "Phiên này đang được xử lý bởi nhân viên khác. Bạn chỉ có thể xem lịch sử.",
  showHeader = true,
  onStatusChange,
  onSessionUpdate,
  onRestrictedAction,
}) {
  const isAssistant = mode === "assistant";
  const [currentUser, setCurrentUser] = useState(null);
  const [sessionId, setSessionId] = useState(providedSessionId);
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [status, setStatus] = useState("idle");
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const clientRef = useRef(null);
  const liveChatAllowed = isAssistant || canUseCustomerLiveChat(currentUser);

  useEffect(() => {
    onStatusChange?.(status);
  }, [onStatusChange, status]);

  useEffect(() => {
    setCurrentUser(getCurrentUser());
    return onSessionChange(() => setCurrentUser(getCurrentUser()));
  }, []);

  useEffect(() => {
    let cancelled = false;
    let client;

    async function connect() {
      if (!liveChatAllowed) {
        setMessages([]);
        setSessionId("");
        setStatus("idle");
        setError("");
        return;
      }

      setError("");
      setStatus("connecting");
      setMessages([]);

      try {
        const session = isAssistant ? { id: providedSessionId } : await getCurrentChatSession();
        const resolvedSessionId = session?.id;
        if (!resolvedSessionId) {
          setStatus("idle");
          return;
        }

        if (cancelled) return;
        setSessionId(resolvedSessionId);

        const history = isAssistant ? await getAssistantChatMessages(resolvedSessionId) : await getCurrentChatMessages();
        if (!cancelled) setMessages(Array.isArray(history) ? history : []);

        client = new Client({
          brokerURL: getChatWebSocketUrl(),
          reconnectDelay: 5000,
          onConnect: () => {
            setStatus("connected");
            client.subscribe(`/topic/chat.sessions.${resolvedSessionId}`, (frame) => {
              const payload = JSON.parse(frame.body);
              if (payload?.status && !payload?.content) {
                onSessionUpdate?.(payload);
                return;
              }
              if (!payload?.content) return;
              setMessages((current) => appendUnique(current, payload));
            });
          },
          onStompError: (frame) => {
            setStatus("error");
            setError(frame.headers?.message || "Không thể kết nối live chat.");
          },
          onWebSocketClose: () => {
            if (!cancelled) setStatus("disconnected");
          },
        });

        client.activate();
        clientRef.current = client;
      } catch (caughtError) {
        if (!cancelled) {
          setStatus("error");
          setError(caughtError.message || "Không thể mở live chat.");
        }
      }
    }

    connect();

    return () => {
      cancelled = true;
      clientRef.current = null;
      if (client) client.deactivate();
    };
  }, [isAssistant, liveChatAllowed, onSessionUpdate, providedSessionId]);

  async function send(event) {
    event.preventDefault();
    const text = content.trim();
    if (readOnly || !liveChatAllowed || !text || !sessionId || sending) return;

    setSending(true);
    setError("");

    try {
      const savedMessage = isAssistant
        ? await sendAssistantChatMessage(sessionId, text)
        : await sendCustomerChatMessage(text);
      setMessages((current) => appendUnique(current, savedMessage));
      setContent("");
    } catch (caughtError) {
      if ([403, 409].includes(caughtError.status)) onRestrictedAction?.();
      setError(caughtError.message || "Không gửi được tin nhắn. Vui lòng thử lại.");
    } finally {
      setSending(false);
    }
  }

  return (
    <section className={`ops-panel live-chat-panel ${variant === "widget" ? "live-chat-widget-panel" : ""} ${!showHeader ? "live-chat-panel-no-head" : ""}`}>
      {showHeader ? (
        <div className="ops-panel-head">
          <div>
            <h2>{isAssistant ? "Hỗ trợ khách hàng" : "Tư vấn trực tiếp"}</h2>
            {sessionId && variant !== "widget" ? <p className="muted-text">Phiên {sessionId}</p> : null}
          </div>
          <span className={`status-pill ${statusClass(status)}`}>{statusLabel(status.toUpperCase())}</span>
        </div>
      ) : null}

      <div className="live-chat-feedback" aria-live="polite">
        {readOnly ? <div className="status-box">{readOnlyMessage}</div> : null}
        {!liveChatAllowed ? <div className="status-box">Tư vấn trực tiếp chỉ dành cho khách hàng và khách vãng lai.</div> : null}
        {error ? <div className="status-box">{error}</div> : null}
      </div>

      {liveChatAllowed ? <div className="live-chat-messages">
        {messages.length ? (
          messages.map((message) => (
            <div
              className={`live-chat-message ${messageTone(message.senderType, isAssistant)}`}
              key={message.id || `${message.senderType}-${message.createdAt}-${message.content}`}
            >
              <strong>{senderLabel(message.senderType)}</strong>
              <p>{message.content}</p>
            </div>
          ))
        ) : (
          <div className="status-box">Chưa có tin nhắn.</div>
        )}
      </div> : null}

      {liveChatAllowed && !readOnly ? <form className="live-chat-form" onSubmit={send}>
        <input
          className="field"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder="Nhập tin nhắn"
          disabled={sending}
        />
        <button className="btn btn-primary" type="submit" disabled={!sessionId || !content.trim() || sending}>
          {sending ? "Đang gửi..." : "Gửi"}
        </button>
      </form> : null}
    </section>
  );
}
