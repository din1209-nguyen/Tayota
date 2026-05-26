"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  checkInServiceAppointment,
  checkInTestDriveAppointment,
  createAdvisorHoliday,
  createAdvisorTimeSlot,
  deleteAdvisorHoliday,
  deleteAdvisorTimeSlot,
  getAdvisorAppointmentDetail,
  getAdvisorAppointments,
  getAdvisorHolidays,
  getAdvisorTimeSlots,
  updateAdvisorAppointment,
} from "@/lib/services/appointments";
import {
  assignCustomerVehicle,
  getActiveAdvisorMechanics,
  getCustomerVehicles,
  removeCustomerVehicle,
  searchAdvisorCustomers,
} from "@/lib/services/advisor";
import {
  assignTicketMechanic,
  createWalkInServiceTicket,
  getAdvisorServiceTicketDetail,
  getAdvisorServiceTickets,
} from "@/lib/services/workorders";
import { formatVnd, statusLabel, unwrapList } from "@/lib/format";

const TABS = [
  ["overview", "Tổng quan"],
  ["appointments", "Lịch hẹn"],
  ["checkin", "Check-in"],
  ["walkin", "Walk-in service"],
  ["vehicles", "Gán xe"],
  ["slots", "Khung giờ"],
  ["holidays", "Ngày nghỉ"],
  ["tickets", "Phiếu dịch vụ"],
  ["reports", "Báo cáo"],
];

const APPOINTMENT_STATUSES = ["PENDING", "CONFIRMED", "CHECKED_IN", "COMPLETED", "CANCELED", "REJECTED", "EXPIRED", "ALL"];
const TICKET_STATUSES = ["ALL", "CONFIRMED", "NEEDS_REASSIGNMENT", "RECEIVING", "IN_PROGRESS", "COMPLETED", "CANCELED", "EXPIRED"];

function toDateTime(item) {
  return `${item?.appointmentDate || ""} ${item?.startTime || ""}`.trim();
}

function emptyWalkIn() {
  return {
    userId: "",
    guestFullName: "",
    guestEmail: "",
    guestPhone: "",
    vinId: "",
    mechanicId: "",
    mileageAtService: "",
    vehicleCondition: "",
    notes: "",
  };
}

