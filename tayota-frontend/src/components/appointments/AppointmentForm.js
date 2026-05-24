"use client";

import { useState } from "react";
import { createAppointment, getAvailableSlots } from "@/lib/services/appointments";
import { getAccessToken } from "@/lib/session";

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function isUuid(value) {
  return UUID_PATTERN.test(value || "");
}

export default function AppointmentForm({ type, defaultCarVersionId = "" }) {
  const isService = type === "service";
  const [form, setForm] = useState({
    guestFullName: "",
    guestPhone: "",
    guestEmail: "",
    carVersionId: defaultCarVersionId,
    vinId: "",
    dealershipId: "",
    appointmentDate: "",
    startTime: "",
    notes: "",
  });
  const [slots, setSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function loadSlots() {
    if (!isUuid(form.dealershipId) || !form.appointmentDate) {
      setMessage("Vui lòng nhập UUID đại lý và chọn ngày hẹn trước.");
      return;
    }

    setLoadingSlots(true);
    setMessage("");
    try {
      const result = await getAvailableSlots({
        dealershipId: form.dealershipId,
        appointmentType: isService ? "SERVICE" : "TEST_DRIVE",
        appointmentDate: form.appointmentDate,
      });
      const availableSlots = Array.isArray(result?.slots)
        ? result.slots.filter((slot) => slot?.available !== false)
        : [];
      setSlots(availableSlots);
      if (!availableSlots.length) {
        setMessage(result?.holiday ? result?.holidayReason || "Đại lý nghỉ trong ngày này." : "Không còn khung giờ trống.");
      }
    } catch (error) {
      setSlots([]);
      setMessage(error.message);
    } finally {
      setLoadingSlots(false);
    }
  }

  function validateForm() {
    if (!form.guestFullName || !form.guestPhone || !form.guestEmail || !form.appointmentDate || !form.startTime) {
      return "Vui lòng điền đầy đủ thông tin liên hệ và khung giờ.";
    }
    if (!isUuid(form.dealershipId)) {
      return "Đại lý phải là UUID hợp lệ.";
    }
    if (isService && form.vinId.length !== 17) {
      return "Đặt lịch dịch vụ cần số VIN gồm 17 ký tự.";
    }
    if (isService && !form.notes.trim()) {
      return "Đặt lịch dịch vụ cần mô tả tình trạng xe.";
    }
    if (!isService && !form.carVersionId) {
      return "Đặt lịch lái thử cần mã phiên bản xe.";
    }
    return "";
  }

  async function submit(event) {
    event.preventDefault();
    if (submitting) return;

    const validationMessage = validateForm();
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }

    setSubmitting(true);
    setMessage("");
    try {
      const authenticated = Boolean(getAccessToken());
      const payload = isService
        ? {
            guestFullName: form.guestFullName,
            guestEmail: form.guestEmail,
            guestPhone: form.guestPhone,
            vinId: form.vinId,
            dealershipId: form.dealershipId,
            appointmentDate: form.appointmentDate,
            startTime: form.startTime,
            notes: form.notes,
          }
        : {
            guestFullName: form.guestFullName,
            guestEmail: form.guestEmail,
            guestPhone: form.guestPhone,
            carVersionId: form.carVersionId,
            dealershipId: form.dealershipId,
            appointmentDate: form.appointmentDate,
            startTime: form.startTime,
            notes: form.notes,
          };

      const result = await createAppointment({ type, authenticated, payload });
      setMessage(`Đặt lịch thành công. Mã lịch hẹn: ${result?.id || "đang chờ xác nhận"}.`);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form-panel" onSubmit={submit}>
      <div className="form-grid">
        <label className="label">
          Họ và tên
          <input className="field" name="guestFullName" value={form.guestFullName} onChange={updateField} />
        </label>
        <label className="label">
          Số điện thoại
          <input className="field" name="guestPhone" value={form.guestPhone} onChange={updateField} />
        </label>
        <label className="label">
          Email
          <input className="field" type="email" name="guestEmail" value={form.guestEmail} onChange={updateField} />
        </label>
        <label className="label">
          {isService ? "VIN" : "Mã phiên bản xe"}
          <input
            className="field"
            name={isService ? "vinId" : "carVersionId"}
            value={isService ? form.vinId : form.carVersionId}
            onChange={updateField}
          />
        </label>
        <label className="label">
          UUID đại lý
          <input className="field" name="dealershipId" value={form.dealershipId} onChange={updateField} />
        </label>
        <label className="label">
          Ngày hẹn
          <input className="field" type="date" name="appointmentDate" value={form.appointmentDate} onChange={updateField} />
        </label>
      </div>
      <div className="slot-row">
        <button className="btn btn-ghost" type="button" onClick={loadSlots} disabled={loadingSlots}>
          {loadingSlots ? "Đang tải giờ..." : "Kiểm tra giờ trống"}
        </button>
        <select className="field" name="startTime" value={form.startTime} onChange={updateField}>
          <option value="">Chọn khung giờ</option>
          {slots.map((slot) => (
            <option key={slot.id || slot.startTime} value={slot.startTime}>
              {slot.startTime}
              {slot.endTime ? ` - ${slot.endTime}` : ""}
            </option>
          ))}
        </select>
      </div>
      <label className="label">
        {isService ? "Mô tả tình trạng xe" : "Ghi chú"}
        <textarea className="field" name="notes" value={form.notes} onChange={updateField} rows={4} />
      </label>
      {message ? <div className="status-box">{message}</div> : null}
      <button className="btn btn-primary" type="submit" disabled={submitting}>
        {submitting ? "Đang gửi..." : "Xác nhận lịch hẹn"}
      </button>
    </form>
  );
}
