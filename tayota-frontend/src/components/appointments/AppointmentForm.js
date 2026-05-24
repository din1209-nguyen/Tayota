"use client";

import { useMemo, useState } from "react";
import { apiFetch, buildQuery } from "@/lib/api";
import { getAccessToken } from "@/lib/session";

export default function AppointmentForm({ type, defaultCarVersionId = "" }) {
  const isService = type === "service";
  const [form, setForm] = useState({
    fullName: "",
    phone: "",
    email: "",
    carVersionId: defaultCarVersionId,
    vin: "",
    dealershipId: "1",
    appointmentDate: "",
    appointmentTime: "",
    note: "",
  });
  const [slots, setSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");

  const endpoint = useMemo(() => {
    const token = getAccessToken();
    if (isService) return token ? "/operation/appointments/service" : "/operation/appointments/service/guest";
    return token ? "/operation/appointments/test-drive" : "/operation/appointments/test-drive/guest";
  }, [isService]);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function loadSlots() {
    if (!form.dealershipId || !form.appointmentDate) {
      setMessage("Vui lòng chọn đại lý và ngày hẹn trước.");
      return;
    }
    setLoadingSlots(true);
    setMessage("");
    try {
      const query = buildQuery({
        dealershipId: form.dealershipId,
        appointmentType: isService ? "SERVICE" : "TEST_DRIVE",
        appointmentDate: form.appointmentDate,
      });
      const result = await apiFetch(`/operation/appointments/available-slots${query}`);
      setSlots(Array.isArray(result) ? result : result?.slots || []);
    } catch (error) {
      setSlots([]);
      setMessage(error.message);
    } finally {
      setLoadingSlots(false);
    }
  }

  async function submit(event) {
    event.preventDefault();
    if (submitting) return;
    if (!form.fullName || !form.phone || !form.email || !form.appointmentDate || !form.appointmentTime) {
      setMessage("Vui lòng điền đầy đủ thông tin liên hệ và khung giờ.");
      return;
    }
    if (isService && !form.vin) {
      setMessage("Đặt lịch dịch vụ cần số VIN.");
      return;
    }
    setSubmitting(true);
    setMessage("");
    try {
      const token = getAccessToken();
      const result = await apiFetch(endpoint, {
        method: "POST",
        token,
        body: JSON.stringify({
          ...form,
          appointmentType: isService ? "SERVICE" : "TEST_DRIVE",
        }),
      });
      setMessage(`Đặt lịch thành công. Mã lịch hẹn: ${result?.appointmentId || result?.id || "đang chờ xác nhận"}.`);
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
          <input className="field" name="fullName" value={form.fullName} onChange={updateField} />
        </label>
        <label className="label">
          Số điện thoại
          <input className="field" name="phone" value={form.phone} onChange={updateField} />
        </label>
        <label className="label">
          Email
          <input className="field" type="email" name="email" value={form.email} onChange={updateField} />
        </label>
        <label className="label">
          {isService ? "VIN" : "Mã phiên bản xe"}
          <input
            className="field"
            name={isService ? "vin" : "carVersionId"}
            value={isService ? form.vin : form.carVersionId}
            onChange={updateField}
          />
        </label>
        <label className="label">
          Đại lý
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
        <select className="field" name="appointmentTime" value={form.appointmentTime} onChange={updateField}>
          <option value="">Chọn khung giờ</option>
          {slots.map((slot, index) => (
            <option key={`${slot}-${index}`} value={slot?.time || slot?.startTime || slot}>
              {slot?.time || slot?.startTime || slot}
            </option>
          ))}
        </select>
      </div>
      <label className="label">
        Ghi chú
        <textarea className="field" name="note" value={form.note} onChange={updateField} rows={4} />
      </label>
      {message ? <div className="status-box">{message}</div> : null}
      <button className="btn btn-primary" type="submit" disabled={submitting}>
        {submitting ? "Đang gửi..." : "Xác nhận lịch hẹn"}
      </button>
    </form>
  );
}
