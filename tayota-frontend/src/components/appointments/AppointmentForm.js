"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { createAppointment, getAvailabilityCalendar, getAvailableSlots, validateServiceVin } from "@/lib/services/appointments";
import { getAllCarVersions, getCarStylesWithVersions, getDealerships, getMyVehicles } from "@/lib/services/car";
import { getMe } from "@/lib/services/auth";
import { getAccessToken } from "@/lib/session";
import { formatVnd, getGoogleMapsUrl, getVehicleId, getVehicleImage, getVehicleName, getVehiclePrice, unwrapList } from "@/lib/format";
import { EMPTY_VEHICLE_FILTERS, filterVehicleItems } from "@/lib/vehicle-filters";
import VehicleFilterControls from "@/components/vehicles/VehicleFilterControls";

const STEPS = ["Dịch vụ", "Đại lý", "Thời gian", "Liên hệ", "Xác nhận"];
const WEEKDAY_LABELS = ["Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN"];

function toDateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getMonthBounds(monthDate) {
  const first = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1);
  const last = new Date(monthDate.getFullYear(), monthDate.getMonth() + 1, 0);
  return { first, last };
}

function getCalendarCells(monthDate, daysByDate) {
  const { first, last } = getMonthBounds(monthDate);
  const leadingBlanks = (first.getDay() + 6) % 7;
  const cells = Array.from({ length: leadingBlanks }, (_, index) => ({ key: `blank-${index}`, blank: true }));

  for (let day = 1; day <= last.getDate(); day += 1) {
    const date = new Date(monthDate.getFullYear(), monthDate.getMonth(), day);
    const dateKey = toDateInputValue(date);
    cells.push({ key: dateKey, date: dateKey, day, meta: daysByDate.get(dateKey) });
  }

  while (cells.length % 7 !== 0) {
    cells.push({ key: `tail-${cells.length}`, blank: true });
  }

  return cells;
}

function normalizeCalendarDate(value) {
  if (!value) return "";
  if (typeof value === "string") return value.slice(0, 10);
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }
  if (typeof value === "object" && value.year && value.month && value.day) {
    return `${value.year}-${String(value.month).padStart(2, "0")}-${String(value.day).padStart(2, "0")}`;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : toDateInputValue(date);
}

function normalizeCalendarDays(days) {
  return unwrapList(days)
    .map((day) => ({ ...day, date: normalizeCalendarDate(day?.date) }))
    .filter((day) => day.date);
}

function isTruthyAvailability(value) {
  return value === true || value === "true" || value === 1 || value === "1";
}

function isFalseAvailability(value) {
  return value === false || value === "false" || value === 0 || value === "0";
}

function dayHasAvailableSlots(day) {
  if (!day || day.holiday) return false;
  if (isFalseAvailability(day.hasAvailableSlots) || isFalseAvailability(day.available)) return false;
  if (isTruthyAvailability(day.hasAvailableSlots) || isTruthyAvailability(day.available)) return true;
  if (Array.isArray(day.slots)) return day.slots.some((slot) => slot?.available !== false);
  const count = Number(day.availableSlotCount ?? day.availableSlotsCount ?? day.slotCount);
  return Number.isFinite(count) && count > 0;
}

function getVisibleMonthDateKeys(monthDate) {
  const { first, last } = getMonthBounds(monthDate);
  const todayKey = toDateInputValue(new Date());
  const dateKeys = [];

  for (let day = 1; day <= last.getDate(); day += 1) {
    const dateKey = toDateInputValue(new Date(monthDate.getFullYear(), monthDate.getMonth(), day));
    if (dateKey >= todayKey) {
      dateKeys.push(dateKey);
    }
  }

  if (!dateKeys.length && toDateInputValue(first).slice(0, 7) === todayKey.slice(0, 7)) {
    dateKeys.push(todayKey);
  }

  return dateKeys;
}

async function buildCalendarDaysFromSlots({ dealershipId, appointmentType, monthDate }) {
  const dateKeys = getVisibleMonthDateKeys(monthDate);
  const results = await Promise.allSettled(
    dateKeys.map(async (dateKey) => {
      const result = await getAvailableSlots({ dealershipId, appointmentType, appointmentDate: dateKey });
      const slots = Array.isArray(result?.slots)
        ? result.slots.filter((slot) => slot?.available !== false)
        : unwrapList(result).filter((slot) => slot?.available !== false);

      return {
        date: dateKey,
        holiday: Boolean(result?.holiday),
        holidayReason: result?.holidayReason || null,
        hasAvailableSlots: slots.length > 0,
      };
    })
  );

  return results
    .filter((item) => item.status === "fulfilled")
    .map((item) => item.value);
}

