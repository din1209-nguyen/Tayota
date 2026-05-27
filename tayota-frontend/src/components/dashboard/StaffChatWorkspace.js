"use client";

import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import {
  assignAssistantChatSession,
  getAssistantChatSessions,
  getChatWebSocketUrl,
  resolveAssistantChatSession,
} from "@/lib/services/chat";
import { statusLabel, unwrapList } from "@/lib/format";
import { getCurrentUser, onSessionChange } from "@/lib/session";

const INBOX_STATUSES = ["WAITING", "CHATTING", "RESOLVED"];

function mergeSessions(...groups) {
  const map = new Map();
  groups.flat().forEach((session) => {
    if (session?.id) map.set(session.id, session);
  });
  return Array.from(map.values()).sort((left, right) => {
    const leftTime = new Date(left.lastMessageAt || left.updatedAt || left.createdAt || 0).getTime();
    const rightTime = new Date(right.lastMessageAt || right.updatedAt || right.createdAt || 0).getTime();
    return rightTime - leftTime;
  });
}

function sessionCustomerLabel(session) {
  return session.customerDisplayName || (session.userId ? "Khách hàng thành viên" : "Khách vãng lai");
}

function sessionLatestMessage(session) {
  return session.lastMessageContent || "Chưa có tin nhắn";
}

function statusClass(status) {
  return `status-${String(status || "idle").toLowerCase()}`;
}

function readOnlyMessageForSession(session) {
  if (session?.status === "RESOLVED") return "Phiên đã kết thúc. Bạn vẫn có thể xem lại lịch sử tin nhắn.";
  if (session?.status === "WAITING") return "Nhấn nhận phiên để tiếp tục nhắn trong phiên này.";
  return "Phiên này đang được xử lý bởi nhân viên khác. Bạn chỉ có thể xem lịch sử.";
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
  const activeSessionReadOnly = activeSession
    ? activeSession.status !== "CHATTING" || !isAssignedToCurrentUser(activeSession)
    : true;
  const activeSessionReadOnlyMessage = readOnlyMessageForSession(activeSession);

  const updateSession = useCallback((session) => {
    if (!session?.id) return;
    setSessions((current) => {
      if (!INBOX_STATUSES.includes(session.status)) {
        return current.filter((item) => item.id !== session.id);
      }
      return mergeSessions([session], current);
    });
  }, []);

  const load = useCallback(async ({ showLoading = false } = {}) => {
    if (showLoading) setLoading(true);
    setError("");
    try {
      const sessionGroups = await Promise.all(INBOX_STATUSES.map((status) => getAssistantChatSessions(status)));
      setSessions(mergeSessions(...sessionGroups.map(unwrapList)));
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
          updateSession(payload);
        });
      },
      onStompError: (frame) => {
        setError(frame.headers?.message || "Không thể nhận cập nhật live chat.");
      },
    });

    client.activate();
    return () => client.deactivate();
  }, [updateSession]);

  async function runAction(actionName, action, sessionId) {
    setBusyAction(`${actionName}:${sessionId}`);
    setError("");
    try {
      const updatedSession = await action(sessionId);
      if (updatedSession?.id) {
        setSessions((current) => mergeSessions([updatedSession], current));
      }
      if (actionName === "assign") setActiveSessionId(sessionId);
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
    <div className="ops-grid workspace-tabs-layout staff-chat-workspace fullscreen-chat">
      <section className="ops-panel staff-chat-sidebar">
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
              <small className="staff-session-latest">{sessionLatestMessage(session)}</small>
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
                  <button
                    className="btn btn-ghost"
                    type="button"
                    disabled={Boolean(busyAction)}
                    onClick={() => runAction("resolve", resolveAssistantChatSession, session.id)}
                  >
                    {isBusy("resolve", session.id) ? "Đang kết thúc..." : "Kết thúc phiên"}
                  </button>
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
          readOnlyMessage={activeSessionReadOnlyMessage}
          onSessionUpdate={updateSession}
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
