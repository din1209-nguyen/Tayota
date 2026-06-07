"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import LiveChatPanel from "@/components/chat/LiveChatPanel";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import { getMyAppointmentDetail, getMyAppointments } from "@/lib/services/appointments";
import { getCarVersion, getDealerships, getMyVehicles } from "@/lib/services/car";
import { getMyReviews } from "@/lib/services/reviews";
import { getServiceInvoice, getUserServiceTicketDetail, getUserServiceTickets } from "@/lib/services/workorders";
import { formatVnd, getVehicleHighlights, getVehicleImage, getVehicleName, getVehicleSeriesName, statusLabel, unwrapList } from "@/lib/format";
import { getValidDashboardTab } from "@/lib/dashboard-nav";

const DASHBOARD_LOAD_TIMEOUT_MS = 15000;
const TYPE_FILTERS = [
  ["ALL", "Tất cả"],
  ["TEST_DRIVE", "Lái thử"],
  ["SERVICE", "Dịch vụ"],
];

function withTimeout(promise, message) {
  let timeoutId;
  const timeout = new Promise((_, reject) => {
    timeoutId = setTimeout(() => reject(new Error(message)), DASHBOARD_LOAD_TIMEOUT_MS);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timeoutId));
}

function formatDateTime(value) {
  if (!value) return "Chưa cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function reviewTypeLabel(type) {
  if (type === "SERVICE") return "Dịch vụ sửa chữa";
  if (type === "TEST_DRIVE") return "Lái thử";
  return "Đánh giá";
}

function appointmentTypeLabel(type) {
  return type === "SERVICE" ? "Dịch vụ" : "Lái thử";
}

function serviceTimeLabel(ticket) {
  return ticket?.completedAt || ticket?.processingAt || ticket?.receivingAt || ticket?.createdAt || "";
}

function serviceItemTypeLabel(type) {
  if (type === "PART") return "Phụ tùng";
  if (type === "LABOR") return "Công thợ";
  return statusLabel(type);
}

function statusToneClass(status) {
  const normalized = String(status || "").toUpperCase();
  if (["COMPLETED", "SUBMITTED", "RESOLVED", "ACTIVE", "SOLD"].includes(normalized)) return "status-success";
  if (["CONFIRMED", "CHECKED_IN", "RECEIVING", "IN_PROGRESS", "CHATTING"].includes(normalized)) return "status-info";
  if (["PENDING", "WAITING", "NEEDS_REASSIGNMENT", "CONNECTING", "MAINTENANCE"].includes(normalized)) return "status-warning";
  if (["CANCELED", "CANCELLED", "REJECTED", "EXPIRED", "ERROR", "FAILED"].includes(normalized)) return "status-danger";
  return "status-neutral";
}

function statusPillClass(status) {
  return `status-pill ${statusToneClass(status)}`;
}

function RatingDisplay({ value }) {
  const rating = Math.max(0, Math.min(5, Math.round(Number(value || 0))));
  if (!rating) return <span className="rating-display muted">Chưa đánh giá</span>;
  return (
    <span className="rating-display" aria-label={`${rating} trên 5`}>
      <span className="rating-stars rating-stars-filled" aria-hidden="true">{"★".repeat(rating)}</span>
      {rating < 5 ? <span className="rating-stars-empty" aria-hidden="true">{"★".repeat(5 - rating)}</span> : null}
      <span className="rating-score">{rating}/5</span>
    </span>
  );
}

function reviewReferenceId(review) {
  if (review.reviewType === "SERVICE") return review.serviceId;
  if (review.reviewType === "TEST_DRIVE") return review.appointmentId;
  return "";
}

function reviewReferenceLabel(type) {
  if (type === "SERVICE") return "Mã dịch vụ liên quan";
  if (type === "TEST_DRIVE") return "Mã lịch lái thử liên quan";
  return "Mã liên quan";
}

function canSubmitReview(review) {
  return review?.status === "PENDING" && review?.reviewToken;
}

function reviewSubmitHref(review) {
  return `/reviews/${encodeURIComponent(review.reviewToken)}`;
}

