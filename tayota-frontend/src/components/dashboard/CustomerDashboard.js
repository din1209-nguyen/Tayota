"use client";

import { useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import { getMyAppointmentDetail, getMyAppointments } from "@/lib/services/appointments";
import { getMyReviews } from "@/lib/services/reviews";
import { statusLabel, unwrapList } from "@/lib/format";

export default function CustomerDashboard() {
  const [tab, setTab] = useState("profile");
  const [data, setData] = useState({ appointments: [], reviews: [] });
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const [appointments, reviews] = await Promise.all([
        getMyAppointments(),
        getMyReviews(),
      ]);
      setData({
        appointments: unwrapList(appointments),
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

  async function openAppointment(id) {
    setDetailLoading(true);
    setError("");
    try {
      setSelectedAppointment(await getMyAppointmentDetail(id));
    } catch (caughtError) {
      setError(caughtError.message || "Không thể tải chi tiết lịch hẹn.");
    } finally {
      setDetailLoading(false);
    }
  }

  return (
    <div className="ops-grid workspace-tabs-layout">
      <nav className="role-tabs wide" aria-label="Các mục người dùng">
        <button className={tab === "profile" ? "active" : ""} type="button" onClick={() => setTab("profile")}>Tài khoản</button>
        <button className={tab === "appointments" ? "active" : ""} type="button" onClick={() => setTab("appointments")}>Lịch của tôi</button>
        <button className={tab === "reviews" ? "active" : ""} type="button" onClick={() => setTab("reviews")}>Đánh giá</button>
        <button className={tab === "chat" ? "active" : ""} type="button" onClick={() => setTab("chat")}>Live chat</button>
      </nav>

      {tab === "profile" ? <ProfilePanel eyebrow="Customer" heading="Hồ sơ cá nhân" /> : null}

      {tab === "appointments" ? <section className="ops-panel wide" id="user-appointments">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Appointments</p>
            <h2>Lịch hẹn của tôi</h2>
          </div>
        </div>
        <div className="ops-list">
          {data.appointments.map((item) => (
            <article key={item.id}>
              <strong>{item.type === "SERVICE" ? "Dịch vụ" : "Lái thử"}</strong>
              <span>{item.appointmentDate} {item.startTime}</span>
              <small>{statusLabel(item.status)}</small>
              <div className="row-actions">
                <button className="btn btn-ghost" type="button" onClick={() => openAppointment(item.id)}>Xem chi tiết</button>
              </div>
            </article>
          ))}
          {!data.appointments.length && !loading ? <p>Chưa có lịch hẹn.</p> : null}
        </div>
        {detailLoading ? <div className="status-box">Đang tải chi tiết lịch hẹn...</div> : null}
        {selectedAppointment ? (
          <div className="inline-form">
            <div className="ops-panel-head">
              <div>
                <p className="eyebrow">Chi tiết lịch</p>
                <h3>{selectedAppointment.type === "SERVICE" ? "Lịch dịch vụ" : "Lịch lái thử"}</h3>
              </div>
              <span className="status-pill">{statusLabel(selectedAppointment.status)}</span>
            </div>
            <dl className="summary-list compact">
              <div><dt>Ngày hẹn</dt><dd>{selectedAppointment.appointmentDate}</dd></div>
              <div><dt>Thời gian</dt><dd>{selectedAppointment.startTime} - {selectedAppointment.endTime}</dd></div>
              <div><dt>Đại lý</dt><dd>{selectedAppointment.dealershipId}</dd></div>
              <div><dt>Xe/VIN</dt><dd>{selectedAppointment.vinId || selectedAppointment.carVersionId || "Đang cập nhật"}</dd></div>
              <div><dt>Ghi chú</dt><dd>{selectedAppointment.notes || "Không có"}</dd></div>
              {selectedAppointment.cancelReason ? <div><dt>Lý do</dt><dd>{selectedAppointment.cancelReason}</dd></div> : null}
            </dl>
          </div>
        ) : null}
      </section> : null}

      {tab === "reviews" ? <section className="ops-panel wide" id="user-reviews">
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
      </section> : null}

      {tab === "chat" ? <div className="wide workspace-chat-panel" id="user-live-chat">
        <LiveChatPanel />
      </div> : null}
    </div>
  );
}
