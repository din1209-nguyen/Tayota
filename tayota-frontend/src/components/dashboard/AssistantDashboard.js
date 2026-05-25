"use client";

import { Client } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import {
  assignAssistantChatSession,
  closeAssistantChatSession,
  getAssistantChatSessions,
  getChatWebSocketUrl,
  resolveAssistantChatSession,
} from "@/lib/services/chat";
import { statusLabel, unwrapList } from "@/lib/format";

function mergeSessions(...groups) {
  const map = new Map();
  groups.flat().forEach((session) => {
    if (session?.id) map.set(session.id, session);
  });
  return Array.from(map.values());
}

export default function AssistantDashboard() {
  const [sessions, setSessions] = useState([]);
  const [activeSessionId, setActiveSessionId] = useState("");
  const [error, setError] = useState("");

  async function load() {
    setError("");
    try {
      const [waitingSessions, chattingSessions] = await Promise.all([
        getAssistantChatSessions("WAITING"),
        getAssistantChatSessions("CHATTING"),
      ]);
      setSessions(mergeSessions(unwrapList(waitingSessions), unwrapList(chattingSessions)));
    } catch (caughtError) {
      setError(caughtError.message || "Không thể tải live chat.");
    }
  }

  useEffect(() => {
    let active = true;

    Promise.all([
      getAssistantChatSessions("WAITING"),
      getAssistantChatSessions("CHATTING"),
    ])
      .then(([waitingSessions, chattingSessions]) => {
        if (active) setSessions(mergeSessions(unwrapList(waitingSessions), unwrapList(chattingSessions)));
      })
      .catch((caughtError) => {
        if (active) setError(caughtError.message || "Không thể tải live chat.");
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const client = new Client({
      brokerURL: getChatWebSocketUrl(),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe("/topic/assistant.chat.sessions", (frame) => {
          const payload = JSON.parse(frame.body);
          if (!payload?.id) return;
          setSessions((current) => {
            if (!["WAITING", "CHATTING"].includes(payload.status)) {
              return current.filter((session) => session.id !== payload.id);
            }
            return mergeSessions([payload], current);
          });
        });
      },
    });

    client.activate();
    return () => client.deactivate();
  }, []);

  async function assign(sessionId) {
    await assignAssistantChatSession(sessionId);
    setActiveSessionId(sessionId);
    load();
  }

  async function resolve(sessionId) {
    await resolveAssistantChatSession(sessionId);
    if (activeSessionId === sessionId) setActiveSessionId("");
    load();
  }

  async function close(sessionId) {
    await closeAssistantChatSession(sessionId);
    if (activeSessionId === sessionId) setActiveSessionId("");
    load();
  }

  return (
    <div className="ops-grid">
      {error ? <div className="status-box wide">{error}</div> : null}

      <section className="ops-panel">
        <p className="eyebrow">Live chat</p>
        <h2>Inbox tư vấn realtime</h2>
        <div className="ops-list">
          {sessions.length ? sessions.map((session) => (
            <article className={activeSessionId === session.id ? "active" : ""} key={session.id}>
              <strong>{statusLabel(session.status)}</strong>
              <span>{session.customerName || session.id}</span>
              <div className="row-actions">
                <button className="btn btn-ghost" type="button" onClick={() => assign(session.id)}>Nhận</button>
                <button className="btn btn-ghost" type="button" onClick={() => setActiveSessionId(session.id)}>Mở</button>
                <button className="btn btn-ghost" type="button" onClick={() => resolve(session.id)}>Xong</button>
                <button className="btn btn-ghost" type="button" onClick={() => close(session.id)}>Đóng</button>
              </div>
            </article>
          )) : <div className="status-box">Chưa có phiên chat đang chờ.</div>}
        </div>
      </section>

      {activeSessionId ? <LiveChatPanel mode="assistant" sessionId={activeSessionId} /> : (
        <section className="ops-panel">
          <p className="eyebrow">Phiên chat</p>
          <h2>Chọn một phiên để bắt đầu</h2>
          <div className="status-box">Assistant chỉ xử lý live chat realtime, không quản lý lịch hẹn.</div>
        </section>
      )}
    </div>
  );
}
