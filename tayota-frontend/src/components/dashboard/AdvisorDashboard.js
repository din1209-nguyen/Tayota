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
import {
  getAdvisorAppointments as loadAdvisorAppointments,
  getAdvisorHolidays as loadHolidays,
  getAdvisorTimeSlots as loadSlots,
} from "@/lib/services/appointments";
import { unwrapList } from "@/lib/format";

function mergeSessions(...groups) {
  const map = new Map();
  groups.flat().forEach((session) => {
    if (session?.id) map.set(session.id, session);
  });
  return Array.from(map.values());
}

export default function AdvisorDashboard() {
  const [appointments, setAppointments] = useState([]);
  const [slots, setSlots] = useState([]);
  const [holidays, setHolidays] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [activeSessionId, setActiveSessionId] = useState("");
  const [status, setStatus] = useState("PENDING");
  const [error, setError] = useState("");

  async function load() {
    setError("");
    try {
      const [nextAppointments, nextSlots, nextHolidays, waitingSessions, chattingSessions] = await Promise.all([
        loadAdvisorAppointments(status),
        loadSlots(),
        loadHolidays(),
        getAssistantChatSessions("WAITING"),
        getAssistantChatSessions("CHATTING"),
      ]);
      setAppointments(unwrapList(nextAppointments));
      setSlots(unwrapList(nextSlots));
      setHolidays(unwrapList(nextHolidays));
      setSessions(mergeSessions(unwrapList(waitingSessions), unwrapList(chattingSessions)));
    } catch (caughtError) {
      setError(caughtError.message || "Không thể tải dashboard tư vấn.");
    }
  }

  useEffect(() => {
    let active = true;

    Promise.all([
      loadAdvisorAppointments(status),
      loadSlots(),
      loadHolidays(),
      getAssistantChatSessions("WAITING"),
      getAssistantChatSessions("CHATTING"),
    ])
      .then(([nextAppointments, nextSlots, nextHolidays, waitingSessions, chattingSessions]) => {
        if (!active) return;
        setAppointments(unwrapList(nextAppointments));
        setSlots(unwrapList(nextSlots));
        setHolidays(unwrapList(nextHolidays));
        setSessions(mergeSessions(unwrapList(waitingSessions), unwrapList(chattingSessions)));
      })
      .catch((caughtError) => {
        if (active) setError(caughtError.message || "Không thể tải dashboard tư vấn.");
      });

    return () => {
      active = false;
    };
  }, [status]);

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
      <nav className="role-tabs wide" aria-label="Assistant sections">
        <a href="#assistant-appointments">Lịch hẹn</a>
        <a href="#assistant-slots">Khung giờ</a>
        <a href="#assistant-holidays">Ngày nghỉ</a>
        <a href="#assistant-chat">Live chat</a>
      </nav>

      {error ? <div className="status-box wide">{error}</div> : null}

      <section className="ops-panel wide" id="assistant-appointments">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Advisor</p>
            <h2>Quản lý lịch hẹn</h2>
          </div>
          <select className="field compact-field" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="PENDING">Chờ xác nhận</option>
            <option value="CONFIRMED">Đã xác nhận</option>
            <option value="COMPLETED">Hoàn tất</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
        <div className="ops-list">
          {appointments.length ? appointments.map((item) => (
            <article key={item.id}>
              <strong>{item.type || item.appointmentType}</strong>
              <span>{item.appointmentDate} {item.startTime}</span>
              <small>{item.status}</small>
            </article>
          )) : <div className="status-box">Chưa có lịch hẹn trong trạng thái này.</div>}
        </div>
      </section>

      <section className="ops-panel" id="assistant-slots">
        <p className="eyebrow">Time slots</p>
        <h2>Khung giờ</h2>
        <div className="ops-list">
          {slots.map((slot) => (
            <article key={slot.id}>
              <strong>{slot.appointmentType}</strong>
              <span>{slot.startTime} - {slot.endTime}</span>
            </article>
          ))}
        </div>
      </section>

      <section className="ops-panel" id="assistant-holidays">
        <p className="eyebrow">Holidays</p>
        <h2>Ngày nghỉ</h2>
        <div className="ops-list">
          {holidays.map((holiday) => (
            <article key={holiday.id}>
              <strong>{holiday.holidayDate}</strong>
              <span>{holiday.reason}</span>
            </article>
          ))}
        </div>
      </section>

      <section className="ops-panel" id="assistant-chat">
        <p className="eyebrow">Assistant chat</p>
        <h2>Inbox tư vấn</h2>
        <div className="ops-list">
          {sessions.length ? sessions.map((session) => (
            <article className={activeSessionId === session.id ? "active" : ""} key={session.id}>
              <strong>{session.status}</strong>
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

      {activeSessionId ? <LiveChatPanel mode="assistant" sessionId={activeSessionId} /> : null}
    </div>
  );
}
