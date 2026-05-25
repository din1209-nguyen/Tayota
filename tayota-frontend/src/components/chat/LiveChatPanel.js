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

export default function LiveChatPanel({ mode = "customer", sessionId: providedSessionId = "", variant = "dashboard" }) {
  const isAssistant = mode === "assistant";
  const [sessionId, setSessionId] = useState(providedSessionId);
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState("");
  const [status, setStatus] = useState("idle");
  const [error, setError] = useState("");
  const clientRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    let client;

    async function connect() {
      setError("");
      setStatus("connecting");

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
  }, [isAssistant, providedSessionId]);

  async function send(event) {
    event.preventDefault();
    const text = content.trim();
    if (!text || !sessionId) return;

    setContent("");
    setError("");

    try {
      const savedMessage = isAssistant
        ? await sendAssistantChatMessage(sessionId, text)
        : await sendCustomerChatMessage(text);
      setMessages((current) => appendUnique(current, savedMessage));
    } catch (caughtError) {
      setError(caughtError.message || "Không gửi được tin nhắn. Vui lòng thử lại.");
    }
  }

  return (
    <section className={`ops-panel live-chat-panel ${variant === "widget" ? "live-chat-widget-panel" : ""}`}>
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">Live chat</p>
          <h2>{isAssistant ? "Hỗ trợ khách hàng" : "Tư vấn trực tiếp"}</h2>
          {sessionId && variant !== "widget" ? <p className="muted-text">Phiên {sessionId}</p> : null}
        </div>
        <span className={`status-pill ${status}`}>{statusLabel(status.toUpperCase())}</span>
      </div>

      <div className="live-chat-feedback" aria-live="polite">
        {error ? <div className="status-box">{error}</div> : null}
      </div>

      <div className="live-chat-messages">
        {messages.length ? (
          messages.map((message) => (
            <div
              className={`live-chat-message ${message.senderType === "CUSTOMER" ? "customer" : "assistant"}`}
              key={message.id || `${message.senderType}-${message.createdAt}-${message.content}`}
            >
              <strong>{senderLabel(message.senderType)}</strong>
              <p>{message.content}</p>
            </div>
          ))
        ) : (
          <div className="status-box">Chưa có tin nhắn.</div>
        )}
      </div>

      <form className="live-chat-form" onSubmit={send}>
        <input
          className="field"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          placeholder="Nhập tin nhắn"
        />
        <button className="btn btn-primary" type="submit" disabled={!sessionId}>
          Gửi
        </button>
      </form>
    </section>
  );
}
