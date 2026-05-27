"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import { sendAiChatMessage } from "@/lib/services/chat";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import { getCurrentUser, onSessionChange } from "@/lib/session";

const UNAVAILABLE_TEXT =
  "AI tạm gián đoạn. Vui lòng thử lại sau hoặc chuyển sang live chat để nhân viên Tayota hỗ trợ trực tiếp.";
const CUSTOMER_LIVE_CHAT_ROLES = new Set(["USER", "CUSTOMER"]);

function splitMessageText(text = "") {
  const normalized = String(text).replace(/\r\n/g, "\n").trim();
  if (!normalized) return [];

  const existingParagraphs = normalized
    .split(/\n{1,}/)
    .map((item) => item.trim())
    .filter(Boolean);

  if (existingParagraphs.length > 1) return existingParagraphs;
  if (normalized.length <= 180) return [normalized];

  const sentences = normalized.match(/[^.!?]+[.!?]+(?:\s+|$)|[^.!?]+$/g) || [normalized];
  const paragraphs = [];
  let current = "";

  sentences.forEach((sentence) => {
    const cleanSentence = sentence.trim();
    if (!cleanSentence) return;

    const next = current ? `${current} ${cleanSentence}` : cleanSentence;
    if (next.length > 220 && current) {
      paragraphs.push(current);
      current = cleanSentence;
    } else {
      current = next;
    }
  });

  if (current) paragraphs.push(current);
  return paragraphs;
}

function splitQuestionParagraph(paragraph) {
  const questionMatches = paragraph.match(/[^?]+\?/g);
  if (!questionMatches?.length) return [paragraph];

  const parts = [];
  let remaining = paragraph;

  questionMatches.forEach((question) => {
    const index = remaining.indexOf(question);
    const before = remaining.slice(0, index).trim();
    if (before) parts.push(before);
    parts.push(question.trim());
    remaining = remaining.slice(index + question.length).trim();
  });

  if (remaining) parts.push(remaining);
  return parts;
}

function splitSentenceParagraph(paragraph) {
  return (paragraph.match(/[^.!?]+[.!?]+(?:\s+|$)|[^.!?]+$/g) || [paragraph])
    .map((item) => item.trim())
    .filter(Boolean);
}

function isFollowUpPrompt(paragraph) {
  const normalized = paragraph.toLowerCase();
  return (
    paragraph.endsWith("?") ||
    normalized.includes("vui lòng cho tôi biết") ||
    normalized.includes("hãy cho tôi biết") ||
    normalized.includes("cho tôi biết!") ||
    normalized.includes("cần thêm thông tin") ||
    normalized.includes("muốn so sánh")
  );
}

function splitAssistantReplies(text = "") {
  const replies = [];
  let mainParagraphs = [];
  const normalized = String(text).replace(/\r\n/g, "\n").trim();
  const paragraphs = normalized
    .split(/\n{1,}/)
    .map((item) => item.trim())
    .filter(Boolean);

  paragraphs.flatMap(splitSentenceParagraph).flatMap(splitQuestionParagraph).forEach((paragraph) => {
    if (isFollowUpPrompt(paragraph)) {
      if (mainParagraphs.length) {
        replies.push(mainParagraphs.join("\n"));
        mainParagraphs = [];
      }
      replies.push(paragraph);
    } else {
      mainParagraphs.push(paragraph);
    }
  });

  if (mainParagraphs.length) replies.push(mainParagraphs.join("\n"));
  return replies.length ? replies : [text];
}

function MessageText({ text }) {
  const paragraphs = splitMessageText(text);
  return paragraphs.length ? paragraphs.map((paragraph, index) => <p key={`${index}-${paragraph.slice(0, 12)}`}>{paragraph}</p>) : <p />;
}

function canUseCustomerLiveChat(user) {
  return !user || CUSTOMER_LIVE_CHAT_ROLES.has(user.role);
}

export default function ChatLauncher() {
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState("");
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [connectionState, setConnectionState] = useState("ready");
  const [currentUser, setCurrentUser] = useState(null);
  const [messages, setMessages] = useState([
    { role: "assistant", text: "Xin chào, tôi có thể tư vấn dòng xe, lịch lái thử và dịch vụ Tayota." },
  ]);

  useEffect(() => {
    setCurrentUser(getCurrentUser());
    return onSessionChange(() => setCurrentUser(getCurrentUser()));
  }, []);

  useEffect(() => {
    if (mode === "live" && !canUseCustomerLiveChat(currentUser)) setMode("");
  }, [currentUser, mode]);

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
      const answerText = result?.answer || result?.message || "Tôi đã nhận được yêu cầu của bạn.";
      setConnectionState("ready");
      setMessages((items) => [
        ...items,
        ...splitAssistantReplies(answerText).map((reply) => ({ role: "assistant", text: reply })),
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

  const liveChatAllowed = canUseCustomerLiveChat(currentUser);

  return (
    <div className="chat-widget" id="ai-chat">
      {open ? (
        <section className={`chat-panel ${mode === "live" ? "chat-panel-live" : ""}`} aria-label="Tư vấn Tayota">
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
              {liveChatAllowed ? (
                <button className="choice-card" type="button" onClick={() => setMode("live")}>
                  <strong>Tư vấn trực tuyến</strong>
                  <span>Trao đổi realtime với tư vấn viên Tayota.</span>
                </button>
              ) : null}
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
                    <MessageText text={message.text} />
                  </div>
                ))}
                {loading ? <div className="chat-bubble assistant"><p>Đang trả lời...</p></div> : null}
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
      {!open ? <div className="chat-greeting">Xin chào</div> : null}
      <button className="chat-launch" type="button" aria-label="Mở chat Tayota" onClick={() => {
        setOpen(true);
        setMode("");
      }}>
        <Image src="/images/chatbot-floating.png" alt="" width={104} height={104} aria-hidden="true" priority />
      </button>
    </div>
  );
}
