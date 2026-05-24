"use client";

import { useEffect, useMemo, useState } from "react";
import { sendAiChatMessage } from "@/lib/services/chat";

function makeSessionId() {
  return `tayota-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export default function AiChatWidget() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([
    { role: "assistant", text: "Tôi có thể tư vấn dòng xe, lịch lái thử và dịch vụ TAYOTA." },
  ]);
  const sessionId = useMemo(() => {
    if (typeof window === "undefined") return "";
    const existing = localStorage.getItem("tayota_ai_session_id");
    if (existing) return existing;
    const next = makeSessionId();
    localStorage.setItem("tayota_ai_session_id", next);
    return next;
  }, []);

  useEffect(() => {
    if (open) document.getElementById("ai-chat-input")?.focus();
  }, [open]);

  async function sendMessage(event) {
    event.preventDefault();
    const text = input.trim();
    if (!text || loading) return;
    setInput("");
    setMessages((items) => [...items, { role: "user", text }]);
    setLoading(true);
    try {
      const result = await sendAiChatMessage({ message: text, sessionId });
      setMessages((items) => [
        ...items,
        {
          role: "assistant",
          text: result?.answer || result?.message || "Đã nhận phản hồi từ AI.",
          sources: result?.sources || [],
        },
      ]);
    } catch (error) {
      setMessages((items) => [
        ...items,
        { role: "assistant", text: error.message || "AI service đang tạm thời gián đoạn." },
      ]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="chat-widget" id="ai-chat">
      {open ? (
        <section className="chat-panel" aria-label="AI tư vấn TAYOTA">
          <div className="chat-head">
            <strong>AI tư vấn</strong>
            <button type="button" onClick={() => setOpen(false)} aria-label="Đóng chat">
              x
            </button>
          </div>
          <div className="chat-messages">
            {messages.map((message, index) => (
              <div className={`chat-bubble ${message.role}`} key={`${message.role}-${index}`}>
                <p>{message.text}</p>
                {message.sources?.length ? <small>Nguồn: {message.sources.length}</small> : null}
              </div>
            ))}
            {loading ? <div className="chat-bubble assistant">Đang trả lời...</div> : null}
          </div>
          <form className="chat-form" onSubmit={sendMessage}>
            <input
              id="ai-chat-input"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Hỏi về dòng xe..."
              aria-label="Tin nhắn"
            />
            <button type="submit" disabled={loading}>
              Gửi
            </button>
          </form>
        </section>
      ) : null}
      <button className="chat-launch" type="button" onClick={() => setOpen(true)}>
        AI tư vấn
      </button>
    </div>
  );
}