function ReviewCodeDisplay({ value }) {
  const code = String(value || "");
  const parts = code.split("-");
  if (parts.length === 5) {
    return (
      <>
        {parts.slice(0, 4).join("-")}-
        <br />
        {parts[4]}
      </>
    );
  }
  return code || "Không có";
}

async function enrichVehicles(vehicles) {
  return Promise.all(
    vehicles.map(async (vehicle) => {
      if (!vehicle.carVersionId) return vehicle;
      try {
        const versionDetail = await getCarVersion(vehicle.carVersionId);
        return { ...vehicle, versionDetail };
      } catch {
        return vehicle;
      }
    })
  );
}

async function enrichAppointments(appointments) {
  return Promise.all(
    appointments.map(async (appointment) => {
      if (!appointment.id) return appointment;
      try {
        const detail = await getMyAppointmentDetail(appointment.id);
        return { ...appointment, ...detail };
      } catch {
        return appointment;
      }
    })
  );
}

async function attachAppointmentVehicleDetails(appointments, vehicles) {
  const knownVersionIds = new Set(vehicles.map((vehicle) => String(vehicle.carVersionId)).filter(Boolean));
  const versionIds = [...new Set(
    appointments
      .map((appointment) => appointment.carVersionId)
      .filter((id) => id && !knownVersionIds.has(String(id)))
      .map(String)
  )];

  if (!versionIds.length) return appointments;

  const versionEntries = await Promise.all(
    versionIds.map(async (id) => {
      try {
        return [id, await getCarVersion(id)];
      } catch {
        return [id, null];
      }
    })
  );
  const versionById = new Map(versionEntries.filter(([, detail]) => detail));
  return appointments.map((appointment) => ({
    ...appointment,
    versionDetail: versionById.get(String(appointment.carVersionId)) || appointment.versionDetail,
  }));
}

