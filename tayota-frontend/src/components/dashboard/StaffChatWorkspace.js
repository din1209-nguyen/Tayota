"use client";

import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import {
  assignAssistantChatSession,
  closeAssistantChatSession,
  getAssistantChatSessions,
  getChatWebSocketUrl,
  resolveAssistantChatSession,
} from "@/lib/services/chat";
import { statusLabel, unwrapList } from "@/lib/format";
import { getCurrentUser, onSessionChange } from "@/lib/session";

function mergeSessions(...groups) {
  const map = new Map();
  groups.flat().forEach((session) => {
    if (session?.id) map.set(session.id, session);
  });
  return Array.from(map.values());
}

function sessionCustomerLabel(session) {
  return session.userId ? "Khách hàng thành viên" : "Khách vãng lai";
}

function statusClass(status) {
  return `status-${String(status || "idle").toLowerCase()}`;
}

export default function StaffChatWorkspace({
  eyebrow = "Live chat",
  heading = "Inbox tư vấn realtime",
  emptyPanelMessage = "Chọn một phiên để bắt đầu hỗ trợ khách hàng.",
}) {
  const [sessions, setSessions] = useState([]);
  const [activeSessionId, setActiveSessionId] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyAction, setBusyAction] = useState("");
  const [error, setError] = useState("");
  const [currentUser, setCurrentUser] = useState(null);

  const activeSession = sessions.find((session) => session.id === activeSessionId);
  const activeSessionReadOnly = Boolean(
    activeSession?.assignedAssistantId &&
    currentUser?.id &&
    activeSession.assignedAssistantId !== currentUser.id
  );

  const load = useCallback(async ({ showLoading = false } = {}) => {
    if (showLoading) setLoading(true);
    setError("");
    try {
      const [waitingSessions, chattingSessions] = await Promise.all([
        getAssistantChatSessions("WAITING"),
        getAssistantChatSessions("CHATTING"),
      ]);
      setSessions(mergeSessions(unwrapList(waitingSessions), unwrapList(chattingSessions)));
    } catch (caughtError) {
      setError(caughtError.message || "Không thể tải danh sách live chat.");
    } finally {
      if (showLoading) setLoading(false);
    }
  }, []);

  useEffect(() => {
    load({ showLoading: true });
  }, [load]);

  useEffect(() => {
    setCurrentUser(getCurrentUser());
    return onSessionChange(() => setCurrentUser(getCurrentUser()));
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
      onStompError: (frame) => {
        setError(frame.headers?.message || "Không thể nhận cập nhật live chat.");
      },
    });

    client.activate();
    return () => client.deactivate();
  }, []);

  async function runAction(actionName, action, sessionId) {
    setBusyAction(`${actionName}:${sessionId}`);
    setError("");
    try {
      await action(sessionId);
      if (actionName === "assign") setActiveSessionId(sessionId);
      if (["resolve", "close"].includes(actionName) && activeSessionId === sessionId) {
        setActiveSessionId("");
      }
      await load();
    } catch (caughtError) {
      setError(caughtError.message || "Không thể cập nhật phiên chat.");
    } finally {
      setBusyAction("");
    }
  }

  function isBusy(actionName, sessionId) {
    return busyAction === `${actionName}:${sessionId}`;
  }

  function isAssignedToCurrentUser(session) {
    return Boolean(session?.assignedAssistantId && currentUser?.id && session.assignedAssistantId === currentUser.id);
  }

  return (
    <div className="ops-grid workspace-tabs-layout staff-chat-workspace">
      <section className="ops-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h2>{heading}</h2>
          </div>
          <button className="btn btn-ghost" type="button" onClick={() => load({ showLoading: true })} disabled={loading || Boolean(busyAction)}>
            Tải lại
          </button>
        </div>

        <div className="live-chat-feedback" aria-live="polite">
          {error ? <div className="status-box">{error}</div> : null}
        </div>

        <div className="ops-list staff-session-list">
          {loading ? <div className="status-box">Đang tải phiên chat...</div> : null}
          {!loading && sessions.length ? sessions.map((session) => (
            <article className={activeSessionId === session.id ? "active" : ""} key={session.id}>
              <div className="ops-panel-head">
                <strong>{sessionCustomerLabel(session)}</strong>
                <span className={`status-pill ${statusClass(session.status)}`}>{statusLabel(session.status)}</span>
              </div>
              <small>Mã phiên: {session.id}</small>
              {session.status === "CHATTING" && !isAssignedToCurrentUser(session) ? (
                <small>Đang được xử lý bởi nhân viên khác</small>
              ) : null}
              <div className="row-actions">
                {session.status === "WAITING" ? (
                  <button
                    className="btn btn-ghost"
                    type="button"
                    disabled={Boolean(busyAction)}
                    onClick={() => runAction("assign", assignAssistantChatSession, session.id)}
                  >
                    {isBusy("assign", session.id) ? "Đang nhận..." : "Nhận phiên"}
                  </button>
                ) : null}
                <button className="btn btn-ghost" type="button" disabled={Boolean(busyAction)} onClick={() => setActiveSessionId(session.id)}>
                  Mở hội thoại
                </button>
                {session.status === "CHATTING" && isAssignedToCurrentUser(session) ? (
                  <>
                    <button
                      className="btn btn-ghost"
                      type="button"
                      disabled={Boolean(busyAction)}
                      onClick={() => runAction("resolve", resolveAssistantChatSession, session.id)}
                    >
                      {isBusy("resolve", session.id) ? "Đang lưu..." : "Hoàn tất"}
                    </button>
                    <button
                      className="btn btn-ghost"
                      type="button"
                      disabled={Boolean(busyAction)}
                      onClick={() => runAction("close", closeAssistantChatSession, session.id)}
                    >
                      {isBusy("close", session.id) ? "Đang đóng..." : "Đóng"}
                    </button>
                  </>
                ) : null}
              </div>
            </article>
          )) : null}
          {!loading && !sessions.length ? <div className="status-box">Chưa có phiên chat cần xử lý.</div> : null}
        </div>
      </section>

      {activeSessionId ? (
        <LiveChatPanel
          mode="assistant"
          sessionId={activeSessionId}
          readOnly={activeSessionReadOnly}
          onRestrictedAction={() => load()}
        />
      ) : (
        <section className="ops-panel staff-chat-placeholder">
          <p className="eyebrow">Phiên chat</p>
          <h2>Hỗ trợ khách hàng</h2>
          <div className="status-box">{emptyPanelMessage}</div>
        </section>
      )}
    </div>
  );
}
