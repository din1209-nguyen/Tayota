"use client";

import { useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import { getMyAppointments } from "@/lib/services/appointments";
import { getMe } from "@/lib/services/auth";
import { getNotifications, markAllNotificationsAsRead } from "@/lib/services/notifications";
import { getMyReviews } from "@/lib/services/reviews";
import { unwrapList } from "@/lib/format";

export default function CustomerDashboard() {
  const [data, setData] = useState({ me: null, appointments: [], notifications: [], reviews: [] });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const [me, appointments, notifications, reviews] = await Promise.all([
        getMe(),
        getMyAppointments(),
        getNotifications(),
        getMyReviews(),
      ]);
      setData({
        me,
        appointments: unwrapList(appointments),
        notifications: unwrapList(notifications),
        reviews: unwrapList(reviews),
      });
    } catch (caughtError) {
      setError(caughtError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function readAll() {
    await markAllNotificationsAsRead();
    load();
  }

  return (
    <div className="ops-grid">
      <section className="ops-panel">
        <p className="eyebrow">Customer</p>
        <h2>{data.me?.fullname || "Tài khoản của tôi"}</h2>
        {loading ? <p>Đang tải...</p> : null}
        {error ? <div className="status-box">{error}</div> : null}
      </section>

      <section className="ops-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Appointments</p>
            <h2>Lịch hẹn của tôi</h2>
          </div>
        </div>
        <div className="ops-list">
          {data.appointments.map((item) => (
            <article key={item.id}>
              <strong>{item.type || "APPOINTMENT"}</strong>
              <span>{item.appointmentDate} {item.startTime}</span>
              <small>{item.status}</small>
            </article>
          ))}
          {!data.appointments.length && !loading ? <p>Chưa có lịch hẹn.</p> : null}
        </div>
      </section>

      <section className="ops-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Notifications</p>
            <h2>Thông báo</h2>
          </div>
          <button className="btn btn-ghost" type="button" onClick={readAll}>Đã đọc</button>
        </div>
        <div className="ops-list">
          {data.notifications.map((item) => (
            <article key={item.id}>
              <strong>{item.title}</strong>
              <span>{item.content}</span>
              <small>{item.read ? "READ" : "NEW"}</small>
            </article>
          ))}
          {!data.notifications.length && !loading ? <p>Không có thông báo.</p> : null}
        </div>
      </section>

      <section className="ops-panel">
        <p className="eyebrow">Reviews</p>
        <h2>Đánh giá của tôi</h2>
        <div className="ops-list">
          {data.reviews.map((item) => (
            <article key={item.id}>
              <strong>{item.rating || item.score || "Review"}</strong>
              <span>{item.comment || item.content || item.status}</span>
            </article>
          ))}
          {!data.reviews.length && !loading ? <p>Chưa có đánh giá.</p> : null}
        </div>
      </section>

      <LiveChatPanel />
    </div>
  );
}
