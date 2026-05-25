"use client";

import { useEffect, useState } from "react";
import { sendAiChatMessage } from "@/lib/services/chat";
import LiveChatPanel from "@/components/chat/LiveChatPanel";

const UNAVAILABLE_TEXT =
  "AI tạm gián đoạn. Vui lòng thử lại sau hoặc chuyển sang live chat để nhân viên Tayota hỗ trợ trực tiếp.";

export default function ChatLauncher() {
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState("");
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [connectionState, setConnectionState] = useState("ready");
  const [messages, setMessages] = useState([
    { role: "assistant", text: "Xin chào, tôi có thể tư vấn dòng xe, lịch lái thử và dịch vụ Tayota." },
  ]);

  useEffect(() => {
    function handleOpen() {
      setOpen(true);
      setMode("ai");
    }

    window.addEventListener("tayota:open-ai-chat", handleOpen);
    if (window.location.hash === "#ai-chat") handleOpen();
    return () => window.removeEventListener("tayota:open-ai-chat", handleOpen);
  }, []);

  useEffect(() => {
    if (open && mode === "ai") document.getElementById("ai-chat-input")?.focus();
  }, [open, mode]);

  async function sendMessage(event) {
    event.preventDefault();
    const text = input.trim();
    if (!text || loading) return;

    setInput("");
    setLoading(true);
    setConnectionState("connecting");
    setMessages((items) => [...items, { role: "user", text }]);

    try {
      const result = await sendAiChatMessage({ message: text });
      setConnectionState("ready");
      setMessages((items) => [
        ...items,
        {
          role: "assistant",
          text: result?.answer || result?.message || "Tôi đã nhận được yêu cầu của bạn.",
          sources: result?.sources || [],
        },
      ]);
    } catch (error) {
      setConnectionState("unavailable");
      setMessages((items) => [
        ...items,
        { role: "assistant", text: error.status === 503 ? UNAVAILABLE_TEXT : error.message || UNAVAILABLE_TEXT },
      ]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="chat-widget" id="ai-chat">
      {open ? (
        <section className="chat-panel" aria-label="Tư vấn Tayota">
          <div className="chat-head">
            <div>
              <span className="eyebrow">Tayota concierge</span>
              <strong>{mode === "live" ? "Live chat" : mode === "ai" ? "Tư vấn AI" : "Chọn kênh hỗ trợ"}</strong>
            </div>
            <button className="icon-button" type="button" onClick={() => setOpen(false)} aria-label="Đóng chat">
              ×
            </button>
          </div>

          {!mode ? (
            <div className="chat-choice-panel">
              <button className="choice-card" type="button" onClick={() => setMode("ai")}>
                <strong>Tư vấn AI</strong>
                <span>Hỏi nhanh về xe, giá, lịch lái thử và dịch vụ.</span>
              </button>
              <button className="choice-card" type="button" onClick={() => setMode("live")}>
                <strong>Live chat</strong>
                <span>Trao đổi realtime với tư vấn viên Tayota.</span>
              </button>
            </div>
          ) : null}

          {mode === "ai" ? (
            <>
              <div className={`chat-state ${connectionState}`}>
                {connectionState === "connecting"
                  ? "Đang kết nối AI..."
                  : connectionState === "unavailable"
                    ? "AI tạm gián đoạn"
                    : "Sẵn sàng hỗ trợ"}
              </div>
              <div className="chat-messages">
                {messages.map((message, index) => (
                  <div className={`chat-bubble ${message.role}`} key={`${message.role}-${index}`}>
                    <p>{message.text}</p>
                    {message.sources?.length ? <small>Nguồn tham khảo: {message.sources.length}</small> : null}
                  </div>
                ))}
                {loading ? <div className="chat-bubble assistant">Đang trả lời...</div> : null}
              </div>
              <form className="chat-form" onSubmit={sendMessage}>
                <input
                  id="ai-chat-input"
                  value={input}
                  onChange={(event) => setInput(event.target.value)}
                  placeholder="Hỏi về xe, giá, lịch lái thử..."
                  aria-label="Tin nhắn gửi AI"
                />
                <button type="submit" disabled={loading}>
                  Gửi
                </button>
              </form>
            </>
          ) : null}

          {mode === "live" ? <LiveChatPanel variant="widget" /> : null}

          {mode ? (
            <button className="chat-mode-back" type="button" onClick={() => setMode("")}>
              Chọn chức năng khác
            </button>
          ) : null}
        </section>
      ) : null}
      <button className="chat-launch" type="button" onClick={() => {
        setOpen(true);
        setMode("");
      }}>
        Chat
      </button>
    </div>
  );
}
