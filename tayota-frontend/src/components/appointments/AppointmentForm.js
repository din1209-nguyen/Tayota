"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { createAppointment, getAvailableSlots } from "@/lib/services/appointments";
import { getAllCarVersions, getCarStylesWithVersions, getDealerships } from "@/lib/services/car";
import { getAccessToken } from "@/lib/session";
import { formatVnd, getGoogleMapsUrl, getVehicleId, getVehicleImage, getVehicleName, getVehiclePrice, unwrapList } from "@/lib/format";
import { EMPTY_VEHICLE_FILTERS, filterVehicleItems } from "@/lib/vehicle-filters";
import VehicleFilterControls from "@/components/vehicles/VehicleFilterControls";

const STEPS = ["Dịch vụ", "Đại lý", "Thời gian", "Liên hệ", "Xác nhận"];

function toTimeLabel(slot) {
  if (!slot) return "";
  return slot.endTime ? `${slot.startTime} - ${slot.endTime}` : slot.startTime;
}

export default function AppointmentForm({ type, defaultCarVersionId = "" }) {
  const isService = type === "service";
  const [step, setStep] = useState(0);
  const [vehicles, setVehicles] = useState([]);
  const [vehicleStyles, setVehicleStyles] = useState([]);
  const [vehicleFilters, setVehicleFilters] = useState(EMPTY_VEHICLE_FILTERS);
  const [dealerships, setDealerships] = useState([]);
  const [slots, setSlots] = useState([]);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [success, setSuccess] = useState(null);
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

  const selectedVehicle = useMemo(
    () => vehicles.find((vehicle) => String(getVehicleId(vehicle)) === String(form.carVersionId)),
    [vehicles, form.carVersionId]
  );
  const selectedDealer = useMemo(
    () => dealerships.find((dealer) => String(dealer.id) === String(form.dealershipId)),
    [dealerships, form.dealershipId]
  );
  const selectedSlot = useMemo(
    () => slots.find((slot) => slot.startTime === form.startTime),
    [slots, form.startTime]
  );
  const filteredVehicles = useMemo(() => {
    const matches = filterVehicleItems(vehicles, vehicleFilters);
    if (!selectedVehicle || matches.some((vehicle) => String(getVehicleId(vehicle)) === String(form.carVersionId))) {
      return matches;
    }
    return [selectedVehicle, ...matches];
  }, [form.carVersionId, selectedVehicle, vehicleFilters, vehicles]);

  useEffect(() => {
    let alive = true;

    async function loadInitialData() {
      setLoadingInitial(true);
      try {
        const [dealerResult, vehicleResult, styleResult] = await Promise.all([
          getDealerships(),
          isService ? Promise.resolve([]) : getAllCarVersions(),
          isService ? Promise.resolve([]) : getCarStylesWithVersions(),
        ]);
        if (!alive) return;
        setDealerships(unwrapList(dealerResult));
        setVehicles(vehicleResult);
        setVehicleStyles(unwrapList(styleResult));
      } catch (error) {
        if (!alive) return;
        setMessage(error.message || "Không thể tải dữ liệu đặt lịch.");
      } finally {
        if (alive) setLoadingInitial(false);
      }
    }

    loadInitialData();
    return () => {
      alive = false;
    };
  }, [isService]);

  useEffect(() => {
    let alive = true;

    async function loadSlots() {
      if (!form.dealershipId || !form.appointmentDate) {
        setSlots([]);
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
        if (!alive) return;
        const availableSlots = Array.isArray(result?.slots)
          ? result.slots.filter((slot) => slot?.available !== false)
          : unwrapList(result).filter((slot) => slot?.available !== false);
        setSlots(availableSlots);
        if (!availableSlots.length) {
          setMessage(result?.holiday ? result?.holidayReason || "Đại lý nghỉ trong ngày này." : "Không còn khung giờ trống.");
        }
      } catch (error) {
        if (!alive) return;
        setSlots([]);
        setMessage(error.message || "Không thể tải khung giờ trống.");
      } finally {
        if (alive) setLoadingSlots(false);
      }
    }

    setForm((current) => ({ ...current, startTime: "" }));
    loadSlots();
    return () => {
      alive = false;
    };
  }, [form.appointmentDate, form.dealershipId, isService]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function choose(name, value) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  function validateStep(targetStep = step) {
    if (targetStep === 0 && !isService && !form.carVersionId) return "Vui lòng chọn mẫu xe muốn lái thử.";
    if (targetStep === 0 && isService && form.vinId.trim().length !== 17) return "Số VIN cần đúng 17 ký tự.";
    if (targetStep === 1 && !form.dealershipId) return "Vui lòng chọn đại lý.";
    if (targetStep === 2 && (!form.appointmentDate || !form.startTime)) return "Vui lòng chọn ngày và khung giờ.";
    if (targetStep === 3 && (!form.guestFullName || !form.guestPhone || !form.guestEmail)) {
      return "Vui lòng nhập đủ họ tên, số điện thoại và email.";
    }
    if (targetStep === 3 && isService && !form.notes.trim()) return "Vui lòng mô tả tình trạng xe.";
    return "";
  }

  function next() {
    const validationMessage = validateStep();
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }
    setMessage("");
    setStep((current) => Math.min(current + 1, STEPS.length - 1));
  }

  function back() {
    setMessage("");
    setStep((current) => Math.max(current - 1, 0));
  }

  async function submit(event) {
    event.preventDefault();
    if (submitting) return;

    const validationMessage = [0, 1, 2, 3].map(validateStep).find(Boolean);
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }

    setSubmitting(true);
    setMessage("");
    try {
      const authenticated = Boolean(getAccessToken());
      const contact = {
        guestFullName: form.guestFullName.trim(),
        guestEmail: form.guestEmail.trim(),
        guestPhone: form.guestPhone.trim(),
        dealershipId: form.dealershipId,
        appointmentDate: form.appointmentDate,
        startTime: form.startTime,
        notes: form.notes.trim(),
      };
      const payload = isService
        ? { ...contact, vinId: form.vinId.trim().toUpperCase() }
        : { ...contact, carVersionId: form.carVersionId };

      const result = await createAppointment({ type, authenticated, payload });
      setSuccess({
        id: result?.id || result?.appointmentId || "đang chờ xác nhận",
        type: isService ? "Dịch vụ" : "Lái thử",
        dealer: selectedDealer?.name || "Đại lý đã chọn",
        date: form.appointmentDate,
        time: toTimeLabel(selectedSlot),
        vehicle: isService ? form.vinId.trim().toUpperCase() : getVehicleName(selectedVehicle),
      });
      setStep(STEPS.length - 1);
    } catch (error) {
      setMessage(error.message || "Không thể tạo lịch hẹn.");
    } finally {
      setSubmitting(false);
    }
  }

  if (success) {
    return (
      <section className="appointment-success form-panel">
        <p className="eyebrow">Đặt lịch thành công</p>
        <h2>Lịch hẹn đang chờ xác nhận</h2>
        <p>Đội ngũ Tayota sẽ liên hệ lại để xác nhận trước thời gian tiếp đón.</p>
        <dl className="summary-list">
          <div><dt>Mã lịch</dt><dd>{success.id}</dd></div>
          <div><dt>Loại lịch</dt><dd>{success.type}</dd></div>
          <div><dt>{isService ? "VIN" : "Xe"}</dt><dd>{success.vehicle}</dd></div>
          <div><dt>Đại lý</dt><dd>{success.dealer}</dd></div>
          <div><dt>Thời gian</dt><dd>{success.date} {success.time}</dd></div>
          <div><dt>Trạng thái</dt><dd>Chờ xác nhận</dd></div>
        </dl>
        <div className="wizard-actions">
          <Link className="btn btn-primary" href="/dashboard/user">
            Xem lịch của tôi
          </Link>
          <Link className="btn btn-ghost" href={isService ? "/vehicles" : `/vehicles/${form.carVersionId}`}>
            {isService ? "Xem danh sách xe" : "Quay lại xe"}
          </Link>
        </div>
      </section>
    );
  }

  return (
    <form className="form-panel appointment-wizard appointment-wizard-large" onSubmit={submit}>
      <div className="wizard-steps" aria-label="Các bước đặt lịch">
        {STEPS.map((label, index) => (
          <button
            className={`wizard-step ${index === step ? "active" : ""} ${index < step ? "complete" : ""}`}
            key={label}
            type="button"
            onClick={() => index < step && setStep(index)}
          >
            <span>{index + 1}</span>
            {label}
          </button>
        ))}
      </div>

      {loadingInitial ? <div className="status-box">Đang tải dữ liệu đặt lịch...</div> : null}

      {step === 0 ? (
        <section className="wizard-panel">
          <span className="eyebrow">{isService ? "Bảo dưỡng và sửa chữa" : "Trải nghiệm xe"}</span>
          <h2>{isService ? "Nhập thông tin xe" : "Chọn mẫu xe muốn lái thử"}</h2>
          {isService ? (
            <label className="label">
              Số VIN
              <input
                className="field"
                name="vinId"
                value={form.vinId}
                onChange={updateField}
                maxLength={17}
                placeholder="17 ký tự trên giấy đăng ký hoặc thân xe"
              />
            </label>
          ) : (
            <>
              <VehicleFilterControls
                filters={vehicleFilters}
                onChange={setVehicleFilters}
                onReset={() => setVehicleFilters(EMPTY_VEHICLE_FILTERS)}
                styles={vehicleStyles}
                variant="chooser"
              />
              <div className="appointment-options vehicle-options">
                {filteredVehicles.map((vehicle) => {
                  const id = getVehicleId(vehicle);
                  const selected = String(form.carVersionId) === String(id);
                  const imageUrl = getVehicleImage(vehicle);
                  return (
                    <button className={`choice-card vehicle-choice ${selected ? "selected" : ""}`} key={id} type="button" onClick={() => choose("carVersionId", id)}>
                      <span className="vehicle-choice-image" style={imageUrl ? { backgroundImage: `url(${imageUrl})` } : undefined} />
                      <strong>{getVehicleName(vehicle)}</strong>
                      <span>{formatVnd(getVehiclePrice(vehicle))}</span>
                    </button>
                  );
                })}
                {!filteredVehicles.length ? <div className="status-box wide">Không tìm thấy xe phù hợp.</div> : null}
              </div>
            </>
          )}
        </section>
      ) : null}

      {step === 1 ? (
        <section className="wizard-panel">
          <span className="eyebrow">Đại lý</span>
          <h2>Chọn nơi tiếp đón</h2>
          <div className="appointment-options dealership-options">
            {dealerships.map((dealer) => (
              <button
                className={`choice-card dealer-choice ${String(form.dealershipId) === String(dealer.id) ? "selected" : ""}`}
                key={dealer.id}
                type="button"
                onClick={() => choose("dealershipId", dealer.id)}
              >
                <strong>{dealer.name}</strong>
                <span>{dealer.address}</span>
                {dealer.phone ? <small>{dealer.phone}</small> : null}
                {dealer.latitude && dealer.longitude ? <small>{dealer.latitude}, {dealer.longitude}</small> : null}
              </button>
            ))}
          </div>
          {selectedDealer ? (
            <a className="btn btn-ghost dealer-map-link" href={getGoogleMapsUrl(selectedDealer)} target="_blank" rel="noreferrer">
              Xem vị trí đại lý
            </a>
          ) : null}
        </section>
      ) : null}

      {step === 2 ? (
        <section className="wizard-panel">
          <span className="eyebrow">Lịch trống</span>
          <h2>Chọn ngày và giờ</h2>
          <label className="label">
            Ngày hẹn
            <input className="field" type="date" name="appointmentDate" value={form.appointmentDate} onChange={updateField} />
          </label>
          <div className="slot-grid">
            {loadingSlots ? <div className="status-box">Đang tải khung giờ...</div> : null}
            {!loadingSlots && slots.map((slot) => (
              <button
                className={`slot-button ${form.startTime === slot.startTime ? "selected" : ""}`}
                key={slot.id || slot.startTime}
                type="button"
                onClick={() => choose("startTime", slot.startTime)}
              >
                {toTimeLabel(slot)}
              </button>
            ))}
          </div>
        </section>
      ) : null}

      {step === 3 ? (
        <section className="wizard-panel">
          <span className="eyebrow">Liên hệ</span>
          <h2>Thông tin xác nhận</h2>
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
          </div>
          <label className="label">
            {isService ? "Mô tả tình trạng xe" : "Ghi chú"}
            <textarea className="field" name="notes" value={form.notes} onChange={updateField} rows={4} />
          </label>
        </section>
      ) : null}

      {step === 4 ? (
        <section className="wizard-panel">
          <span className="eyebrow">Xác nhận</span>
          <h2>Kiểm tra thông tin lịch hẹn</h2>
          <dl className="summary-list">
            <div><dt>Loại lịch</dt><dd>{isService ? "Dịch vụ" : "Lái thử"}</dd></div>
            <div><dt>{isService ? "VIN" : "Xe"}</dt><dd>{isService ? form.vinId : getVehicleName(selectedVehicle)}</dd></div>
            <div><dt>Đại lý</dt><dd>{selectedDealer?.name || "Chưa chọn"}</dd></div>
            <div><dt>Thời gian</dt><dd>{form.appointmentDate} {toTimeLabel(selectedSlot)}</dd></div>
            <div><dt>Khách hàng</dt><dd>{form.guestFullName} - {form.guestPhone}</dd></div>
          </dl>
        </section>
      ) : null}

      {message ? <div className="status-box">{message}</div> : null}

      <div className="wizard-actions">
        <button className="btn btn-secondary" type="button" onClick={back} disabled={step === 0 || submitting}>
          Quay lại
        </button>
        {step < STEPS.length - 1 ? (
          <button className="btn btn-primary" type="button" onClick={next}>
            Tiếp tục
          </button>
        ) : (
          <button className="btn btn-primary" type="submit" disabled={submitting}>
            {submitting ? "Đang gửi..." : "Xác nhận lịch hẹn"}
          </button>
        )}
      </div>
    </form>
  );
}
