"use client";

import { useEffect, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import { getMyAppointmentDetail, getMyAppointments } from "@/lib/services/appointments";
import { getMyVehicles } from "@/lib/services/car";
import { getMyReviews } from "@/lib/services/reviews";
import { statusLabel, unwrapList } from "@/lib/format";

function formatDateTime(value) {
  if (!value) return "Chưa cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function reviewTypeLabel(type) {
  if (type === "SERVICE") return "Dịch vụ";
  if (type === "TEST_DRIVE") return "Lái thử";
  return "Đánh giá";
}

function appointmentTypeLabel(type) {
  return type === "SERVICE" ? "Dịch vụ" : "Lái thử";
}

function vehicleStatusLabel(status) {
  if (status === "SOLD") return "Đang sở hữu";
  if (status === "MAINTENANCE") return "Đang bảo dưỡng";
  if (status === "IN_STOCK") return "Trong kho";
  return statusLabel(status);
}

function ratingText(value) {
  const rating = Number(value || 0);
  if (!rating) return "Chưa đánh giá";
  return `${"★".repeat(rating)}${"☆".repeat(Math.max(0, 5 - rating))} ${rating}/5`;
}

function compactReviewComment(item) {
  return item.serviceComment || item.mechanicComment || statusLabel(item.status) || "Chưa có nhận xét.";
}

export default function CustomerDashboard() {
  const [tab, setTab] = useState("profile");
  const [data, setData] = useState({ appointments: [], reviews: [], vehicles: [] });
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [selectedReview, setSelectedReview] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const [appointments, reviews, vehicles] = await Promise.all([
        getMyAppointments(),
        getMyReviews(),
        getMyVehicles(),
      ]);
      setData({
        appointments: unwrapList(appointments),
        reviews: unwrapList(reviews),
        vehicles: unwrapList(vehicles),
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
        <button className={tab === "vehicles" ? "active" : ""} type="button" onClick={() => setTab("vehicles")}>Xe cá nhân</button>
        <button className={tab === "appointments" ? "active" : ""} type="button" onClick={() => setTab("appointments")}>Lịch của tôi</button>
        <button className={tab === "reviews" ? "active" : ""} type="button" onClick={() => setTab("reviews")}>Đánh giá</button>
        <button className={tab === "chat" ? "active" : ""} type="button" onClick={() => setTab("chat")}>Live chat</button>
      </nav>

      {tab === "profile" ? <ProfilePanel eyebrow="Customer" heading="Hồ sơ cá nhân" /> : null}

      {tab === "vehicles" ? <section className="ops-panel wide" id="user-vehicles">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Vehicles</p>
            <h2>Xe cá nhân</h2>
          </div>
        </div>
        {error ? <div className="status-box">{error}</div> : null}
        <div className="customer-card-grid">
          {data.vehicles.map((vehicle) => (
            <article className="customer-info-card" key={vehicle.vinId}>
              <div className="customer-card-head">
                <div>
                  <span>{vehicle.carVersionName || "Xe Tayota"}</span>
                  <strong>{vehicle.vinId}</strong>
                </div>
                <small className="status-pill">{vehicleStatusLabel(vehicle.status)}</small>
              </div>
              <dl className="mini-meta">
                <div><dt>Đại lý</dt><dd>{vehicle.dealershipId || "Đang cập nhật"}</dd></div>
                <div><dt>Ngày gán</dt><dd>{formatDateTime(vehicle.assignedAt)}</dd></div>
                <div><dt>Chủ xe</dt><dd>{vehicle.customerFullName || "Tài khoản của tôi"}</dd></div>
              </dl>
              <div className="row-actions">
                <a className="btn btn-ghost" href={`/appointments/service?vinId=${encodeURIComponent(vehicle.vinId)}`}>Đặt lịch dịch vụ</a>
                {vehicle.carVersionId ? <a className="btn btn-ghost" href={`/vehicles/${vehicle.carVersionId}`}>Xem mẫu xe</a> : null}
              </div>
            </article>
          ))}
          {!data.vehicles.length && !loading ? <div className="status-box">Chưa có xe cá nhân được gán vào tài khoản.</div> : null}
        </div>
      </section> : null}

      {tab === "appointments" ? <section className="ops-panel wide" id="user-appointments">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Appointments</p>
            <h2>Lịch hẹn của tôi</h2>
          </div>
        </div>
        {error ? <div className="status-box">{error}</div> : null}
        <div className="customer-card-grid">
          {data.appointments.map((item) => (
            <article className="customer-info-card" key={item.id}>
              <div className="customer-card-head">
                <div>
                  <span>{appointmentTypeLabel(item.type)}</span>
                  <strong>{item.appointmentDate || "Chưa có ngày"}</strong>
                </div>
                <small className="status-pill">{statusLabel(item.status)}</small>
              </div>
              <p>{item.startTime || "--:--"} - {item.endTime || "--:--"}</p>
              <dl className="mini-meta">
                <div><dt>Xe/VIN</dt><dd>{item.vinId || item.carVersionId || "Đang cập nhật"}</dd></div>
                <div><dt>Ghi chú</dt><dd>{item.notes || "Không có"}</dd></div>
              </dl>
              <div className="row-actions">
                <button className="btn btn-ghost" type="button" onClick={() => openAppointment(item.id)}>Xem chi tiết</button>
              </div>
            </article>
          ))}
          {!data.appointments.length && !loading ? <div className="status-box">Chưa có lịch hẹn.</div> : null}
        </div>
        {detailLoading ? <div className="status-box">Đang tải chi tiết lịch hẹn...</div> : null}
      </section> : null}

      {tab === "reviews" ? <section className="ops-panel wide" id="user-reviews">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Reviews</p>
            <h2>Đánh giá của tôi</h2>
          </div>
        </div>
        {error ? <div className="status-box">{error}</div> : null}
        <div className="customer-card-grid">
          {data.reviews.map((item) => (
            <article className="customer-info-card" key={item.id}>
              <div className="customer-card-head">
                <div>
                  <span>{reviewTypeLabel(item.reviewType)}</span>
                  <strong>{ratingText(item.serviceRating)}</strong>
                </div>
                <small className="status-pill">{statusLabel(item.status)}</small>
              </div>
              <p>{compactReviewComment(item)}</p>
              <small>{item.submittedAt ? `Đã gửi ${formatDateTime(item.submittedAt)}` : "Chưa gửi đánh giá"}</small>
              <div className="row-actions">
                <button className="btn btn-ghost" type="button" onClick={() => setSelectedReview(item)}>Xem chi tiết</button>
              </div>
            </article>
          ))}
          {!data.reviews.length && !loading ? <div className="status-box">Chưa có đánh giá.</div> : null}
        </div>
      </section> : null}

      {tab === "chat" ? <div className="wide workspace-chat-panel" id="user-live-chat">
        <LiveChatPanel />
      </div> : null}

      {selectedAppointment ? (
        <div className="manager-modal-backdrop detail-modal-backdrop" role="presentation" onClick={() => setSelectedAppointment(null)}>
          <section className="detail-modal" role="dialog" aria-modal="true" aria-labelledby="appointment-detail-title" onClick={(event) => event.stopPropagation()}>
            <header className="manager-modal-head detail-modal-head">
              <div>
                <p className="eyebrow">Chi tiết lịch</p>
                <h2 id="appointment-detail-title">Lịch {appointmentTypeLabel(selectedAppointment.type).toLowerCase()}</h2>
              </div>
              <button className="icon-button" type="button" onClick={() => setSelectedAppointment(null)} aria-label="Đóng chi tiết lịch hẹn">×</button>
            </header>
            <div className="detail-modal-body">
              <span className="status-pill">{statusLabel(selectedAppointment.status)}</span>
              <dl className="summary-list compact">
                <div><dt>Ngày hẹn</dt><dd>{selectedAppointment.appointmentDate}</dd></div>
                <div><dt>Thời gian</dt><dd>{selectedAppointment.startTime} - {selectedAppointment.endTime}</dd></div>
                <div><dt>Đại lý</dt><dd>{selectedAppointment.dealershipId}</dd></div>
                <div><dt>Xe/VIN</dt><dd>{selectedAppointment.vinId || selectedAppointment.carVersionId || "Đang cập nhật"}</dd></div>
                <div><dt>Ghi chú</dt><dd>{selectedAppointment.notes || "Không có"}</dd></div>
                {selectedAppointment.cancelReason ? <div><dt>Lý do</dt><dd>{selectedAppointment.cancelReason}</dd></div> : null}
                <div><dt>Tạo lúc</dt><dd>{formatDateTime(selectedAppointment.createdAt)}</dd></div>
              </dl>
            </div>
          </section>
        </div>
      ) : null}

      {selectedReview ? (
        <div className="manager-modal-backdrop detail-modal-backdrop" role="presentation" onClick={() => setSelectedReview(null)}>
          <section className="detail-modal" role="dialog" aria-modal="true" aria-labelledby="review-detail-title" onClick={(event) => event.stopPropagation()}>
            <header className="manager-modal-head detail-modal-head">
              <div>
                <p className="eyebrow">{reviewTypeLabel(selectedReview.reviewType)}</p>
                <h2 id="review-detail-title">Chi tiết đánh giá</h2>
              </div>
              <button className="icon-button" type="button" onClick={() => setSelectedReview(null)} aria-label="Đóng chi tiết đánh giá">×</button>
            </header>
            <div className="detail-modal-body">
              <span className="status-pill">{statusLabel(selectedReview.status)}</span>
              <dl className="summary-list compact">
                <div><dt>Đánh giá dịch vụ</dt><dd>{ratingText(selectedReview.serviceRating)}</dd></div>
                <div><dt>Nhận xét dịch vụ</dt><dd>{selectedReview.serviceComment || "Không có"}</dd></div>
                <div><dt>Đánh giá kỹ thuật viên</dt><dd>{ratingText(selectedReview.mechanicRating)}</dd></div>
                <div><dt>Nhận xét kỹ thuật viên</dt><dd>{selectedReview.mechanicComment || "Không có"}</dd></div>
                <div><dt>VIN</dt><dd>{selectedReview.vinId || "Không có"}</dd></div>
                <div><dt>Lịch/phiếu dịch vụ</dt><dd>{selectedReview.appointmentId || selectedReview.serviceId || "Không có"}</dd></div>
                <div><dt>Ngày gửi</dt><dd>{formatDateTime(selectedReview.submittedAt)}</dd></div>
              </dl>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  );
}