function toTimeLabel(slot) {
  if (!slot) return "";
  return slot.endTime ? `${slot.startTime} - ${slot.endTime}` : slot.startTime;
}

export default function AppointmentForm({ type, defaultCarVersionId = "" }) {
  const isService = type === "service";
  const [step, setStep] = useState(0);
  const [authReady, setAuthReady] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [vehicles, setVehicles] = useState([]);
  const [vehicleStyles, setVehicleStyles] = useState([]);
  const [myVehicles, setMyVehicles] = useState([]);
  const [profile, setProfile] = useState(null);
  const [vehicleFilters, setVehicleFilters] = useState(EMPTY_VEHICLE_FILTERS);
  const [dealerships, setDealerships] = useState([]);
  const [slots, setSlots] = useState([]);
  const [calendarDays, setCalendarDays] = useState([]);
  const [calendarMonth, setCalendarMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [loadingCalendar, setLoadingCalendar] = useState(false);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [validatingVin, setValidatingVin] = useState(false);
  const [message, setMessage] = useState("");
  const [vinValidationMessage, setVinValidationMessage] = useState("");
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
  const daysByDate = useMemo(() => new Map(calendarDays.map((day) => [day.date, day])), [calendarDays]);
  const calendarCells = useMemo(() => getCalendarCells(calendarMonth, daysByDate), [calendarMonth, daysByDate]);

  useEffect(() => {
    setAuthenticated(Boolean(getAccessToken()));
    setAuthReady(true);
  }, []);

  useEffect(() => {
    if (!authReady) return;

    let alive = true;

    async function loadInitialData() {
      setLoadingInitial(true);
      try {
        const [dealerResult, vehicleResult, styleResult, profileResult, myVehicleResult] = await Promise.all([
          getDealerships(),
          isService ? Promise.resolve([]) : getAllCarVersions(),
          isService ? Promise.resolve([]) : getCarStylesWithVersions(),
          authenticated ? getMe() : Promise.resolve(null),
          isService && authenticated ? getMyVehicles() : Promise.resolve([]),
        ]);
        if (!alive) return;
        setDealerships(unwrapList(dealerResult));
        setVehicles(vehicleResult);
        setVehicleStyles(unwrapList(styleResult).map((style) => ({ ...style, series: style.series || style.carSeries || [] })));
        setProfile(profileResult);
        setMyVehicles(unwrapList(myVehicleResult));
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
  }, [authReady, authenticated, isService]);

  useEffect(() => {
    let alive = true;

    async function loadCalendar() {
      if (!form.dealershipId) {
        setCalendarDays([]);
        return;
      }

      setLoadingCalendar(true);
      setMessage("");

      try {
        const { first, last } = getMonthBounds(calendarMonth);
        const result = await getAvailabilityCalendar({
          dealershipId: form.dealershipId,
          appointmentType: isService ? "SERVICE" : "TEST_DRIVE",
          from: toDateInputValue(first),
          to: toDateInputValue(last),
        });
        if (!alive) return;
        let normalizedDays = normalizeCalendarDays(result);
        if (!normalizedDays.some(dayHasAvailableSlots)) {
          normalizedDays = await buildCalendarDaysFromSlots({
            dealershipId: form.dealershipId,
            appointmentType: isService ? "SERVICE" : "TEST_DRIVE",
            monthDate: calendarMonth,
          });
          if (!alive) return;
        }
        setCalendarDays(normalizedDays);
        if (!normalizedDays.some(dayHasAvailableSlots)) {
          setMessage("Chưa có ngày khả dụng. Vui lòng kiểm tra khung giờ làm việc hoặc ngày nghỉ đại lý.");
        }
      } catch (error) {
        if (!alive) return;
        setCalendarDays([]);
        setMessage(error.message || "Không thể tải lịch khả dụng.");
      } finally {
        if (alive) setLoadingCalendar(false);
      }
    }

    setForm((current) => ({ ...current, appointmentDate: "", startTime: "" }));
    loadCalendar();
    return () => {
      alive = false;
    };
  }, [calendarMonth, form.dealershipId, isService]);

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
          setMessage(result?.holiday
            ? result?.holidayReason || "Đại lý nghỉ trong ngày này."
            : "Chỉ có thể đặt lịch ở các khung giờ cách hiện tại tối thiểu 12 tiếng. Các khung giờ còn lại của ngày này không khả dụng hoặc nằm ngoài giờ làm việc của đại lý, vui lòng chọn ngày khác.");
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
    if (name === "vinId") {
      setVinValidationMessage("");
      setForm((current) => ({ ...current, [name]: value.toUpperCase() }));
      return;
    }
    setForm((current) => ({ ...current, [name]: value }));
  }

  function choose(name, value) {
    if (name === "vinId") {
      setVinValidationMessage("");
    }
    setForm((current) => ({ ...current, [name]: value }));
  }

  function toggleVehicle(value) {
    setForm((current) => ({
      ...current,
      carVersionId: String(current.carVersionId) === String(value) ? "" : value,
    }));
  }

  function moveCalendarMonth(direction) {
    setCalendarMonth((current) => new Date(current.getFullYear(), current.getMonth() + direction, 1));
  }

  function validateStep(targetStep = step) {
    if (targetStep === 0 && !isService && !form.carVersionId) return "Vui lòng chọn mẫu xe muốn lái thử.";
    if (targetStep === 0 && isService && form.vinId.trim().length !== 17) return "Số VIN cần đúng 17 ký tự.";
    if (targetStep === 1 && !form.dealershipId) return "Vui lòng chọn đại lý.";
    if (targetStep === 2 && (!form.appointmentDate || !form.startTime)) return "Vui lòng chọn ngày và khung giờ.";
    if (targetStep === 3 && !authenticated && (!form.guestFullName || !form.guestPhone || !form.guestEmail)) {
      return "Vui lòng nhập đủ họ tên, số điện thoại và email.";
    }
    if (targetStep === 3 && isService && !form.notes.trim()) return "Vui lòng mô tả tình trạng xe.";
    return "";
  }

  async function next() {
    const validationMessage = validateStep();
    if (validationMessage) {
      setMessage(validationMessage);
      return;
    }

    if (isService && step === 0) {
      const normalizedVin = form.vinId.trim().toUpperCase();
      setValidatingVin(true);
      try {
        await validateServiceVin(normalizedVin);
        setVinValidationMessage("VIN hợp lệ. Bạn có thể tiếp tục đặt lịch.");
      } catch (error) {
        setMessage(error.message || "Không thể kiểm tra VIN.");
        return;
      } finally {
        setValidatingVin(false);
      }
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

      {step === 0 ? (
        <section className="wizard-panel">
          <span className="eyebrow">{isService ? "Bảo dưỡng và sửa chữa" : "Trải nghiệm xe"}</span>
          <h2>{isService ? "Nhập thông tin xe" : "Chọn mẫu xe muốn lái thử"}</h2>
          {isService ? (
            <>
              {authenticated && myVehicles.length ? (
                <label className="label">
                  Xe của tôi
                  <select className="field" value={form.vinId} onChange={(event) => choose("vinId", event.target.value)}>
                    <option value="">Chọn VIN đã gán với tài khoản</option>
                    {myVehicles.map((vehicle) => (
                      <option key={vehicle.vinId} value={vehicle.vinId}>
                        {vehicle.vinId} · {vehicle.carVersionName || "Xe Tayota"}
                      </option>
                    ))}
                  </select>
                </label>
              ) : null}
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
              {vinValidationMessage ? (
                <div className="form-alert error">{vinValidationMessage}</div>
              ) : null}
              {!authReady ? (
                <div className="status-box">Đang kiểm tra trạng thái tài khoản...</div>
              ) : null}
              {authReady && authenticated && !myVehicles.length ? (
                <div className="status-box">Tài khoản của bạn chưa có VIN được gán. Bạn vẫn có thể nhập VIN, hệ thống sẽ kiểm tra quyền sở hữu khi gửi lịch.</div>
              ) : null}
              {authReady && !authenticated ? (
                <div className="status-box">Khách vãng lai có thể nhập VIN. Hệ thống sẽ kiểm tra VIN ngay ở bước này trước khi cho phép chọn đại lý.</div>
              ) : null}
            </>
          ) : (
            <>
              <div className="appointment-vehicle-filter">
                <div className="appointment-filter-head">
                  <div>
                    <strong>Lọc xe</strong>
                    <span>{filteredVehicles.length} mẫu phù hợp</span>
                  </div>
                  <button className="filter-clear" type="button" onClick={() => setVehicleFilters(EMPTY_VEHICLE_FILTERS)}>
                    Xóa lọc
                  </button>
                </div>
                <VehicleFilterControls
                  filters={vehicleFilters}
                  onChange={setVehicleFilters}
                  styles={vehicleStyles}
                  advanced={false}
                  showKeyword={false}
                  variant="chooser"
                />
              </div>
              <div className="appointment-options vehicle-options">
                {filteredVehicles.map((vehicle) => {
                  const id = getVehicleId(vehicle);
                  const selected = String(form.carVersionId) === String(id);
                  const imageUrl = getVehicleImage(vehicle);
                  return (
                    <button className={`choice-card vehicle-choice ${selected ? "selected" : ""}`} key={id} type="button" onClick={() => toggleVehicle(id)}>
                      <span className="vehicle-choice-image" style={imageUrl ? { backgroundImage: `url(${imageUrl})` } : undefined} />
                      <span className="vehicle-choice-copy">
                        <strong className="vehicle-choice-name">{getVehicleName(vehicle)}</strong>
                        <span className="vehicle-choice-price">{formatVnd(getVehiclePrice(vehicle))}</span>
                      </span>
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
          <div className="appointment-time-grid">
            <div className="booking-calendar-card">
              <div className="booking-calendar-head">
                <button className="calendar-nav" type="button" onClick={() => moveCalendarMonth(-1)} aria-label="Tháng trước">‹</button>
                <strong>{calendarMonth.toLocaleDateString("vi-VN", { month: "long", year: "numeric" })}</strong>
                <button className="calendar-nav" type="button" onClick={() => moveCalendarMonth(1)} aria-label="Tháng sau">›</button>
              </div>
              <div className="booking-calendar-weekdays">
                {WEEKDAY_LABELS.map((label) => <span key={label}>{label}</span>)}
              </div>
              <div className="booking-calendar" aria-label="Lịch ngày khả dụng">
                {loadingCalendar ? <div className="status-box wide">Đang tải lịch đại lý...</div> : null}
                {!loadingCalendar && calendarCells.map((cell) => {
                  if (cell.blank) return <span className="calendar-blank" key={cell.key} />;
                  const day = cell.meta;
                  const disabled = !dayHasAvailableSlots(day);
                  const selected = form.appointmentDate === cell.date;
                  return (
                    <button
                      className={`calendar-day ${selected ? "selected" : ""} ${disabled ? "disabled" : ""}`}
                      key={cell.key}
                      type="button"
                      disabled={disabled}
                      title={day?.holiday ? day.holidayReason || "Đại lý nghỉ" : disabled ? "Không có khung giờ khả dụng" : "Có thể đặt lịch"}
                      onClick={() => choose("appointmentDate", cell.date)}
                    >
                      <strong>{cell.day}</strong>
                    </button>
                  );
                })}
                {!loadingCalendar && !calendarDays.length ? <div className="form-alert error wide">Chưa có dữ liệu lịch khả dụng.</div> : null}
              </div>
            </div>
            <div className="booking-slot-card">
              <div className="booking-slot-head">
                <span className="eyebrow">Khung giờ</span>
                <strong>{form.appointmentDate || "Chọn ngày trên lịch"}</strong>
              </div>
              <div className="slot-grid">
                {loadingSlots ? <div className="status-box wide">Đang tải khung giờ...</div> : null}
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
                {!loadingSlots && form.appointmentDate && !slots.length ? (
                  <div className="form-alert error wide">Ngày này chưa có khung giờ phù hợp. Vui lòng chọn ngày khác.</div>
                ) : null}
                {!loadingSlots && !form.appointmentDate ? (
                  <div className="status-box wide">Chọn một ngày còn trống để xem khung giờ tiếp đón.</div>
                ) : null}
              </div>
            </div>
          </div>
        </section>
      ) : null}

      {step === 3 ? (
        <section className="wizard-panel">
          <span className="eyebrow">Liên hệ</span>
          <h2>Thông tin xác nhận</h2>
          {authenticated ? (
            <dl className="summary-list compact">
              <div><dt>Khách hàng</dt><dd>{profile?.fullname || profile?.fullName || "Tài khoản của tôi"}</dd></div>
              <div><dt>Email</dt><dd>{profile?.email || "Đang cập nhật"}</dd></div>
              <div><dt>Số điện thoại</dt><dd>{profile?.phone || "Đang cập nhật"}</dd></div>
            </dl>
          ) : (
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
          )}
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
            <div><dt>Khách hàng</dt><dd>{authenticated ? profile?.fullname || profile?.email || "Tài khoản của tôi" : `${form.guestFullName} - ${form.guestPhone}`}</dd></div>
          </dl>
        </section>
      ) : null}

      <div className="wizard-feedback" aria-live="polite">
        {loadingInitial ? <div className="status-box">Đang tải dữ liệu đặt lịch...</div> : null}
        {message ? <div className="form-alert error">{message}</div> : null}
      </div>

      <div className="wizard-actions">
        <button className="btn btn-secondary" type="button" onClick={back} disabled={step === 0 || submitting || validatingVin}>
          Quay lại
        </button>
        {step < STEPS.length - 1 ? (
          <button className="btn btn-primary" type="button" onClick={next} disabled={validatingVin || loadingInitial || !authReady}>
            {validatingVin ? "Đang kiểm tra VIN..." : "Tiếp tục"}
          </button>
        ) : (
          <button className="btn btn-primary" type="submit" disabled={submitting || loadingInitial || !authReady}>
            {submitting ? "Đang gửi..." : "Xác nhận lịch hẹn"}
          </button>
        )}
      </div>
    </form>
  );
}
