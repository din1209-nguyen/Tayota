"use client";

import { useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import {
  assignAssistantChatSession,
  getAssistantChatSessions,
  resolveAssistantChatSession,
} from "@/lib/services/chat";
import { getAdvisorAppointments as loadAdvisorAppointments, getAdvisorHolidays as loadHolidays, getAdvisorTimeSlots as loadSlots } from "@/lib/services/appointments";
import { unwrapList } from "@/lib/format";

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
      const [nextAppointments, nextSlots, nextHolidays, nextSessions] = await Promise.all([
        loadAdvisorAppointments(status),
        loadSlots(),
        loadHolidays(),
        getAssistantChatSessions("WAITING"),
      ]);
      setAppointments(unwrapList(nextAppointments));
      setSlots(unwrapList(nextSlots));
      setHolidays(unwrapList(nextHolidays));
      setSessions(unwrapList(nextSessions));
    } catch (caughtError) {
      setError(caughtError.message);
    }
  }

  useEffect(() => {
    let active = true;

    Promise.all([
      loadAdvisorAppointments(status),
      loadSlots(),
      loadHolidays(),
      getAssistantChatSessions("WAITING"),
    ])
      .then(([nextAppointments, nextSlots, nextHolidays, nextSessions]) => {
        if (!active) return;
        setAppointments(unwrapList(nextAppointments));
        setSlots(unwrapList(nextSlots));
        setHolidays(unwrapList(nextHolidays));
        setSessions(unwrapList(nextSessions));
      })
      .catch((caughtError) => {
        if (active) setError(caughtError.message);
      });

    return () => {
      active = false;
    };
  }, [status]);

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

  return (
    <div className="ops-grid">
      {error ? <div className="status-box wide">{error}</div> : null}
      <section className="ops-panel wide">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Advisor</p>
            <h2>Quản lý lịch hẹn</h2>
          </div>
          <select className="field compact-field" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="PENDING">PENDING</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
        </div>
        <div className="ops-list">
          {appointments.map((item) => (
            <article key={item.id}>
              <strong>{item.type}</strong>
              <span>{item.appointmentDate} {item.startTime}</span>
              <small>{item.status}</small>
            </article>
          ))}
        </div>
      </section>

      <section className="ops-panel">
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

      <section className="ops-panel">
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

      <section className="ops-panel">
        <p className="eyebrow">Assistant chat</p>
        <h2>Phiên đang chờ</h2>
        <div className="ops-list">
          {sessions.map((session) => (
            <article key={session.id}>
              <strong>{session.status}</strong>
              <span>{session.id}</span>
              <div className="row-actions">
                <button className="btn btn-ghost" type="button" onClick={() => assign(session.id)}>Nhận</button>
                <button className="btn btn-ghost" type="button" onClick={() => resolve(session.id)}>Xong</button>
              </div>
            </article>
          ))}
        </div>
      </section>

      {activeSessionId ? <LiveChatPanel mode="assistant" sessionId={activeSessionId} /> : null}
    </div>
  );
}
