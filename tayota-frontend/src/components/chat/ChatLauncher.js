"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import { sendAiChatMessage } from "@/lib/services/chat";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import { getCurrentUser, onSessionChange } from "@/lib/session";
import { statusLabel } from "@/lib/format";

const UNAVAILABLE_TEXT =
  "AI tạm gián đoạn. Vui lòng thử lại sau hoặc chuyển sang live chat để nhân viên Tayota hỗ trợ trực tiếp.";
const CUSTOMER_LIVE_CHAT_ROLES = new Set(["USER", "CUSTOMER"]);
const URL_PATTERN = /https?:\/\/[^\s<>()]+[^\s<>().,;:!?]/g;

function splitMessageText(text = "") {
  const normalized = String(text).replace(/\r\n/g, "\n").trim();
  if (!normalized) return [];

  const existingParagraphs = normalized
    .split(/\n{1,}/)
    .map((item) => item.trim())
    .filter(Boolean);

  if (existingParagraphs.length > 1) return existingParagraphs;
  if (normalized.length <= 180) return [normalized];

  const sentences = normalized.split(/(?<=[.!?])\s+/);
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

  paragraphs.flatMap(splitQuestionParagraph).forEach((paragraph) => {
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
  return paragraphs.length ? paragraphs.map((paragraph, index) => <p key={`${index}-${paragraph.slice(0, 12)}`}>{renderTextWithLinks(paragraph)}</p>) : <p />;
}

function renderTextWithLinks(text) {
  const nodes = [];
  let lastIndex = 0;
  for (const match of text.matchAll(URL_PATTERN)) {
    const url = match[0];
    const index = match.index || 0;
    if (index > lastIndex) nodes.push(text.slice(lastIndex, index));
    nodes.push(
      <a href={url} key={`${url}-${index}`} target="_blank" rel="noreferrer">
        {url}
      </a>
    );
    lastIndex = index + url.length;
  }
  if (lastIndex < text.length) nodes.push(text.slice(lastIndex));
  return nodes.length ? nodes : text;
}

function canUseCustomerLiveChat(user) {
  return !user || CUSTOMER_LIVE_CHAT_ROLES.has(user.role);
}

function liveStatusClass(status) {
  return `status-${String(status || "idle").toLowerCase()}`;
}

function getChatWidgetSize(isOpen, mode) {
  if (typeof window === "undefined") return { width: 104, height: 104 };
  const mobile = window.matchMedia("(max-width: 620px)").matches;
  const launchSize = mobile ? 86 : 104;
  if (!isOpen) return { width: launchSize, height: launchSize };
  const panelGap = mobile ? 24 : 32;
  const panelHeightGap = mobile ? 120 : 136;
  const panelWidth = mode === "live" ? 680 : 600;
  const panelHeight = mode === "live" ? 760 : 740;
  return {
    width: Math.min(panelWidth, window.innerWidth - panelGap),
    height: Math.min(panelHeight, window.innerHeight - panelHeightGap) + launchSize + 12,
  };
}

function clampChatPosition(nextLeft, nextTop, isOpen, mode) {
  if (typeof window === "undefined") return { left: nextLeft, top: nextTop };
  const margin = 12;
  const size = getChatWidgetSize(isOpen, mode);
  const maxLeft = Math.max(margin, window.innerWidth - size.width - margin);
  const maxTop = Math.max(margin, window.innerHeight - size.height - margin);
  return {
    left: Math.min(Math.max(margin, nextLeft), maxLeft),
    top: Math.min(Math.max(margin, nextTop), maxTop),
  };
}

function getDefaultChatPosition(isOpen, mode) {
  if (typeof window === "undefined") return null;
  const size = getChatWidgetSize(isOpen, mode);
  return clampChatPosition(window.innerWidth - size.width - 22, window.innerHeight - size.height - 22, isOpen, mode);
}

function isInteractiveDragTarget(target) {
  if (target?.closest?.(".chat-launch")) return false;
  return Boolean(target?.closest?.("button, a, input, textarea, select, label, [data-no-drag='true']"));
}

export default function ChatLauncher() {
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState("");
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [connectionState, setConnectionState] = useState("ready");
  const [liveStatus, setLiveStatus] = useState("idle");
  const [currentUser, setCurrentUser] = useState(null);
  const [position, setPosition] = useState(null);
  const [dragging, setDragging] = useState(false);
  const dragRef = useRef({
    active: false,
    moved: false,
    pointerId: null,
    offsetX: 0,
    offsetY: 0,
    nextLeft: 22,
    nextTop: 22,
    frame: 0,
    captureTarget: null,
  });
  const latestPositionRef = useRef({ left: 22, top: 22 });
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
    setPosition(getDefaultChatPosition(false, ""));
  }, []);

  useEffect(() => {
    if (position) latestPositionRef.current = position;
  }, [position]);

  useEffect(() => {
    setPosition((current) => {
      if (!open) return getDefaultChatPosition(false, "");
      const basePosition = current || getDefaultChatPosition(open, mode);
      return clampChatPosition(basePosition.left, basePosition.top, open, mode);
    });
  }, [open, mode]);

  useEffect(() => {
    function handleResize() {
      setPosition((current) => {
        const basePosition = current || getDefaultChatPosition(open, mode);
        return clampChatPosition(basePosition.left, basePosition.top, open, mode);
      });
    }

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, [open, mode]);

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

  useEffect(() => {
    if (mode !== "live") setLiveStatus("idle");
  }, [mode]);

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
  const widgetStyle = position
    ? { left: position.left, top: position.top, right: "auto", bottom: "auto" }
    : { right: 22, bottom: 22, left: "auto", top: "auto" };

  function commitDragPosition(left, top) {
    const nextPosition = clampChatPosition(left, top, open, mode);
    latestPositionRef.current = nextPosition;
    setPosition(nextPosition);
  }

  function beginDrag(event) {
    if (event.button !== 0 || isInteractiveDragTarget(event.target)) return;
    event.preventDefault();
    const widget = event.currentTarget.closest(".chat-widget");
    const rect = widget?.getBoundingClientRect();
    if (!rect) return;
    const current = dragRef.current;
    current.active = true;
    current.moved = false;
    current.pointerId = event.pointerId;
    current.offsetX = event.clientX - rect.left;
    current.offsetY = event.clientY - rect.top;
    current.nextLeft = latestPositionRef.current.left;
    current.nextTop = latestPositionRef.current.top;
    current.captureTarget = event.currentTarget;
    setDragging(true);
    event.currentTarget.setPointerCapture?.(event.pointerId);
    window.addEventListener("pointermove", moveDrag);
    window.addEventListener("pointerup", endDrag, { once: true });
    window.addEventListener("pointercancel", endDrag, { once: true });
  }

  function moveDrag(event) {
    const current = dragRef.current;
    if (!current.active || current.pointerId !== event.pointerId) return;
    event.preventDefault();
    const nextLeft = event.clientX - current.offsetX;
    const nextTop = event.clientY - current.offsetY;
    if (Math.abs(nextLeft - latestPositionRef.current.left) > 4 || Math.abs(nextTop - latestPositionRef.current.top) > 4) {
      current.moved = true;
    }
    current.nextLeft = nextLeft;
    current.nextTop = nextTop;
    if (current.frame) return;
    current.frame = window.requestAnimationFrame(() => {
      current.frame = 0;
      commitDragPosition(current.nextLeft, current.nextTop);
    });
  }

  function endDrag(event) {
    const current = dragRef.current;
    if (!current.active || current.pointerId !== event.pointerId) return;
    current.active = false;
    window.removeEventListener("pointermove", moveDrag);
    window.removeEventListener("pointerup", endDrag);
    window.removeEventListener("pointercancel", endDrag);
    if (current.frame) {
      window.cancelAnimationFrame(current.frame);
      current.frame = 0;
    }
    commitDragPosition(current.nextLeft, current.nextTop);
    current.captureTarget?.releasePointerCapture?.(event.pointerId);
    current.captureTarget = null;
    setDragging(false);
  }

  function openLauncher() {
    if (dragRef.current.moved) {
      dragRef.current.moved = false;
      return;
    }
    setOpen(true);
    setMode("");
  }

  return (
    <div
      className={`chat-widget ${dragging ? "is-dragging" : ""}`}
      id="ai-chat"
      style={widgetStyle}
    >
      {open ? (
        <section className={`chat-panel ${mode === "live" ? "chat-panel-live" : ""}`} aria-label="Tư vấn Tayota">
          <div
            className="chat-head chat-drag-handle"
            onPointerDown={beginDrag}
          >
            <div>
              <span className="eyebrow">Tayota concierge</span>
              <strong>{mode === "live" ? "Tư vấn trực tiếp" : mode === "ai" ? "Tư vấn AI" : "Chọn kênh hỗ trợ"}</strong>
            </div>
            {mode === "live" ? (
              <span className={`status-pill ${liveStatusClass(liveStatus)}`}>{statusLabel(liveStatus.toUpperCase())}</span>
            ) : null}
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

          {mode === "live" ? <LiveChatPanel variant="widget" showHeader={false} onStatusChange={setLiveStatus} /> : null}

          {mode ? (
            <button className="chat-mode-back" type="button" onClick={() => setMode("")}>
              Chọn chức năng khác
            </button>
          ) : null}
        </section>
      ) : null}
      {!open ? <div className="chat-greeting">Xin chào</div> : null}
      <button
        className="chat-launch"
        type="button"
        aria-label="Mở chat Tayota"
        onClick={openLauncher}
        onPointerDown={beginDrag}
      >
        <Image src="/images/chatbot-floating.png" alt="" width={104} height={104} aria-hidden="true" priority draggable={false} />
      </button>
    </div>
  );
}
