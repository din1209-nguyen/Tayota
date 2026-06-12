"use client";

import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import {
  assignAssistantChatSession,
  closeAssistantChatSession,
  getAssistantChatSessions,
  getChatWebSocketUrl,
} from "@/lib/services/chat";
import { statusLabel, unwrapList } from "@/lib/format";
import { getCurrentUser, onSessionChange } from "@/lib/session";

const INBOX_STATUSES = ["WAITING", "CHATTING", "CLOSED"];
const SESSION_STATUS_PRIORITY = {
  WAITING: 0,
  CHATTING: 1,
  CLOSED: 2,
};

function sessionSortTime(session) {
  return new Date(session?.lastMessageAt || session?.updatedAt || session?.createdAt || 0).getTime();
}

function mergeSessions(...groups) {
  const map = new Map();
  groups.flat().forEach((session) => {
    if (session?.id) map.set(session.id, session);
  });
  return Array.from(map.values()).sort((left, right) => {
    const leftPriority = SESSION_STATUS_PRIORITY[left.status] ?? 99;
    const rightPriority = SESSION_STATUS_PRIORITY[right.status] ?? 99;
    if (leftPriority !== rightPriority) return leftPriority - rightPriority;
    return sessionSortTime(right) - sessionSortTime(left);
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
  if (session?.status === "CLOSED") return "Phiên đã đóng. Bạn vẫn có thể xem lại lịch sử tin nhắn.";
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

  const syncSessionsAfterRealtimeMessage = useCallback(() => {
    load();
  }, [load]);

  useEffect(() => {
    load({ showLoading: true });
  }, [load]);

  useEffect(() => {
    setCurrentUser(getCurrentUser());
    return onSessionChange(() => setCurrentUser(getCurrentUser()));
  }, []);

  useEffect(() => {
    let manuallyClosed = false;
    const client = new Client({
      brokerURL: getChatWebSocketUrl(),
      reconnectDelay: 5000,
      onConnect: () => {
        setError("");
        client.subscribe("/topic/assistant.chat.sessions", (frame) => {
          const payload = JSON.parse(frame.body);
          if (!payload?.id) return;
          updateSession(payload);
        });
        load();
      },
      onStompError: (frame) => {
        setError(frame.headers?.message || "Không thể nhận cập nhật live chat.");
      },
      onWebSocketClose: () => {
        if (manuallyClosed) return;
        setError("Mất kết nối live chat realtime. Hệ thống sẽ tự kết nối lại.");
      },
      onWebSocketError: () => {
        if (manuallyClosed) return;
        setError("Không thể kết nối live chat realtime. Vui lòng kiểm tra gateway WebSocket.");
      },
    });

    client.activate();
    return () => {
      manuallyClosed = true;
      client.deactivate();
    };
  }, [load, updateSession]);

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
        <div className="ops-panel-head staff-chat-sidebar-head">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h2>{heading}</h2>
          </div>
        </div>

        <div className="live-chat-feedback" aria-live="polite">
          {error ? <div className="status-box">{error}</div> : null}
        </div>

        <div className="ops-list staff-session-list">
          {loading ? <div className="status-box">Đang tải phiên chat...</div> : null}
          {!loading && sessions.length ? sessions.map((session) => (
            <article className={activeSessionId === session.id ? "active" : ""} key={session.id}>
              <div className="staff-session-top">
                <strong>{sessionCustomerLabel(session)}</strong>
                <span className={`status-pill ${statusClass(session.status)}`}>{statusLabel(session.status)}</span>
              </div>
              <div className="staff-session-latest">
                <span>Tin mới</span>
                <strong>{sessionLatestMessage(session)}</strong>
              </div>
              {session.status === "CHATTING" && !isAssignedToCurrentUser(session) ? (
                <small className="staff-session-note">Đang được xử lý bởi nhân viên khác</small>
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
                    onClick={() => runAction("close", closeAssistantChatSession, session.id)}
                  >
                    {isBusy("close", session.id) ? "Đang đóng..." : "Kết thúc phiên"}
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
          onMessageReceived={syncSessionsAfterRealtimeMessage}
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
