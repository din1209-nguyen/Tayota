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

function appendUnique(messages, nextMessage) {
  if (!nextMessage) return messages;
  if (nextMessage.id && messages.some((message) => message.id === nextMessage.id)) return messages;
  return [...messages, nextMessage];
}

export default function LiveChatPanel({ mode = "customer", sessionId: providedSessionId = "" }) {
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
        const session = mode === "customer" ? await getCurrentChatSession() : { id: providedSessionId };
        const resolvedSessionId = session?.id;
        if (!resolvedSessionId) {
          setStatus("idle");
          return;
        }

        if (cancelled) return;
        setSessionId(resolvedSessionId);

        const history = mode === "customer"
          ? await getCurrentChatMessages()
          : await getAssistantChatMessages(resolvedSessionId);
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
          setError(caughtError.message);
        }
      }
    }

    connect();

    return () => {
      cancelled = true;
      clientRef.current = null;
      if (client) client.deactivate();
    };
  }, [mode, providedSessionId]);

  async function send(event) {
    event.preventDefault();
    const text = content.trim();
    if (!text || !sessionId) return;
    setContent("");

    const client = clientRef.current;
    if (client?.connected) {
      client.publish({
        destination: mode === "assistant" ? "/app/assistant.chat.send" : "/app/chat.send",
        headers: { chat_session: sessionId },
        body: JSON.stringify({ content: text }),
      });
      return;
    }

    const savedMessage = mode === "assistant"
      ? await sendAssistantChatMessage(sessionId, text)
      : await sendCustomerChatMessage(text);
    setMessages((current) => appendUnique(current, savedMessage));
  }

  return (
    <section className="ops-panel live-chat-panel">
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">Live chat</p>
          <h2>{sessionId ? `Session ${sessionId}` : "Chưa có phiên chat"}</h2>
        </div>
        <span className={`status-pill ${status}`}>{status}</span>
      </div>
      {error ? <div className="status-box">{error}</div> : null}
      <div className="live-chat-messages">
        {messages.length ? messages.map((message) => (
          <div className="live-chat-message" key={message.id || `${message.senderType}-${message.createdAt}`}>
            <strong>{message.senderType || "USER"}</strong>
            <p>{message.content}</p>
          </div>
        )) : <div className="status-box">Chưa có tin nhắn.</div>}
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