export default function CustomerDashboard() {
  const searchParams = useSearchParams();
  const tab = getValidDashboardTab("USER", searchParams.get("tab"));
  const loadSequence = useRef(0);
  const [data, setData] = useState({ appointments: [], reviews: [], vehicles: [], dealerships: [], serviceTickets: [] });
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [selectedReview, setSelectedReview] = useState(null);
  const [selectedService, setSelectedService] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [appointmentTypeFilter, setAppointmentTypeFilter] = useState("ALL");
  const [reviewTypeFilter, setReviewTypeFilter] = useState("ALL");

  const load = useCallback(async () => {
    const sequence = loadSequence.current + 1;
    loadSequence.current = sequence;
    setLoading(true);
    setError("");
    try {
      const dashboardData = await withTimeout((async () => {
        const [appointments, reviews, vehicles, serviceTickets] = await Promise.all([
          getMyAppointments(),
          getMyReviews(),
          getMyVehicles(),
          getUserServiceTickets().catch(() => []),
        ]);
        const [enrichedVehicles, dealerships, enrichedAppointments] = await Promise.all([
          enrichVehicles(unwrapList(vehicles)),
          getDealerships().then(unwrapList).catch(() => []),
          enrichAppointments(unwrapList(appointments)),
        ]);
        const appointmentsWithVehicles = await attachAppointmentVehicleDetails(enrichedAppointments, enrichedVehicles);
        return {
          appointments: appointmentsWithVehicles,
          reviews: unwrapList(reviews),
          vehicles: enrichedVehicles,
          dealerships,
          serviceTickets: unwrapList(serviceTickets),
        };
      })(), "Không thể tải dữ liệu dashboard. Vui lòng thử lại.");
      if (sequence !== loadSequence.current) return;
      setData({
        appointments: dashboardData.appointments,
        reviews: dashboardData.reviews,
        vehicles: dashboardData.vehicles,
        dealerships: dashboardData.dealerships,
        serviceTickets: dashboardData.serviceTickets,
      });
    } catch (caughtError) {
      if (sequence !== loadSequence.current) return;
      setError(caughtError.message);
    } finally {
      if (sequence === loadSequence.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load, tab]);

  useEffect(() => {
    const refreshableTabs = new Set(["vehicles", "appointments", "services", "reviews"]);
    function refreshOnReturn() {
      if (document.visibilityState === "visible" && refreshableTabs.has(tab)) load();
    }

    window.addEventListener("focus", refreshOnReturn);
    window.addEventListener("pageshow", refreshOnReturn);
    document.addEventListener("visibilitychange", refreshOnReturn);
    return () => {
      window.removeEventListener("focus", refreshOnReturn);
      window.removeEventListener("pageshow", refreshOnReturn);
      document.removeEventListener("visibilitychange", refreshOnReturn);
    };
  }, [load, tab]);

  async function openAppointment(id) {
    setDetailLoading(true);
    setError("");
    try {
      const currentItem = data.appointments.find((item) => String(item.id) === String(id)) || {};
      const detail = await getMyAppointmentDetail(id);
      setSelectedAppointment({ ...currentItem, ...detail, versionDetail: currentItem.versionDetail || detail.versionDetail });
    } catch (caughtError) {
      setError(caughtError.message || "Không thể tải chi tiết lịch hẹn.");
    } finally {
      setDetailLoading(false);
    }
  }

  async function openServiceTicket(ticket) {
    setDetailLoading(true);
    setError("");
    try {
      const [detail, invoice] = await Promise.all([
        getUserServiceTicketDetail(ticket.id),
        ticket.status === "COMPLETED" ? getServiceInvoice(ticket.id).catch(() => null) : Promise.resolve(null),
      ]);
      setSelectedService({ ...ticket, ...detail, invoice });
    } catch (caughtError) {
      setError(caughtError.message || "Không thể tải chi tiết dịch vụ.");
    } finally {
      setDetailLoading(false);
    }
  }

  const dealershipNameById = new Map(data.dealerships.map((dealer) => [String(dealer.id), dealer.name]));
  const vehicleByVinId = new Map(data.vehicles.map((vehicle) => [String(vehicle.vinId), vehicle]));
  const vehicleByVersionId = new Map(data.vehicles.map((vehicle) => [String(vehicle.carVersionId), vehicle]));
  const getDealershipName = (item) => (
    dealershipNameById.get(String(item?.dealershipId)) || item?.dealershipName || item?.dealershipId || "Đang cập nhật"
  );
  const getAppointmentVehicleName = (item) => {
    const vehicle = item?.vinId
      ? vehicleByVinId.get(String(item.vinId))
      : vehicleByVersionId.get(String(item?.carVersionId));
    const detail = vehicle?.versionDetail;
    if (detail || vehicle?.carVersionName) {
      return getVehicleName({ ...detail, carVersionName: vehicle.carVersionName });
    }
    if (item?.versionDetail) return getVehicleName(item.versionDetail);
    return item?.carVersionName || item?.versionName || item?.modelName || "Đang cập nhật";
  };
  const getServiceVehicleName = (ticket) => {
    if (ticket?.invoice?.carVersionName) return ticket.invoice.carVersionName;
    const vehicle = ticket?.vinId ? vehicleByVinId.get(String(ticket.vinId)) : null;
    if (vehicle?.versionDetail || vehicle?.carVersionName) {
      return getVehicleName({ ...vehicle.versionDetail, carVersionName: vehicle.carVersionName });
    }
    return ticket?.carVersionName || "Đang cập nhật";
  };
  const getServiceDealershipName = (ticket) => ticket?.invoice?.dealershipName || getDealershipName(ticket);
  const filteredAppointments = appointmentTypeFilter === "ALL"
    ? data.appointments
    : data.appointments.filter((item) => item.type === appointmentTypeFilter);
  const filteredReviews = reviewTypeFilter === "ALL"
    ? data.reviews
    : data.reviews.filter((item) => item.reviewType === reviewTypeFilter);

  return (
    <div className="ops-grid workspace-tabs-layout">
      {tab === "profile" ? <ProfilePanel eyebrow="Customer" heading="Hồ sơ cá nhân" /> : null}

      {tab === "vehicles" ? <section className="ops-panel wide customer-vehicles-panel" id="user-vehicles">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Vehicles</p>
            <h2>Xe cá nhân</h2>
          </div>
        </div>
        {error ? <div className="status-box">{error}</div> : null}
        {loading ? <div className="status-box">Đang tải xe cá nhân...</div> : null}
        <div className="customer-vehicle-grid">
          {data.vehicles.map((vehicle) => {
            const vehicleDetail = vehicle.versionDetail || {};
            const displayVehicle = { ...vehicleDetail, carVersionName: vehicle.carVersionName };
            const imageUrl = getVehicleImage(displayVehicle);
            const highlights = getVehicleHighlights(displayVehicle).slice(0, 3);
            const dealershipName = getDealershipName(vehicle);

            return (
              <article className="customer-vehicle-card" key={vehicle.vinId}>
                <div className="customer-vehicle-media" aria-hidden="true">
                  {imageUrl ? <span style={{ backgroundImage: `url(${imageUrl})` }} /> : <span className="vehicle-silhouette" />}
                </div>
                <div className="customer-vehicle-body">
                  <div className="customer-vehicle-head">
                    <div>
                      <p className="eyebrow">{getVehicleSeriesName(displayVehicle)}</p>
                      <h3>{getVehicleName(displayVehicle)}</h3>
                      {vehicleDetail.modelYear ? <small>Model {vehicleDetail.modelYear}</small> : null}
                    </div>
                  </div>
                  {highlights.length ? (
                    <dl className="customer-vehicle-highlights">
                      {highlights.map((item) => (
                        <div key={item.label}>
                          <dt>{item.label}</dt>
                          <dd>{item.value}</dd>
                        </div>
                      ))}
                    </dl>
                  ) : null}
                  <dl className="customer-vehicle-meta">
                    <div><dt>VIN</dt><dd>{vehicle.vinId}</dd></div>
                    <div><dt>Đại lý</dt><dd>{dealershipName}</dd></div>
                    <div><dt>Ngày gán</dt><dd>{formatDateTime(vehicle.assignedAt)}</dd></div>
                    <div><dt>Chủ xe</dt><dd>{vehicle.customerFullName || "Tài khoản của tôi"}</dd></div>
                  </dl>
                  <div className="customer-vehicle-actions">
                    <Link className="btn btn-primary" href={`/appointments/service?vinId=${encodeURIComponent(vehicle.vinId)}`}>Đặt lịch dịch vụ</Link>
                    {vehicle.carVersionId ? <Link className="btn btn-ghost" href={`/vehicles/${vehicle.carVersionId}`}>Xem mẫu xe</Link> : null}
                  </div>
                </div>
              </article>
            );
          })}
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
        <div className="segmented-tabs customer-panel-filters" role="group" aria-label="Lọc lịch hẹn">
          {TYPE_FILTERS.map(([value, label]) => (
            <button className={appointmentTypeFilter === value ? "active" : ""} key={value} type="button" onClick={() => setAppointmentTypeFilter(value)} aria-pressed={appointmentTypeFilter === value}>
              {label}
            </button>
          ))}
        </div>
        <div className="customer-row-list">
          {filteredAppointments.map((item) => (
            <button className="customer-data-row" key={item.id} type="button" onClick={() => openAppointment(item.id)}>
              <div className="customer-row-main">
                <span>{appointmentTypeLabel(item.type)}</span>
                <strong>{item.appointmentDate || "Chưa có ngày"}</strong>
                <small>Mã lịch: {item.id}</small>
              </div>
              <dl className="customer-row-meta">
                <div><dt>Thời gian</dt><dd>{item.startTime || "--:--"} - {item.endTime || "--:--"}</dd></div>
                <div><dt>Xe</dt><dd>{getAppointmentVehicleName(item)}</dd></div>
                <div><dt>Đại lý</dt><dd>{getDealershipName(item)}</dd></div>
              </dl>
              <div className="customer-row-action">
                <small className={statusPillClass(item.status)}>{statusLabel(item.status)}</small>
                <span className="customer-detail-text">Xem chi tiết</span>
              </div>
            </button>
          ))}
          {!filteredAppointments.length && !loading ? <div className="status-box">Chưa có lịch hẹn phù hợp.</div> : null}
        </div>
        {detailLoading ? <div className="status-box">Đang tải chi tiết lịch hẹn...</div> : null}
      </section> : null}

      {tab === "services" ? <section className="ops-panel wide customer-services-panel" id="user-services">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Services</p>
            <h2>Dịch vụ của tôi</h2>
          </div>
        </div>
        {error ? <div className="status-box">{error}</div> : null}
        <div className="customer-row-list">
          {data.serviceTickets.map((ticket) => (
            <button className="customer-data-row customer-service-row" key={ticket.id} type="button" onClick={() => openServiceTicket(ticket)}>
              <div className="customer-row-main">
                <span>Phiếu dịch vụ</span>
                <strong>{getServiceVehicleName(ticket)}</strong>
                <small>Mã phiếu: {ticket.id}</small>
              </div>
              <dl className="customer-row-meta">
                <div><dt>VIN</dt><dd>{ticket.vinId || "Không có"}</dd></div>
                <div><dt>Đại lý</dt><dd>{getServiceDealershipName(ticket)}</dd></div>
                <div><dt>Cập nhật</dt><dd>{formatDateTime(serviceTimeLabel(ticket))}</dd></div>
              </dl>
              <div className="customer-row-action">
                <small className={statusPillClass(ticket.status)}>{statusLabel(ticket.status)}</small>
                <span className="customer-detail-text">Xem chi tiết</span>
              </div>
            </button>
          ))}
          {!data.serviceTickets.length && !loading ? <div className="status-box">Chưa có dịch vụ sửa chữa nào.</div> : null}
        </div>
        {detailLoading ? <div className="status-box">Đang tải chi tiết dịch vụ...</div> : null}
      </section> : null}

      {tab === "reviews" ? <section className="ops-panel wide" id="user-reviews">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Reviews</p>
            <h2>Đánh giá của tôi</h2>
          </div>
        </div>
        {error ? <div className="status-box">{error}</div> : null}
        <div className="segmented-tabs customer-panel-filters" role="group" aria-label="Lọc đánh giá">
          {TYPE_FILTERS.map(([value, label]) => (
            <button className={reviewTypeFilter === value ? "active" : ""} key={value} type="button" onClick={() => setReviewTypeFilter(value)} aria-pressed={reviewTypeFilter === value}>
              {label}
            </button>
          ))}
        </div>
        <div className="customer-row-list">
          {filteredReviews.map((item) => (
            <article className="customer-data-row customer-review-row" key={item.id}>
              <div className="customer-row-main">
                <span>Đánh giá</span>
                <strong>{reviewTypeLabel(item.reviewType)}</strong>
                <small className="review-code">Mã đánh giá: <ReviewCodeDisplay value={item.id} /></small>
              </div>
              <dl className="customer-row-meta">
                <div><dt>Ngày gửi</dt><dd>{item.submittedAt ? formatDateTime(item.submittedAt) : "Chưa gửi"}</dd></div>
                <div><dt>Số sao</dt><dd><RatingDisplay value={item.serviceRating} /></dd></div>
              </dl>
              <div className="customer-row-action">
                <small className={statusPillClass(item.status)}>{statusLabel(item.status)}</small>
                <span className="customer-row-buttons">
                  {canSubmitReview(item) ? <Link className="btn btn-primary review-submit-link" href={reviewSubmitHref(item)}>Gửi đánh giá</Link> : null}
                  <button className="btn btn-ghost review-detail-button" type="button" onClick={() => setSelectedReview(item)}>Xem chi tiết</button>
                </span>
              </div>
            </article>
          ))}
          {!filteredReviews.length && !loading ? <div className="status-box">Chưa có đánh giá phù hợp.</div> : null}
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
              <span className={statusPillClass(selectedAppointment.status)}>{statusLabel(selectedAppointment.status)}</span>
              <dl className="summary-list compact">
                <div><dt>Ngày hẹn</dt><dd>{selectedAppointment.appointmentDate}</dd></div>
                <div><dt>Thời gian</dt><dd>{selectedAppointment.startTime} - {selectedAppointment.endTime}</dd></div>
                <div><dt>Đại lý</dt><dd>{getDealershipName(selectedAppointment)}</dd></div>
                <div><dt>Xe</dt><dd>{getAppointmentVehicleName(selectedAppointment)}</dd></div>
                {selectedAppointment.type !== "TEST_DRIVE" ? <div><dt>VIN</dt><dd>{selectedAppointment.vinId || "Không có"}</dd></div> : null}
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
          <section className="detail-modal review-detail-modal" role="dialog" aria-modal="true" aria-labelledby="review-detail-title" onClick={(event) => event.stopPropagation()}>
            <header className="manager-modal-head detail-modal-head">
              <div>
                <p className="eyebrow">{reviewTypeLabel(selectedReview.reviewType)}</p>
                <h2 id="review-detail-title">Chi tiết đánh giá</h2>
              </div>
              <button className="icon-button" type="button" onClick={() => setSelectedReview(null)} aria-label="Đóng chi tiết đánh giá">×</button>
            </header>
            <div className="detail-modal-body">
              <span className={statusPillClass(selectedReview.status)}>{statusLabel(selectedReview.status)}</span>
              <dl className="summary-list compact">
                <div className="review-id-row"><dt>Mã đánh giá</dt><dd className="review-code">{selectedReview.id || "Không có"}</dd></div>
                <div><dt>Loại</dt><dd>{reviewTypeLabel(selectedReview.reviewType)}</dd></div>
                <div><dt>Đánh giá dịch vụ</dt><dd><RatingDisplay value={selectedReview.serviceRating} /></dd></div>
                <div><dt>Nhận xét dịch vụ</dt><dd>{selectedReview.serviceComment || "Không có"}</dd></div>
                {selectedReview.reviewType !== "TEST_DRIVE" ? (
                  <>
                    <div><dt>Đánh giá kỹ thuật viên</dt><dd><RatingDisplay value={selectedReview.mechanicRating} /></dd></div>
                    <div><dt>Nhận xét kỹ thuật viên</dt><dd>{selectedReview.mechanicComment || "Không có"}</dd></div>
                    <div><dt>VIN</dt><dd>{selectedReview.vinId || "Không có"}</dd></div>
                  </>
                ) : null}
                <div className="review-reference-row"><dt>{reviewReferenceLabel(selectedReview.reviewType)}</dt><dd className="review-code">{reviewReferenceId(selectedReview) || "Không có"}</dd></div>
                <div><dt>Ngày gửi</dt><dd>{formatDateTime(selectedReview.submittedAt)}</dd></div>
              </dl>
            </div>
          </section>
        </div>
      ) : null}

      {selectedService ? (() => {
        const ticket = selectedService.serviceTicket || selectedService;
        const invoice = selectedService.invoice || {};
        const items = invoice.items || selectedService.items || [];
        const partItems = items.filter((item) => item.itemType === "PART");
        const laborItems = items.filter((item) => item.itemType === "LABOR");
        const totalAmount = invoice.totalAmount ?? ticket.totalAmount ?? 0;

        return (
          <div className="manager-modal-backdrop detail-modal-backdrop" role="presentation" onClick={() => setSelectedService(null)}>
            <section className="detail-modal service-detail-modal" role="dialog" aria-modal="true" aria-labelledby="service-detail-title" onClick={(event) => event.stopPropagation()}>
              <header className="manager-modal-head detail-modal-head">
                <div>
                  <p className="eyebrow">Chi tiết dịch vụ</p>
                  <h2 id="service-detail-title">{getServiceVehicleName({ ...ticket, invoice })}</h2>
                </div>
                <button className="icon-button" type="button" onClick={() => setSelectedService(null)} aria-label="Đóng chi tiết dịch vụ">×</button>
              </header>
              <div className="detail-modal-body">
                <span className={statusPillClass(ticket.status)}>{statusLabel(ticket.status)}</span>
                <div className="service-total-card">
                  <span>Tổng tiền dịch vụ</span>
                  <strong>{formatVnd(totalAmount)}</strong>
                </div>
                <dl className="summary-list compact">
                  <div><dt>Mã phiếu</dt><dd>{ticket.id}</dd></div>
                  <div><dt>VIN</dt><dd>{ticket.vinId || "Không có"}</dd></div>
                  <div><dt>Đại lý</dt><dd>{getServiceDealershipName({ ...ticket, invoice })}</dd></div>
                  <div><dt>Kỹ thuật viên</dt><dd>{invoice.mechanicName || ticket.mechanicId || "Đang cập nhật"}</dd></div>
                  <div><dt>Số km</dt><dd>{ticket.mileageAtService ? `${ticket.mileageAtService} km` : "Chưa cập nhật"}</dd></div>
                  <div><dt>Tiếp nhận</dt><dd>{formatDateTime(ticket.receivingAt)}</dd></div>
                  <div><dt>Bắt đầu sửa</dt><dd>{formatDateTime(ticket.processingAt)}</dd></div>
                  <div><dt>Hoàn tất</dt><dd>{formatDateTime(ticket.completedAt)}</dd></div>
                  <div><dt>Tình trạng xe</dt><dd>{ticket.vehicleCondition || invoice.vehicleCondition || "Chưa cập nhật"}</dd></div>
                  <div><dt>Ghi chú</dt><dd>{ticket.notes || "Không có"}</dd></div>
                </dl>
                <div className="service-breakdown">
                  <section className="service-breakdown-group" aria-labelledby="service-parts-title">
                    <header className="service-breakdown-head">
                      <div>
                        <p className="eyebrow">Phụ tùng</p>
                        <h3 id="service-parts-title">Phụ tùng đã thay</h3>
                      </div>
                      <span>{partItems.length} hạng mục</span>
                    </header>
                    <div className="service-item-list">
                      {partItems.map((item) => (
                        <article className="service-item-card service-part-card" key={item.id || item.itemName}>
                          <div className="service-item-copy">
                            <span>{serviceItemTypeLabel(item.itemType)}</span>
                            <strong>{item.itemName || "Phụ tùng"}</strong>
                            {item.note ? <small>{item.note}</small> : null}
                          </div>
                          <dl>
                            <div className="quantity-field"><dt>Số lượng</dt><dd>{item.quantity || 1}</dd></div>
                            <div className="money-field unit-price-field"><dt>Đơn giá</dt><dd>{formatVnd(item.unitPrice || 0)}</dd></div>
                            <div className="category-field"><dt>Hạng mục</dt><dd>{statusLabel(item.billingType)}</dd></div>
                            <div className="money-field money-field-total final-price-field"><dt>Thành tiền</dt><dd>{formatVnd(item.finalPrice || 0)}</dd></div>
                          </dl>
                        </article>
                      ))}
                      {!partItems.length ? <div className="status-box">Chưa có phụ tùng được cập nhật.</div> : null}
                    </div>
                  </section>

                  <section className="service-breakdown-group" aria-labelledby="service-labor-title">
                    <header className="service-breakdown-head">
                      <div>
                        <p className="eyebrow">Công thợ</p>
                        <h3 id="service-labor-title">Công thực hiện</h3>
                      </div>
                      <span>{laborItems.length} hạng mục</span>
                    </header>
                    <div className="service-item-list">
                      {laborItems.map((item) => (
                        <article className="service-item-card service-labor-card" key={item.id || item.itemName}>
                          <div className="service-item-copy">
                            <span>{serviceItemTypeLabel(item.itemType)}</span>
                            <strong>{item.itemName || "Công thợ"}</strong>
                            {item.note ? <small>{item.note}</small> : null}
                          </div>
                          <dl>
                            <div className="category-field"><dt>Loại phí</dt><dd>{statusLabel(item.billingType)}</dd></div>
                            <div className="money-field money-field-total final-price-field"><dt>Thành tiền</dt><dd>{formatVnd(item.finalPrice || 0)}</dd></div>
                          </dl>
                        </article>
                      ))}
                      {!laborItems.length ? <div className="status-box">Chưa có công thợ được cập nhật.</div> : null}
                    </div>
                  </section>
                </div>
              </div>
            </section>
          </div>
        );
      })() : null}
    </div>
  );
}