export default function AdvisorDashboard() {
  const [tab, setTab] = useState("overview");
  const [status, setStatus] = useState("PENDING");
  const [ticketStatus, setTicketStatus] = useState("ALL");
  const [appointments, setAppointments] = useState([]);
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [slots, setSlots] = useState([]);
  const [holidays, setHolidays] = useState([]);
  const [mechanics, setMechanics] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [customerKeyword, setCustomerKeyword] = useState("");
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [customerVehicles, setCustomerVehicles] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [appointmentForm, setAppointmentForm] = useState({ appointmentDate: "", startTime: "", cancelReason: "", notes: "" });
  const [checkInForm, setCheckInForm] = useState({ mechanicId: "", mileageAtService: "", vehicleCondition: "", notes: "" });
  const [walkInForm, setWalkInForm] = useState(emptyWalkIn());
  const [vehicleForm, setVehicleForm] = useState({ vinId: "", note: "" });
  const [reassignMechanicId, setReassignMechanicId] = useState("");
  const [slotForm, setSlotForm] = useState({ appointmentType: "SERVICE", startTime: "", endTime: "" });
  const [holidayForm, setHolidayForm] = useState({ holidayDate: "", reason: "" });

  const stats = useMemo(() => ({
    pending: appointments.filter((item) => item.status === "PENDING").length,
    confirmed: appointments.filter((item) => item.status === "CONFIRMED").length,
    activeTickets: tickets.filter((item) => ["CONFIRMED", "RECEIVING", "IN_PROGRESS"].includes(item.status)).length,
    completedTickets: tickets.filter((item) => item.status === "COMPLETED").length,
  }), [appointments, tickets]);

  const loadBase = useCallback(async function loadBase() {
    setLoading(true);
    setMessage("");
    try {
      const [nextAppointments, nextSlots, nextHolidays, nextMechanics, nextTickets] = await Promise.all([
        getAdvisorAppointments(status),
        getAdvisorTimeSlots(),
        getAdvisorHolidays(),
        getActiveAdvisorMechanics(),
        getAdvisorServiceTickets({ status: ticketStatus }),
      ]);
      setAppointments(unwrapList(nextAppointments));
      setSlots(unwrapList(nextSlots));
      setHolidays(unwrapList(nextHolidays));
      setMechanics(unwrapList(nextMechanics));
      setTickets(unwrapList(nextTickets));
    } catch (error) {
      setMessage(error.message || "Không thể tải dashboard cố vấn.");
    } finally {
      setLoading(false);
    }
  }, [status, ticketStatus]);

  useEffect(() => {
    loadBase();
  }, [loadBase]);

  async function run(action, successMessage) {
    setActionLoading(true);
    setMessage("");
    try {
      const result = await action();
      setMessage(successMessage);
      await loadBase();
      return result;
    } catch (error) {
      setMessage(error.message || "Thao tác thất bại.");
      return null;
    } finally {
      setActionLoading(false);
    }
  }

  async function openAppointment(id) {
    const detail = await run(() => getAdvisorAppointmentDetail(id), "");
    if (detail) {
      setSelectedAppointment(detail);
      setAppointmentForm({
        appointmentDate: detail.appointmentDate || "",
        startTime: detail.startTime || "",
        cancelReason: detail.cancelReason || "",
        notes: detail.notes || "",
      });
    }
  }

  async function searchCustomers() {
    if (!customerKeyword.trim()) return;
    const result = await run(() => searchAdvisorCustomers({ keyword: customerKeyword, size: 8 }), "");
    setCustomers(unwrapList(result));
  }

  async function chooseCustomer(customer) {
    setSelectedCustomer(customer);
    setWalkInForm((current) => ({ ...current, userId: customer.id }));
    const result = await run(() => getCustomerVehicles(customer.id), "");
    setCustomerVehicles(unwrapList(result));
  }

  async function openTicket(id) {
    const detail = await run(() => getAdvisorServiceTicketDetail(id), "");
    if (detail) {
      setSelectedTicket(detail);
      setReassignMechanicId("");
    }
  }

  async function reassignTicket() {
    if (!selectedTicket?.serviceTicket?.id || !reassignMechanicId) {
      setMessage("Vui lòng chọn kỹ thuật viên để phân công lại.");
      return;
    }
    const result = await run(
      () => assignTicketMechanic(selectedTicket.serviceTicket.id, { mechanicId: reassignMechanicId }),
      "Đã phân công lại kỹ thuật viên."
    );
    if (result) {
      await openTicket(selectedTicket.serviceTicket.id);
    }
  }

  async function submitCheckIn() {
    if (!selectedAppointment) return;
    if (selectedAppointment.type === "TEST_DRIVE") {
      await run(() => checkInTestDriveAppointment(selectedAppointment.id), "Check-in lái thử thành công.");
      setSelectedAppointment(null);
      return;
    }

    const payload = {
      mechanicId: checkInForm.mechanicId,
      mileageAtService: checkInForm.mileageAtService ? Number(checkInForm.mileageAtService) : null,
      vehicleCondition: checkInForm.vehicleCondition,
      notes: checkInForm.notes,
    };
    await run(() => checkInServiceAppointment(selectedAppointment.id, payload), "Check-in dịch vụ và tạo phiếu thành công.");
    setSelectedAppointment(null);
  }

  async function submitWalkIn(event) {
    event.preventDefault();
    const payload = {
      ...walkInForm,
      mileageAtService: walkInForm.mileageAtService ? Number(walkInForm.mileageAtService) : null,
      userId: walkInForm.userId || null,
    };
    const result = await run(() => createWalkInServiceTicket(payload), "Tạo phiếu dịch vụ trực tiếp thành công.");
    if (result) setWalkInForm(emptyWalkIn());
  }

  async function assignVehicle(event) {
    event.preventDefault();
    if (!selectedCustomer) {
      setMessage("Vui lòng chọn khách hàng trước khi gán xe.");
      return;
    }
    const result = await run(() => assignCustomerVehicle({ ...vehicleForm, userId: selectedCustomer.id }), "Gán xe cho khách hàng thành công.");
    if (result) {
      setVehicleForm({ vinId: "", note: "" });
      setCustomerVehicles(unwrapList(await getCustomerVehicles(selectedCustomer.id)));
    }
  }

  return (
    <div className="ops-grid workspace-tabs-layout advisor-workspace">
      <nav className="role-tabs wide" aria-label="Các mục cố vấn dịch vụ">
        {TABS.map(([id, label]) => (
          <button className={tab === id ? "active" : ""} key={id} type="button" onClick={() => setTab(id)}>{label}</button>
        ))}
      </nav>

      {message ? <div className="status-box wide">{message}</div> : null}
      {loading ? <div className="status-box wide">Đang tải dữ liệu vận hành...</div> : null}

      {tab === "overview" ? (
        <section className="ops-panel wide">
          <div className="ops-panel-head">
            <div><p className="eyebrow">Advisor</p><h2>Tổng quan hôm nay</h2></div>
            <button className="btn btn-ghost" type="button" onClick={loadBase}>Tải lại</button>
          </div>
          <div className="metric-grid">
            <article><strong>{stats.pending}</strong><span>Lịch chờ xác nhận</span></article>
            <article><strong>{stats.confirmed}</strong><span>Lịch đã xác nhận</span></article>
            <article><strong>{stats.activeTickets}</strong><span>Phiếu đang xử lý</span></article>
            <article><strong>{stats.completedTickets}</strong><span>Phiếu hoàn tất</span></article>
          </div>
        </section>
      ) : null}

      {tab === "appointments" ? (
        <section className="ops-panel wide">
          <div className="ops-panel-head">
            <div><p className="eyebrow">Appointments</p><h2>Quản lý lịch hẹn</h2></div>
            <select className="field compact-field" value={status} onChange={(event) => setStatus(event.target.value)}>
              {APPOINTMENT_STATUSES.map((item) => <option key={item} value={item}>{item === "ALL" ? "Tất cả" : statusLabel(item)}</option>)}
            </select>
          </div>
          <div className="ops-list">
            {appointments.map((item) => (
              <article className={selectedAppointment?.id === item.id ? "active" : ""} key={item.id}>
                <strong>{item.type === "SERVICE" ? "Dịch vụ" : "Lái thử"}</strong>
                <span>{toDateTime(item)}</span>
                <small>{statusLabel(item.status)}</small>
                <div className="row-actions"><button className="btn btn-ghost" type="button" onClick={() => openAppointment(item.id)}>Chi tiết</button></div>
              </article>
            ))}
            {!appointments.length && !loading ? <div className="status-box">Không có lịch hẹn trong bộ lọc này.</div> : null}
          </div>
        </section>
      ) : null}

      {tab === "appointments" && selectedAppointment ? (
        <section className="ops-panel">
          <p className="eyebrow">Chi tiết</p>
          <h2>{selectedAppointment.type === "SERVICE" ? "Lịch dịch vụ" : "Lịch lái thử"}</h2>
          <dl className="summary-list compact">
            <div><dt>Khách hàng</dt><dd>{selectedAppointment.customerFullName || "Đang cập nhật"}</dd></div>
            <div><dt>Liên hệ</dt><dd>{selectedAppointment.customerPhone || selectedAppointment.customerEmail || "Đang cập nhật"}</dd></div>
            <div><dt>VIN/Xe</dt><dd>{selectedAppointment.vinId || selectedAppointment.carVersionId || "Đang cập nhật"}</dd></div>
            <div><dt>Trạng thái</dt><dd>{statusLabel(selectedAppointment.status)}</dd></div>
          </dl>
          <div className="form-grid">
            <label className="label">Ngày hẹn<input className="field" type="date" value={appointmentForm.appointmentDate} onChange={(event) => setAppointmentForm({ ...appointmentForm, appointmentDate: event.target.value })} /></label>
            <label className="label">Giờ bắt đầu<input className="field" type="time" value={appointmentForm.startTime} onChange={(event) => setAppointmentForm({ ...appointmentForm, startTime: event.target.value })} /></label>
          </div>
          <label className="label">Lý do hủy/từ chối<textarea className="field" rows={3} value={appointmentForm.cancelReason} onChange={(event) => setAppointmentForm({ ...appointmentForm, cancelReason: event.target.value })} /></label>
          <div className="row-actions wrap">
            <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => updateAdvisorAppointment(selectedAppointment.id, { status: "CONFIRMED" }), "Đã xác nhận lịch hẹn.")}>Xác nhận</button>
            {selectedAppointment.status === "CHECKED_IN" && selectedAppointment.type === "TEST_DRIVE" ? (
              <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => updateAdvisorAppointment(selectedAppointment.id, { status: "COMPLETED" }), "Đã hoàn thành lịch lái thử.")}>Hoàn thành lái thử</button>
            ) : null}
            <button className="btn btn-ghost" type="button" disabled={actionLoading} onClick={() => run(() => updateAdvisorAppointment(selectedAppointment.id, { appointmentDate: appointmentForm.appointmentDate, startTime: appointmentForm.startTime }), "Đã đổi lịch hẹn.")}>Đổi lịch</button>
            <button className="btn btn-ghost" type="button" disabled={actionLoading} onClick={() => run(() => updateAdvisorAppointment(selectedAppointment.id, { status: "REJECTED", cancelReason: appointmentForm.cancelReason }), "Đã từ chối lịch hẹn.")}>Từ chối</button>
            <button className="btn btn-ghost" type="button" disabled={actionLoading} onClick={() => run(() => updateAdvisorAppointment(selectedAppointment.id, { status: "CANCELED", cancelReason: appointmentForm.cancelReason }), "Đã hủy lịch hẹn.")}>Hủy</button>
          </div>
        </section>
      ) : null}

      {tab === "checkin" ? (
        <section className="ops-panel wide">
          <div className="ops-panel-head">
            <div><p className="eyebrow">Check-in</p><h2>Khách đã tới đại lý</h2></div>
            <button className="btn btn-ghost" type="button" onClick={() => setStatus("CONFIRMED")}>Xem lịch đã xác nhận</button>
          </div>
          <div className="ops-list">
            {appointments.filter((item) => item.status === "CONFIRMED").map((item) => (
              <article key={item.id}>
                <strong>{item.type === "SERVICE" ? "Dịch vụ" : "Lái thử"}</strong>
                <span>{toDateTime(item)}</span>
                <div className="row-actions"><button className="btn btn-ghost" type="button" onClick={() => openAppointment(item.id)}>Chọn</button></div>
              </article>
            ))}
          </div>
          {selectedAppointment ? (
            <div className="inline-form">
              <h3>Check-in {selectedAppointment.vinId || selectedAppointment.customerFullName || selectedAppointment.id}</h3>
              {selectedAppointment.type === "SERVICE" ? (
                <div className="form-grid">
                  <label className="label">Kỹ thuật viên<select className="field" value={checkInForm.mechanicId} onChange={(event) => setCheckInForm({ ...checkInForm, mechanicId: event.target.value })}><option value="">Chọn thợ</option>{mechanics.map((item) => <option key={item.id} value={item.id}>{item.fullName || item.id}</option>)}</select></label>
                  <label className="label">Số km<input className="field" type="number" min="0" value={checkInForm.mileageAtService} onChange={(event) => setCheckInForm({ ...checkInForm, mileageAtService: event.target.value })} /></label>
                  <label className="label wide">Tình trạng xe<textarea className="field" rows={3} value={checkInForm.vehicleCondition} onChange={(event) => setCheckInForm({ ...checkInForm, vehicleCondition: event.target.value })} /></label>
                  <label className="label wide">Ghi chú<textarea className="field" rows={3} value={checkInForm.notes} onChange={(event) => setCheckInForm({ ...checkInForm, notes: event.target.value })} /></label>
                </div>
              ) : null}
              <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={submitCheckIn}>Xác nhận check-in</button>
            </div>
          ) : null}
        </section>
      ) : null}

      {tab === "walkin" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Walk-in service</p>
          <h2>Tạo phiếu không cần lịch hẹn</h2>
          <div className="search-row">
            <input className="field" value={customerKeyword} onChange={(event) => setCustomerKeyword(event.target.value)} placeholder="Tìm khách theo tên, email hoặc số điện thoại" />
            <button className="btn btn-ghost" type="button" onClick={searchCustomers}>Tìm</button>
          </div>
          <div className="chip-row">{customers.map((item) => <button className={selectedCustomer?.id === item.id ? "selected" : ""} key={item.id} type="button" onClick={() => chooseCustomer(item)}>{item.fullName} · {item.phone || item.email}</button>)}</div>
          <form className="ops-form" onSubmit={submitWalkIn}>
            <div className="form-grid">
              <label className="label">Họ tên guest<input className="field" value={walkInForm.guestFullName} onChange={(event) => setWalkInForm({ ...walkInForm, guestFullName: event.target.value })} disabled={Boolean(walkInForm.userId)} /></label>
              <label className="label">Email guest<input className="field" type="email" value={walkInForm.guestEmail} onChange={(event) => setWalkInForm({ ...walkInForm, guestEmail: event.target.value })} disabled={Boolean(walkInForm.userId)} /></label>
              <label className="label">Số điện thoại guest<input className="field" value={walkInForm.guestPhone} onChange={(event) => setWalkInForm({ ...walkInForm, guestPhone: event.target.value })} disabled={Boolean(walkInForm.userId)} /></label>
              <label className="label">VIN<input className="field" maxLength={17} value={walkInForm.vinId} onChange={(event) => setWalkInForm({ ...walkInForm, vinId: event.target.value.toUpperCase() })} /></label>
              <label className="label">Kỹ thuật viên<select className="field" value={walkInForm.mechanicId} onChange={(event) => setWalkInForm({ ...walkInForm, mechanicId: event.target.value })}><option value="">Chọn thợ</option>{mechanics.map((item) => <option key={item.id} value={item.id}>{item.fullName || item.id}</option>)}</select></label>
              <label className="label">Số km<input className="field" type="number" min="0" value={walkInForm.mileageAtService} onChange={(event) => setWalkInForm({ ...walkInForm, mileageAtService: event.target.value })} /></label>
              <label className="label wide">Tình trạng xe<textarea className="field" rows={3} value={walkInForm.vehicleCondition} onChange={(event) => setWalkInForm({ ...walkInForm, vehicleCondition: event.target.value })} /></label>
              <label className="label wide">Ghi chú<textarea className="field" rows={3} value={walkInForm.notes} onChange={(event) => setWalkInForm({ ...walkInForm, notes: event.target.value })} /></label>
            </div>
            <button className="btn btn-primary" type="submit" disabled={actionLoading}>Tạo phiếu dịch vụ</button>
          </form>
        </section>
      ) : null}

      {tab === "vehicles" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Customer vehicles</p>
          <h2>Gán VIN vào tài khoản khách</h2>
          <div className="search-row">
            <input className="field" value={customerKeyword} onChange={(event) => setCustomerKeyword(event.target.value)} placeholder="Tìm khách hàng" />
            <button className="btn btn-ghost" type="button" onClick={searchCustomers}>Tìm</button>
          </div>
          <div className="chip-row">{customers.map((item) => <button className={selectedCustomer?.id === item.id ? "selected" : ""} key={item.id} type="button" onClick={() => chooseCustomer(item)}>{item.fullName} · {item.email}</button>)}</div>
          <form className="ops-form inline-form" onSubmit={assignVehicle}>
            <strong>{selectedCustomer ? `Khách đang chọn: ${selectedCustomer.fullName}` : "Chưa chọn khách hàng"}</strong>
            <div className="form-grid">
              <label className="label">VIN<input className="field" maxLength={17} value={vehicleForm.vinId} onChange={(event) => setVehicleForm({ ...vehicleForm, vinId: event.target.value.toUpperCase() })} /></label>
              <label className="label">Ghi chú<input className="field" value={vehicleForm.note} onChange={(event) => setVehicleForm({ ...vehicleForm, note: event.target.value })} /></label>
            </div>
            <button className="btn btn-primary" type="submit" disabled={actionLoading}>Gán xe</button>
          </form>
          <div className="ops-list">
            {customerVehicles.map((item) => (
              <article key={item.vinId}>
                <strong>{item.vinId}</strong>
                <span>{item.carVersionName || "Xe Tayota"} · {statusLabel(item.status)}</span>
                <div className="row-actions"><button className="btn btn-ghost" type="button" onClick={() => run(() => removeCustomerVehicle(item.vinId), "Đã gỡ VIN khỏi khách hàng.")}>Gỡ</button></div>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      {tab === "slots" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Time slots</p>
          <h2>Khung giờ làm việc</h2>
          <form className="ops-form inline-form" onSubmit={(event) => { event.preventDefault(); run(() => createAdvisorTimeSlot({ ...slotForm, active: true }), "Đã tạo khung giờ."); }}>
            <div className="form-grid">
              <label className="label">Loại lịch<select className="field" value={slotForm.appointmentType} onChange={(event) => setSlotForm({ ...slotForm, appointmentType: event.target.value })}><option value="SERVICE">Dịch vụ</option><option value="TEST_DRIVE">Lái thử</option></select></label>
              <label className="label">Bắt đầu<input className="field" type="time" value={slotForm.startTime} onChange={(event) => setSlotForm({ ...slotForm, startTime: event.target.value })} /></label>
              <label className="label">Kết thúc<input className="field" type="time" value={slotForm.endTime} onChange={(event) => setSlotForm({ ...slotForm, endTime: event.target.value })} /></label>
            </div>
            <button className="btn btn-primary" type="submit">Tạo khung giờ</button>
          </form>
          <div className="ops-list">{slots.map((slot) => <article key={slot.id}><strong>{slot.appointmentType}</strong><span>{slot.startTime} - {slot.endTime}</span><div className="row-actions"><button className="btn btn-ghost" type="button" onClick={() => run(() => deleteAdvisorTimeSlot(slot.id), "Đã tắt khung giờ.")}>Tắt</button></div></article>)}</div>
        </section>
      ) : null}

      {tab === "holidays" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Holidays</p>
          <h2>Ngày nghỉ đại lý</h2>
          <form className="ops-form inline-form" onSubmit={(event) => { event.preventDefault(); run(() => createAdvisorHoliday({ ...holidayForm, active: true }), "Đã tạo ngày nghỉ."); }}>
            <div className="form-grid">
              <label className="label">Ngày nghỉ<input className="field" type="date" value={holidayForm.holidayDate} onChange={(event) => setHolidayForm({ ...holidayForm, holidayDate: event.target.value })} /></label>
              <label className="label">Lý do<input className="field" value={holidayForm.reason} onChange={(event) => setHolidayForm({ ...holidayForm, reason: event.target.value })} /></label>
            </div>
            <button className="btn btn-primary" type="submit">Tạo ngày nghỉ</button>
          </form>
          <div className="ops-list">{holidays.map((holiday) => <article key={holiday.id}><strong>{holiday.holidayDate}</strong><span>{holiday.reason || "Ngày nghỉ"}</span><div className="row-actions"><button className="btn btn-ghost" type="button" onClick={() => run(() => deleteAdvisorHoliday(holiday.id), "Đã tắt ngày nghỉ.")}>Tắt</button></div></article>)}</div>
        </section>
      ) : null}

      {tab === "tickets" ? (
        <section className="ops-panel wide">
          <div className="ops-panel-head">
            <div><p className="eyebrow">Service tickets</p><h2>Phiếu dịch vụ đại lý</h2></div>
            <select className="field compact-field" value={ticketStatus} onChange={(event) => setTicketStatus(event.target.value)}>{TICKET_STATUSES.map((item) => <option key={item} value={item}>{item === "ALL" ? "Tất cả" : statusLabel(item)}</option>)}</select>
          </div>
          <div className="ops-list">{tickets.map((ticket) => <article key={ticket.id}><strong>{ticket.vinId}</strong><span>{ticket.customerFullName || "Khách vãng lai"} · {statusLabel(ticket.status)}</span><small>{formatVnd(ticket.totalAmount)}</small><div className="row-actions"><button className="btn btn-ghost" type="button" onClick={() => openTicket(ticket.id)}>Chi tiết</button></div></article>)}</div>
          {selectedTicket ? <div className="inline-form"><h3>{selectedTicket.serviceTicket?.vinId}</h3><dl className="summary-list compact"><div><dt>Khách hàng</dt><dd>{selectedTicket.serviceTicket?.customerFullName || "Đang cập nhật"}</dd></div><div><dt>Trạng thái</dt><dd>{statusLabel(selectedTicket.serviceTicket?.status)}</dd></div><div><dt>Tổng tiền</dt><dd>{formatVnd(selectedTicket.serviceTicket?.totalAmount)}</dd></div><div><dt>Ghi chú</dt><dd>{selectedTicket.serviceTicket?.notes || "Không có"}</dd></div></dl>{selectedTicket.serviceTicket?.status === "NEEDS_REASSIGNMENT" ? <div className="form-grid"><label className="label">Kỹ thuật viên mới<select className="field" value={reassignMechanicId} onChange={(event) => setReassignMechanicId(event.target.value)}><option value="">Chọn thợ</option>{mechanics.map((item) => <option key={item.id} value={item.id}>{item.fullName || item.id}</option>)}</select></label><div className="label"><span>&nbsp;</span><button className="btn btn-primary" type="button" disabled={actionLoading} onClick={reassignTicket}>Phân công lại</button></div></div> : null}<div className="ops-list">{selectedTicket.items?.map((item) => <article key={item.id}><strong>{item.itemName}</strong><span>{statusLabel(item.itemType)} · {item.quantity} x {formatVnd(item.unitPrice)}</span><small>{formatVnd(item.finalPrice)}</small></article>)}</div></div> : null}
        </section>
      ) : null}

      {tab === "reports" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Reports</p>
          <h2>Báo cáo vận hành</h2>
          <div className="metric-grid">
            <article><strong>{appointments.length}</strong><span>Lịch trong bộ lọc hiện tại</span></article>
            <article><strong>{tickets.length}</strong><span>Phiếu trong bộ lọc hiện tại</span></article>
            <article><strong>{formatVnd(tickets.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0))}</strong><span>Tổng tiền phiếu đang hiển thị</span></article>
          </div>
        </section>
      ) : null}
    </div>
  );
}
