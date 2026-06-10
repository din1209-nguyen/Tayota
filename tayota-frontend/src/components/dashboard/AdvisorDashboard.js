"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  checkInServiceAppointment,
  checkInTestDriveAppointment,
  createAdvisorAppointment,
  createAdvisorHoliday,
  createAdvisorTimeSlot,
  deleteAdvisorHoliday,
  deleteAdvisorTimeSlot,
  getAdvisorDealership,
  getAdvisorAppointmentDetail,
  getAdvisorAppointments,
  getAdvisorHolidays,
  getAdvisorTimeSlots,
  getAvailabilityCalendar,
  getAvailableSlots,
  updateAdvisorAppointment,
  updateAdvisorHoliday,
  updateAdvisorTimeSlot,
} from "@/lib/services/appointments";
import { getAllCarVersions } from "@/lib/services/car";
import {
  assignCustomerVehicle,
  getActiveAdvisorMechanics,
  getCustomerVehicles,
  removeCustomerVehicle,
  searchAdvisorCustomers,
} from "@/lib/services/advisor";
import {
  assignTicketMechanic,
  cancelAdvisorServiceTicket,
  createWalkInServiceTicket,
  getAdvisorServiceTicketDetail,
  getAdvisorServiceTickets,
  getServiceInvoice,
} from "@/lib/services/workorders";
import { getAdvisorReport } from "@/lib/services/reports";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import ServiceBreakdown from "@/components/dashboard/ServiceBreakdown";
import { getValidDashboardTab } from "@/lib/dashboard-nav";
import { formatServiceTicketNote, statusLabel, statusPillClass, unwrapList } from "@/lib/format";

const APPOINTMENT_TYPE_FILTERS = [
  ["ALL", "Tất cả"],
  ["TEST_DRIVE", "Buổi hẹn lái thử"],
  ["SERVICE", "Dịch vụ chăm sóc xe"],
];
const APPOINTMENT_LIST_FILTERS = [
  ["PENDING_REVIEW", "Chờ xác nhận"],
  ["UPCOMING", "Sắp tới"],
  ["TODAY", "Hôm nay"],
  ["ALL", "Tất cả"],
];
const SLOT_TYPE_FILTERS = [
  ["ALL", "Tất cả"],
  ["SERVICE", "Dịch vụ"],
  ["TEST_DRIVE", "Lái thử"],
];
const TICKET_STATUS_FILTERS = [
  ["ALL", "Tất cả"],
  ["NEEDS_REASSIGNMENT", "Chờ phân công"],
  ["RECEIVING", "Đã tiếp nhận"],
  ["IN_PROGRESS", "Đang sửa"],
  ["COMPLETED", "Hoàn tất"],
  ["CANCELED", "Đã hủy"],
];
const PAGE_SIZE = 20;
const WEEKDAY_LABELS = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"];
const CUSTOMER_SEARCH_PLACEHOLDER = "Nhập tên, email hoặc số điện thoại khách hàng";
const CUSTOMER_SEARCH_HINT = "Tìm theo tên, email hoặc số điện thoại đã đăng ký.";
const REPORT_RANGE_FILTERS = [
  ["TODAY", "Hôm nay"],
  ["LAST_7_DAYS", "7 ngày"],
  ["THIS_MONTH", "Tháng này"],
  ["CUSTOM", "Tùy chỉnh"],
];
const REPORT_TYPE_FILTERS = [
  ["ALL", "Tất cả"],
  ["TEST_DRIVE", "Lái thử"],
  ["SERVICE", "Dịch vụ"],
];

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

function emptyAdvisorAppointment() {
  return {
    customerMode: "guest",
    userId: "",
    guestFullName: "",
    guestEmail: "",
    guestPhone: "",
    appointmentType: "SERVICE",
    appointmentDate: "",
    startTime: "",
    vinId: "",
    carVersionId: "",
    notes: "",
  };
}

function isVietnameseMessage(message) {
  return /[ăâđêôơưáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]/i.test(message)
    || /\b(vui lòng|không|khách|phiếu|dịch vụ|kỹ thuật|thông tin|chọn|nhập|thất bại)\b/i.test(message);
}

function getAdvisorServiceActionError(error, fallback = "Không thể tạo phiếu dịch vụ. Vui lòng kiểm tra lại thông tin và thử lại.") {
  const rawMessage = String(error?.message || "").trim();
  const normalized = rawMessage.toLowerCase();

  if (!rawMessage) return fallback;
  if (normalized.includes("the given id must not be null") || normalized.includes("id must not be null")) {
    return "Vui lòng chọn đầy đủ thông tin bắt buộc trước khi tạo phiếu dịch vụ.";
  }
  if (normalized.includes("invalid uuid") || normalized.includes("not a valid uuid") || normalized.includes("uuid")) {
    return "Thông tin đã chọn không hợp lệ. Vui lòng chọn lại khách hàng, VIN hoặc kỹ thuật viên.";
  }
  if (normalized.includes("not found")) {
    return "Không tìm thấy thông tin phù hợp. Vui lòng kiểm tra lại khách hàng, VIN hoặc kỹ thuật viên.";
  }
  if (normalized.includes("required") || normalized.includes("missing") || normalized.includes("must not be blank") || normalized.includes("must not be empty")) {
    return "Vui lòng nhập đầy đủ thông tin bắt buộc.";
  }
  if (isVietnameseMessage(rawMessage)) return rawMessage;
  return fallback;
}

function getWalkInValidationMessage(form) {
  if (!String(form.vinId || "").trim()) return "Vui lòng nhập hoặc chọn VIN trước khi tạo phiếu dịch vụ.";
  if (!String(form.mechanicId || "").trim()) return "Vui lòng chọn kỹ thuật viên trước khi tạo phiếu dịch vụ.";
  if (!form.userId && !String(form.guestFullName || "").trim()) {
    return "Vui lòng nhập họ tên khách hàng hoặc chọn khách hàng có tài khoản.";
  }
  return "";
}

function toDateInputValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function toReportDateDisplayValue(value) {
  const dateKey = toDateKey(value);
  const match = dateKey.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return "";
  return `${match[3]}/${match[2]}/${match[1]}`;
}

function parseReportDateDisplayValue(value) {
  const match = String(value || "").trim().match(/^(\d{1,2})[\/.-](\d{1,2})[\/.-](\d{4})$/);
  if (!match) return "";

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const date = new Date(year, month - 1, day);

  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return "";
  }

  return toDateInputValue(date);
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

function dayHasAvailableSlots(day) {
  if (!day || day.holiday) return false;
  if (isFalseAvailability(day.hasAvailableSlots) || isFalseAvailability(day.available)) return false;
  if (isTruthyAvailability(day.hasAvailableSlots) || isTruthyAvailability(day.available)) return true;
  if (Array.isArray(day.slots)) return day.slots.some((slot) => slot?.available !== false);
  const count = Number(day.availableSlotCount ?? day.availableSlotsCount ?? day.slotCount);
  return Number.isFinite(count) && count > 0;
}

function isTruthyAvailability(value) {
  return value === true || value === "true" || value === 1 || value === "1";
}

function isFalseAvailability(value) {
  return value === false || value === "false" || value === 0 || value === "0";
}

function toDateTime(item) {
  return `${item?.appointmentDate || ""} ${item?.startTime || ""}`.trim();
}

function appointmentTimestamp(item) {
  const value = item?.appointmentDate ? `${item.appointmentDate}T${item.startTime || "00:00:00"}` : item?.createdAt;
  const time = value ? new Date(value).getTime() : 0;
  return Number.isFinite(time) ? time : 0;
}

function toDateKey(value) {
  if (!value) return "";
  if (value instanceof Date) return toDateInputValue(value);
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }
  if (typeof value === "object" && value.year && value.month && value.day) {
    return `${value.year}-${String(value.month).padStart(2, "0")}-${String(value.day).padStart(2, "0")}`;
  }
  return String(value).slice(0, 10);
}

function firstDateKey(item, fields) {
  for (const field of fields) {
    const dateKey = toDateKey(item?.[field]);
    if (dateKey) return dateKey;
  }
  return "";
}

function getAdvisorReportRangeBounds(range, customFrom, customTo) {
  const today = new Date();
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());

  if (range === "LAST_7_DAYS") {
    const from = new Date(todayStart);
    from.setDate(todayStart.getDate() - 6);
    return { from: toDateInputValue(from), to: toDateInputValue(todayStart) };
  }

  if (range === "THIS_MONTH") {
    const from = new Date(todayStart.getFullYear(), todayStart.getMonth(), 1);
    return { from: toDateInputValue(from), to: toDateInputValue(todayStart) };
  }

  if (range === "CUSTOM") {
    return {
      from: customFrom || toDateInputValue(todayStart),
      to: customTo || customFrom || toDateInputValue(todayStart),
    };
  }

  return { from: toDateInputValue(todayStart), to: toDateInputValue(todayStart) };
}

function sortByNewest(items = []) {
  return [...items].sort((left, right) => appointmentTimestamp(right) - appointmentTimestamp(left));
}

function paginate(items, page, pageSize = PAGE_SIZE) {
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const safePage = Math.min(Math.max(page, 1), totalPages);
  const start = (safePage - 1) * pageSize;
  return {
    items: items.slice(start, start + pageSize),
    start,
    totalPages,
    page: safePage,
  };
}

function canEditAppointmentSchedule(appointment) {
  return appointment?.status === "PENDING";
}

function appointmentTypeLabel(type) {
  return type === "SERVICE" ? "Dịch vụ chăm sóc xe" : "Buổi hẹn lái thử";
}

function appointmentTypeClass(type) {
  return type === "SERVICE" ? "appointment-type-service" : "appointment-type-test-drive";
}

function formatVndZero(value) {
  const numeric = Number(value || 0);
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number.isFinite(numeric) ? numeric : 0);
}

function formatAdvisorDateTime(value) {
  if (!value) return "Đang cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function hasReachedServiceStatus(ticket, statuses) {
  return statuses.includes(String(ticket?.status || "").toUpperCase());
}

function serviceReceivedAtLabel(ticket) {
  if (!hasReachedServiceStatus(ticket, ["RECEIVING", "IN_PROGRESS", "COMPLETED"])) return "Chưa tiếp nhận";
  return formatAdvisorDateTime(ticket?.receivingAt);
}

function serviceProcessingAtLabel(ticket) {
  if (!hasReachedServiceStatus(ticket, ["IN_PROGRESS", "COMPLETED"])) return "Chưa bắt đầu";
  return formatAdvisorDateTime(ticket?.processingAt);
}

function serviceCompletedAtLabel(ticket) {
  if (!hasReachedServiceStatus(ticket, ["COMPLETED"])) return "Chưa hoàn tất";
  return formatAdvisorDateTime(ticket?.completedAt);
}

function serviceTimeLabel(ticket) {
  if (hasReachedServiceStatus(ticket, ["COMPLETED"]) && ticket?.completedAt) return ticket.completedAt;
  if (hasReachedServiceStatus(ticket, ["IN_PROGRESS", "COMPLETED"]) && ticket?.processingAt) return ticket.processingAt;
  if (hasReachedServiceStatus(ticket, ["RECEIVING", "IN_PROGRESS", "COMPLETED"]) && ticket?.receivingAt) return ticket.receivingAt;
  return ticket?.createdAt || ticket?.updatedAt || "";
}

function serviceTicketAmountLabel(ticket) {
  if (ticket?.status !== "COMPLETED") return "Chưa chốt";
  return formatVndZero(ticket?.totalAmount);
}

function canCancelServiceTicket(ticket) {
  return ["CONFIRMED", "NEEDS_REASSIGNMENT"].includes(String(ticket?.status || "").toUpperCase());
}

function ratingLabel(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return "Chưa có";
  return `${numeric.toFixed(1)} / 5`;
}

function shortDateLabel(value) {
  if (!value) return "Đang cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10);
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit" }).format(date);
}

function reportStatusColor(index) {
  const colors = ["#2563eb", "#0f766e", "#d97706", "#be123c", "#7c3aed", "#64748b", "#0891b2"];
  return colors[index % colors.length];
}

function nonZeroReportItems(items = []) {
  return items.filter((item) => Number(item?.count || 0) > 0);
}

function ReportDateInput({ value, onChange, label }) {
  const [displayValue, setDisplayValue] = useState(() => toReportDateDisplayValue(value));

  useEffect(() => {
    setDisplayValue(toReportDateDisplayValue(value));
  }, [value]);

  const handleChange = (event) => {
    const nextValue = event.target.value;
    setDisplayValue(nextValue);

    if (!nextValue.trim()) {
      onChange("");
      return;
    }

    const parsedValue = parseReportDateDisplayValue(nextValue);
    if (parsedValue) onChange(parsedValue);
  };

  const handleBlur = () => {
    const parsedValue = parseReportDateDisplayValue(displayValue);
    setDisplayValue(parsedValue ? toReportDateDisplayValue(parsedValue) : toReportDateDisplayValue(value));
  };

  return (
    <input
      aria-label={label}
      className="field advisor-report-date-field"
      inputMode="numeric"
      onBlur={handleBlur}
      onChange={handleChange}
      pattern="\d{1,2}/\d{1,2}/\d{4}"
      placeholder="dd/mm/yyyy"
      type="text"
      value={displayValue}
    />
  );
}

function ReportDonutChart({ title, items }) {
  const visibleItems = nonZeroReportItems(items);
  const total = visibleItems.reduce((sum, item) => sum + Number(item.count || 0), 0);
  const gradient = visibleItems.reduce((state, item, index) => {
    const value = Number(item.count || 0);
    const start = state.current;
    const end = start + (value / total) * 100;
    return {
      current: end,
      segments: [...state.segments, `${reportStatusColor(index)} ${start}% ${end}%`],
    };
  }, { current: 0, segments: [] }).segments.join(", ");

  return (
    <article className="advisor-report-chart">
      <div className="advisor-report-chart-head">
        <h3>{title}</h3>
        <strong>{total}</strong>
      </div>
      {total > 0 ? (
        <div className="advisor-donut-wrap">
          <div className="advisor-donut" style={{ background: `conic-gradient(${gradient})` }}><span>{total}</span></div>
          <div className="advisor-chart-legend">
            {visibleItems.map((item, index) => (
              <span key={item.status}><i style={{ background: reportStatusColor(index) }} />{statusLabel(item.status)} · {item.count}</span>
            ))}
          </div>
        </div>
      ) : <div className="advisor-report-empty">Chưa có dữ liệu trong bộ lọc này.</div>}
    </article>
  );
}

function RevenueChart({ items = [] }) {
  return (
    <article className="advisor-report-chart">
      <div className="advisor-report-chart-head">
        <h3>Doanh thu theo ngày</h3>
        <strong>{formatVndZero(items.reduce((sum, item) => sum + Number(item.amount || 0), 0))}</strong>
      </div>
      {items.length ? (
        <div className="advisor-revenue-list">
          {items.map((item) => (
            <div className="advisor-revenue-item" key={item.date}>
              <span>{shortDateLabel(item.date)}</span>
              <strong>{formatVndZero(item.amount)}</strong>
            </div>
          ))}
        </div>
      ) : <div className="advisor-report-empty">Chưa có doanh thu trong bộ lọc này.</div>}
    </article>
  );
}

function RatingDistributionChart({ items = [] }) {
  const maxCount = Math.max(...items.map((item) => Number(item.count || 0)), 0);

  return (
    <article className="advisor-report-chart">
      <div className="advisor-report-chart-head">
        <h3>Phân bố đánh giá</h3>
        <strong>{items.reduce((sum, item) => sum + Number(item.count || 0), 0)}</strong>
      </div>
      {maxCount > 0 ? (
        <div className="advisor-rating-bars">
          {items.map((item) => {
            const count = Number(item.count || 0);
            const width = count > 0 ? Math.max(4, Math.round((count / maxCount) * 100)) : 0;
            return (
              <div className="advisor-rating-bar" key={item.rating}>
                <span>{item.rating} sao</span>
                <div><i style={{ width: `${width}%` }} /></div>
                <strong>{count}</strong>
              </div>
            );
          })}
        </div>
      ) : <div className="advisor-report-empty">Chưa có đánh giá trong bộ lọc này.</div>}
    </article>
  );
}

function Pagination({ page, totalPages, totalItems, start, count, onChange }) {
  if (totalItems <= PAGE_SIZE) return null;

  return (
    <div className="advisor-pagination">
      <span>Hiển thị {start + 1}-{start + count} / {totalItems}</span>
      <div>
        <button className="btn btn-ghost" type="button" disabled={page <= 1} onClick={() => onChange(page - 1)}>Trước</button>
        <strong>{page} / {totalPages}</strong>
        <button className="btn btn-ghost" type="button" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>Sau</button>
      </div>
    </div>
  );
}

export default function AdvisorDashboard() {
  const searchParams = useSearchParams();
  const tab = getValidDashboardTab("SERVICE_ADVISOR", searchParams.get("tab"));
  const [appointmentTypeFilter, setAppointmentTypeFilter] = useState("ALL");
  const [appointmentListFilter, setAppointmentListFilter] = useState("PENDING_REVIEW");
  const [slotTypeFilter, setSlotTypeFilter] = useState("ALL");
  const [ticketStatus, setTicketStatus] = useState("NEEDS_REASSIGNMENT");
  const [appointments, setAppointments] = useState([]);
  const [allAppointments, setAllAppointments] = useState([]);
  const [selectedAppointment, setSelectedAppointment] = useState(null);
  const [recentlyConfirmedAppointmentId, setRecentlyConfirmedAppointmentId] = useState(null);
  const [activePanel, setActivePanel] = useState("list");
  const [appointmentPage, setAppointmentPage] = useState(1);
  const [ticketPage, setTicketPage] = useState(1);
  const [slots, setSlots] = useState([]);
  const [holidays, setHolidays] = useState([]);
  const [mechanics, setMechanics] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [allTickets, setAllTickets] = useState([]);
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [customers, setCustomers] = useState([]);
  const [customerKeyword, setCustomerKeyword] = useState("");
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [customerVehicles, setCustomerVehicles] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [appointmentForm, setAppointmentForm] = useState({ appointmentDate: "", startTime: "", cancelReason: "", notes: "" });
  const [editSchedule, setEditSchedule] = useState(false);
  const [scheduleMonth, setScheduleMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [scheduleDays, setScheduleDays] = useState([]);
  const [scheduleSlots, setScheduleSlots] = useState([]);
  const [loadingScheduleDays, setLoadingScheduleDays] = useState(false);
  const [loadingScheduleSlots, setLoadingScheduleSlots] = useState(false);
  const [scheduleMessage, setScheduleMessage] = useState("");
  const [checkInMessage, setCheckInMessage] = useState("");
  const [confirmDialog, setConfirmDialog] = useState(null);
  const [confirmMessage, setConfirmMessage] = useState("");
  const [checkInForm, setCheckInForm] = useState({ mechanicId: "", mileageAtService: "", vehicleCondition: "", notes: "" });
  const [advisorDealershipId, setAdvisorDealershipId] = useState("");
  const [advisorAppointmentForm, setAdvisorAppointmentForm] = useState(emptyAdvisorAppointment());
  const [carVersions, setCarVersions] = useState([]);
  const [createScheduleMonth, setCreateScheduleMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [createScheduleDays, setCreateScheduleDays] = useState([]);
  const [createScheduleSlots, setCreateScheduleSlots] = useState([]);
  const [loadingCreateScheduleDays, setLoadingCreateScheduleDays] = useState(false);
  const [loadingCreateScheduleSlots, setLoadingCreateScheduleSlots] = useState(false);
  const [createScheduleMessage, setCreateScheduleMessage] = useState("");
  const [appointmentCustomerSearchMessage, setAppointmentCustomerSearchMessage] = useState("");
  const [createAppointmentOpen, setCreateAppointmentOpen] = useState(false);
  const [createWalkInOpen, setCreateWalkInOpen] = useState(false);
  const [walkInMessage, setWalkInMessage] = useState("");
  const [walkInCustomerSearchMessage, setWalkInCustomerSearchMessage] = useState("");
  const [vehicleCustomerSearchMessage, setVehicleCustomerSearchMessage] = useState("");
  const [walkInForm, setWalkInForm] = useState(emptyWalkIn());
  const [vehicleForm, setVehicleForm] = useState({ vinId: "", note: "" });
  const [reassignMechanicId, setReassignMechanicId] = useState("");
  const [reassignMessage, setReassignMessage] = useState("");
  const [ticketCancelReason, setTicketCancelReason] = useState("");
  const [ticketCancelMessage, setTicketCancelMessage] = useState("");
  const [slotForm, setSlotForm] = useState({ appointmentType: "SERVICE", startTime: "", endTime: "" });
  const [slotCreateMessage, setSlotCreateMessage] = useState("");
  const [slotTabMessage, setSlotTabMessage] = useState(null);
  const [holidayForm, setHolidayForm] = useState({ holidayDate: "", reason: "" });
  const [holidayCreateMessage, setHolidayCreateMessage] = useState("");
  const [holidayTabMessage, setHolidayTabMessage] = useState(null);
  const [reportRange, setReportRange] = useState("TODAY");
  const [reportType, setReportType] = useState("ALL");
  const [reportCustomFrom, setReportCustomFrom] = useState(() => toDateInputValue(new Date()));
  const [reportCustomTo, setReportCustomTo] = useState(() => toDateInputValue(new Date()));
  const [advisorReport, setAdvisorReport] = useState(null);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportMessage, setReportMessage] = useState("");

  const visibleAppointments = useMemo(() => {
    const todayKey = toDateInputValue(new Date());
    const listFiltered = appointmentListFilter === "PENDING_REVIEW"
      ? appointments.filter((item) => item.status === "PENDING")
      : appointmentListFilter === "UPCOMING"
        ? appointments.filter((item) => {
          const appointmentDateKey = toDateKey(item?.appointmentDate);
          return item.status === "CONFIRMED" && appointmentDateKey && appointmentDateKey > todayKey;
        })
        : appointmentListFilter === "TODAY"
          ? appointments.filter((item) => toDateKey(item?.appointmentDate) === todayKey)
          : appointments;
    const typeFiltered = appointmentTypeFilter === "ALL"
      ? listFiltered
      : listFiltered.filter((item) => item.type === appointmentTypeFilter);
    return typeFiltered;
  }, [appointmentListFilter, appointmentTypeFilter, appointments]);
  const sortedAppointments = useMemo(() => sortByNewest(visibleAppointments), [visibleAppointments]);
  const pagedAppointments = useMemo(() => paginate(sortedAppointments, appointmentPage), [appointmentPage, sortedAppointments]);
  const pagedTickets = useMemo(() => paginate(tickets, ticketPage), [ticketPage, tickets]);
  const filteredSlots = useMemo(() => (
    slots
      .filter((slot) => slotTypeFilter === "ALL" || slot.appointmentType === slotTypeFilter)
      .sort((left, right) => String(left.startTime || "").localeCompare(String(right.startTime || "")))
  ), [slotTypeFilter, slots]);
  const sortedHolidays = useMemo(() => (
    [...holidays].sort((left, right) => String(right.holidayDate || "").localeCompare(String(left.holidayDate || "")))
  ), [holidays]);
  const daysByDate = useMemo(() => new Map(scheduleDays.map((day) => [day.date, day])), [scheduleDays]);
  const scheduleCells = useMemo(() => getCalendarCells(scheduleMonth, daysByDate), [daysByDate, scheduleMonth]);
  const createDaysByDate = useMemo(() => new Map(createScheduleDays.map((day) => [day.date, day])), [createScheduleDays]);
  const createScheduleCells = useMemo(() => getCalendarCells(createScheduleMonth, createDaysByDate), [createDaysByDate, createScheduleMonth]);
  const mechanicNameById = useMemo(() => new Map(mechanics.map((item) => [String(item.id), item.fullName || item.id])), [mechanics]);

  const overviewStats = useMemo(() => {
    const todayKey = toDateInputValue(new Date());
    const todayAppointments = allAppointments.filter((item) => toDateKey(item?.appointmentDate) === todayKey);

    return {
      appointmentsToday: todayAppointments.length,
      pendingAppointments: todayAppointments.filter((item) => item.status === "PENDING").length,
      checkedInCustomers: todayAppointments.filter((item) => item.status === "CHECKED_IN").length,
      activeServiceTickets: allTickets.filter((item) => ["RECEIVING", "IN_PROGRESS"].includes(item.status)).length,
      completedTestDrives: allAppointments.filter((item) => (
        item.type === "TEST_DRIVE"
        && item.status === "COMPLETED"
        && firstDateKey(item, ["completedAt", "appointmentDate"]) === todayKey
      )).length,
      completedServices: allTickets.filter((item) => (
        item.status === "COMPLETED"
        && firstDateKey(item, ["completedAt", "receivingAt", "createdAt"]) === todayKey
      )).length,
    };
  }, [allAppointments, allTickets]);
  const overviewDateLabel = useMemo(() => (
    new Intl.DateTimeFormat("vi-VN", { weekday: "long", day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date())
  ), []);
  const reportQuery = useMemo(() => (
    getAdvisorReportRangeBounds(reportRange, reportCustomFrom, reportCustomTo)
  ), [reportCustomFrom, reportCustomTo, reportRange]);
  const getAdvisorVehicleLabel = useCallback((item) => (
    item?.carVersionName
    || item?.versionName
    || item?.modelName
    || item?.vinId
    || item?.carVersionId
    || "Đang cập nhật"
  ), []);
  const getAdvisorDealershipLabel = useCallback((item) => (
    item?.dealershipName
    || item?.invoice?.dealershipName
    || item?.dealershipId
    || "Đang cập nhật"
  ), []);
  const getAdvisorTicketTitle = useCallback((ticket) => (
    ticket?.invoice?.carVersionName
    || ticket?.carVersionName
    || ticket?.customerFullName
    || ticket?.vinId
    || "Phiếu dịch vụ"
  ), []);

  const loadBase = useCallback(async function loadBase() {
    setLoading(true);
    setMessage("");
    try {
      const [nextAppointments, nextSlots, nextHolidays, nextMechanics, nextTickets, dealership, versions, nextAllTickets] = await Promise.all([
        getAdvisorAppointments("ALL"),
        getAdvisorTimeSlots(),
        getAdvisorHolidays(),
        getActiveAdvisorMechanics(),
        getAdvisorServiceTickets({ status: ticketStatus }),
        getAdvisorDealership(),
        getAllCarVersions(),
        ticketStatus === "ALL" ? Promise.resolve(null) : getAdvisorServiceTickets({ status: "ALL" }),
      ]);
      setAppointments(unwrapList(nextAppointments));
      setSlots(unwrapList(nextSlots));
      setHolidays(unwrapList(nextHolidays));
      setMechanics(unwrapList(nextMechanics));
      setTickets(unwrapList(nextTickets));
      setAllAppointments(unwrapList(nextAppointments));
      setAllTickets(ticketStatus === "ALL" ? unwrapList(nextTickets) : unwrapList(nextAllTickets));
      setAdvisorDealershipId(dealership?.dealershipId || "");
      setCarVersions(unwrapList(versions));
    } catch (error) {
      setMessage(error.message || "Không thể tải dashboard cố vấn.");
    } finally {
      setLoading(false);
    }
  }, [ticketStatus]);

  useEffect(() => {
    loadBase();
  }, [loadBase]);

  useEffect(() => {
    if (tab !== "reports") return;
    let ignore = false;

    async function loadAdvisorReport() {
      setReportLoading(true);
      setReportMessage("");
      try {
        const result = await getAdvisorReport({ ...reportQuery, type: reportType });
        if (!ignore) setAdvisorReport(result || null);
      } catch (error) {
        if (!ignore) setReportMessage(error.message || "Không thể tải báo cáo đại lý.");
      } finally {
        if (!ignore) setReportLoading(false);
      }
    }

    loadAdvisorReport();

    return () => {
      ignore = true;
    };
  }, [reportQuery, reportType, tab]);

  useEffect(() => {
    setAppointmentPage(1);
  }, [appointmentListFilter, appointmentTypeFilter]);

  useEffect(() => {
    setTicketPage(1);
  }, [ticketStatus]);

  useEffect(() => {
    if (appointmentPage > pagedAppointments.totalPages) {
      setAppointmentPage(pagedAppointments.totalPages);
    }
  }, [appointmentPage, pagedAppointments.totalPages]);

  useEffect(() => {
    if (ticketPage > pagedTickets.totalPages) {
      setTicketPage(pagedTickets.totalPages);
    }
  }, [pagedTickets.totalPages, ticketPage]);

  useEffect(() => {
    setActivePanel("list");
    setSelectedAppointment(null);
    setSelectedTicket(null);
    setEditSchedule(false);
  }, [tab]);

  useEffect(() => {
    let alive = true;

    async function loadScheduleDays() {
      if (!editSchedule || !selectedAppointment?.dealershipId) {
        setScheduleDays([]);
        return;
      }

      setLoadingScheduleDays(true);
      setScheduleMessage("");
      try {
        const { first, last } = getMonthBounds(scheduleMonth);
        const result = await getAvailabilityCalendar({
          dealershipId: selectedAppointment.dealershipId,
          appointmentType: selectedAppointment.type,
          from: toDateInputValue(first),
          to: toDateInputValue(last),
        });
        if (alive) setScheduleDays(normalizeCalendarDays(result));
      } catch (error) {
        if (alive) {
          setScheduleDays([]);
          setScheduleMessage(error.message || "Không thể tải ngày làm việc.");
        }
      } finally {
        if (alive) setLoadingScheduleDays(false);
      }
    }

    loadScheduleDays();
    return () => {
      alive = false;
    };
  }, [editSchedule, scheduleMonth, selectedAppointment?.dealershipId, selectedAppointment?.type]);

  useEffect(() => {
    let alive = true;

    async function loadScheduleSlots() {
      if (!editSchedule || !selectedAppointment?.dealershipId || !appointmentForm.appointmentDate) {
        setScheduleSlots([]);
        return;
      }

      setLoadingScheduleSlots(true);
      setScheduleMessage("");
      try {
        const result = await getAvailableSlots({
          dealershipId: selectedAppointment.dealershipId,
          appointmentType: selectedAppointment.type,
          appointmentDate: appointmentForm.appointmentDate,
        });
        const availableSlots = Array.isArray(result?.slots)
          ? result.slots.filter((slot) => slot?.available !== false)
          : unwrapList(result).filter((slot) => slot?.available !== false);
        if (alive) {
          setScheduleSlots(availableSlots);
          if (!availableSlots.length) {
            setScheduleMessage(result?.holiday ? result?.holidayReason || "Đại lý nghỉ trong ngày này." : "Ngày này chưa có khung giờ phù hợp.");
          }
        }
      } catch (error) {
        if (alive) {
          setScheduleSlots([]);
          setScheduleMessage(error.message || "Không thể tải khung giờ phù hợp.");
        }
      } finally {
        if (alive) setLoadingScheduleSlots(false);
      }
    }

    loadScheduleSlots();
    return () => {
      alive = false;
    };
  }, [appointmentForm.appointmentDate, editSchedule, selectedAppointment?.dealershipId, selectedAppointment?.type]);

  useEffect(() => {
    let alive = true;

    async function loadCreateScheduleDays() {
      if (!createAppointmentOpen || !advisorDealershipId) {
        setCreateScheduleDays([]);
        return;
      }

      setLoadingCreateScheduleDays(true);
      setCreateScheduleMessage("");
      try {
        const { first, last } = getMonthBounds(createScheduleMonth);
        const result = await getAvailabilityCalendar({
          dealershipId: advisorDealershipId,
          appointmentType: advisorAppointmentForm.appointmentType,
          from: toDateInputValue(first),
          to: toDateInputValue(last),
        });
        if (alive) {
          const normalizedDays = normalizeCalendarDays(result);
          setCreateScheduleDays(normalizedDays);
          if (!normalizedDays.some(dayHasAvailableSlots)) {
            setCreateScheduleMessage("Chưa có ngày khả dụng. Vui lòng kiểm tra khung giờ làm việc hoặc ngày nghỉ đại lý.");
          }
        }
      } catch (error) {
        if (alive) {
          setCreateScheduleDays([]);
          setCreateScheduleMessage(error.message || "Không thể tải ngày làm việc.");
        }
      } finally {
        if (alive) setLoadingCreateScheduleDays(false);
      }
    }

    loadCreateScheduleDays();
    return () => {
      alive = false;
    };
  }, [advisorAppointmentForm.appointmentType, advisorDealershipId, createAppointmentOpen, createScheduleMonth]);

  useEffect(() => {
    let alive = true;

    async function loadCreateScheduleSlots() {
      if (!createAppointmentOpen || !advisorDealershipId || !advisorAppointmentForm.appointmentDate) {
        setCreateScheduleSlots([]);
        return;
      }

      setLoadingCreateScheduleSlots(true);
      setCreateScheduleMessage("");
      try {
        const result = await getAvailableSlots({
          dealershipId: advisorDealershipId,
          appointmentType: advisorAppointmentForm.appointmentType,
          appointmentDate: advisorAppointmentForm.appointmentDate,
        });
        const availableSlots = Array.isArray(result?.slots)
          ? result.slots.filter((slot) => slot?.available !== false)
          : unwrapList(result).filter((slot) => slot?.available !== false);
        if (alive) {
          setCreateScheduleSlots(availableSlots);
          if (!availableSlots.length) {
            setCreateScheduleMessage(result?.holiday ? result?.holidayReason || "Đại lý nghỉ trong ngày này." : "Ngày này chưa có khung giờ phù hợp.");
          }
        }
      } catch (error) {
        if (alive) {
          setCreateScheduleSlots([]);
          setCreateScheduleMessage(error.message || "Không thể tải khung giờ phù hợp.");
        }
      } finally {
        if (alive) setLoadingCreateScheduleSlots(false);
      }
    }

    loadCreateScheduleSlots();
    return () => {
      alive = false;
    };
  }, [advisorAppointmentForm.appointmentDate, advisorAppointmentForm.appointmentType, advisorDealershipId, createAppointmentOpen]);

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

  function requestConfirmation(config) {
    setConfirmMessage("");
    setConfirmDialog(config);
  }

  async function confirmCurrentAction() {
    if (!confirmDialog) return;
    setActionLoading(true);
    setConfirmMessage("");
    try {
      const result = await confirmDialog.action();
      if (confirmDialog.successMessage) setMessage(confirmDialog.successMessage);
      await loadBase();
      if (confirmDialog.afterSuccess) await confirmDialog.afterSuccess(result);
      setConfirmDialog(null);
    } catch (error) {
      if (confirmDialog.onError) {
        confirmDialog.onError(error);
        setConfirmDialog(null);
      } else {
        setConfirmMessage(error.message || "Thao tác thất bại.");
      }
    } finally {
      setActionLoading(false);
    }
  }

  function backToList() {
    setActivePanel("list");
    setSelectedAppointment(null);
    setRecentlyConfirmedAppointmentId(null);
    setSelectedTicket(null);
    setEditSchedule(false);
    setScheduleMessage("");
    setCheckInMessage("");
    setReassignMessage("");
    setTicketCancelReason("");
    setTicketCancelMessage("");
    setSlotCreateMessage("");
    setHolidayCreateMessage("");
  }

  async function openAppointment(id, { edit = false, panel = "appointment-detail" } = {}) {
    setActionLoading(true);
    setMessage("");
    setRecentlyConfirmedAppointmentId(null);
    try {
      const detail = await getAdvisorAppointmentDetail(id);
      setSelectedAppointment(detail);
      setAppointmentForm({
        appointmentDate: detail.appointmentDate || "",
        startTime: detail.startTime || "",
        cancelReason: detail.cancelReason || "",
        notes: detail.notes || "",
      });
      setScheduleMessage("");
      setCheckInMessage("");
      setScheduleSlots([]);
      if (detail.appointmentDate) {
        const date = new Date(`${detail.appointmentDate}T00:00:00`);
        setScheduleMonth(new Date(date.getFullYear(), date.getMonth(), 1));
      }
      setEditSchedule(Boolean(edit && canEditAppointmentSchedule(detail)));
      setActivePanel(panel);
    } catch (error) {
      setMessage(error.message || "Không thể tải chi tiết lịch hẹn.");
    } finally {
      setActionLoading(false);
    }
  }

  async function updateSelectedAppointment(payload, successMessage) {
    if (!selectedAppointment?.id) return;
    requestConfirmation({
      title: payload.status === "CANCELED" ? "Xác nhận hủy lịch hẹn" : payload.status === "COMPLETED" ? "Xác nhận hoàn tất lái thử" : "Xác nhận thao tác",
      description: payload.status === "CANCELED"
        ? "Lịch hẹn sẽ được chuyển sang trạng thái đã hủy."
        : payload.status === "COMPLETED"
          ? "Buổi hẹn lái thử sẽ được đánh dấu hoàn tất."
          : "Lịch hẹn sẽ được xác nhận.",
      action: () => updateAdvisorAppointment(selectedAppointment.id, payload),
      successMessage,
      afterSuccess: (result) => {
        setSelectedAppointment(result);
        if (payload.status === "CONFIRMED") {
          setRecentlyConfirmedAppointmentId(result?.id || selectedAppointment.id);
        } else if (["CANCELED", "COMPLETED", "CHECKED_IN"].includes(payload.status)) {
          setRecentlyConfirmedAppointmentId(null);
        }
      },
    });
  }

  async function saveSchedule() {
    if (!appointmentForm.appointmentDate || !appointmentForm.startTime) {
      setScheduleMessage("Vui lòng chọn ngày làm việc và khung giờ phù hợp.");
      return;
    }
    requestConfirmation({
      title: "Xác nhận đổi lịch hẹn",
      description: "Ngày giờ của lịch hẹn sẽ được cập nhật theo lựa chọn hiện tại.",
      action: () => updateAdvisorAppointment(selectedAppointment.id, {
        appointmentDate: appointmentForm.appointmentDate,
        startTime: appointmentForm.startTime,
      }),
      successMessage: "Đã đổi lịch hẹn.",
      afterSuccess: (result) => {
        setSelectedAppointment(result);
        setEditSchedule(false);
        setScheduleMessage("");
      },
    });
  }

  function changeAdvisorAppointmentType(nextType) {
    setAdvisorAppointmentForm((current) => ({
      ...current,
      appointmentType: nextType,
      appointmentDate: "",
      startTime: "",
      vinId: nextType === "SERVICE" ? current.vinId : "",
      carVersionId: nextType === "TEST_DRIVE" ? current.carVersionId : "",
    }));
    setCreateScheduleSlots([]);
    setCreateScheduleMessage("");
  }

  async function submitAdvisorAppointment(event) {
    event.preventDefault();
    if (!advisorAppointmentForm.appointmentDate || !advisorAppointmentForm.startTime) {
      setCreateScheduleMessage("Vui lòng chọn ngày và khung giờ phù hợp.");
      return;
    }
    if (advisorAppointmentForm.customerMode === "user" && !advisorAppointmentForm.userId) {
      setCreateScheduleMessage("Vui lòng chọn khách hàng đã có tài khoản.");
      return;
    }

    const payload = {
      appointmentType: advisorAppointmentForm.appointmentType,
      appointmentDate: advisorAppointmentForm.appointmentDate,
      startTime: advisorAppointmentForm.startTime,
      notes: advisorAppointmentForm.notes,
      userId: advisorAppointmentForm.customerMode === "user" ? advisorAppointmentForm.userId : null,
      guestFullName: advisorAppointmentForm.customerMode === "guest" ? advisorAppointmentForm.guestFullName : "",
      guestEmail: advisorAppointmentForm.customerMode === "guest" ? advisorAppointmentForm.guestEmail : "",
      guestPhone: advisorAppointmentForm.customerMode === "guest" ? advisorAppointmentForm.guestPhone : "",
      vinId: advisorAppointmentForm.appointmentType === "SERVICE" ? advisorAppointmentForm.vinId : "",
      carVersionId: advisorAppointmentForm.appointmentType === "TEST_DRIVE" ? advisorAppointmentForm.carVersionId : "",
    };

    setCreateScheduleMessage("");
    requestConfirmation({
      title: "Xác nhận tạo lịch hẹn",
      description: "Lịch hẹn sẽ được tạo theo thông tin và khung giờ đang chọn.",
      action: () => createAdvisorAppointment(payload),
      successMessage: "Đã tạo lịch hẹn đã xác nhận.",
      afterSuccess: (result) => {
        setAdvisorAppointmentForm(emptyAdvisorAppointment());
        setSelectedCustomer(null);
        setCustomerVehicles([]);
        setCreateScheduleDays([]);
        setCreateScheduleSlots([]);
        closeCreateAppointmentModal();
        setAppointmentListFilter("ALL");
        setAppointmentTypeFilter(result?.type || result?.appointmentType || payload.appointmentType || "ALL");
      },
      onError: (error) => {
        setCreateScheduleMessage(error.message || "Không thể tạo lịch hẹn.");
      },
    });
  }

  async function toggleTimeSlot(slot) {
    const isActive = slot.active !== false;
    const nextActive = !isActive;
    setActionLoading(true);
    setSlotTabMessage(null);
    try {
      await updateAdvisorTimeSlot(slot.id, { active: nextActive });
      setSlots((current) => current.map((item) => (
        item.id === slot.id ? { ...item, active: nextActive } : item
      )));
      setSlotTabMessage({ type: "success", text: nextActive ? "Đã bật lại khung giờ." : "Đã tắt khung giờ." });
    } catch (error) {
      setSlotTabMessage({ type: "error", text: error.message || "Không thể cập nhật khung giờ." });
    } finally {
      setActionLoading(false);
    }
  }

  async function toggleHoliday(holiday) {
    const isActive = holiday.active !== false;
    const nextActive = !isActive;
    setActionLoading(true);
    setHolidayTabMessage(null);
    try {
      await updateAdvisorHoliday(holiday.id, { active: nextActive });
      setHolidays((current) => current.map((item) => (
        item.id === holiday.id ? { ...item, active: nextActive } : item
      )));
      setHolidayTabMessage({ type: "success", text: nextActive ? "Đã bật lại ngày nghỉ." : "Đã tắt ngày nghỉ." });
    } catch (error) {
      setHolidayTabMessage({ type: "error", text: error.message || "Không thể cập nhật ngày nghỉ." });
    } finally {
      setActionLoading(false);
    }
  }

  async function removeTimeSlot(slot) {
    setActionLoading(true);
    setSlotTabMessage(null);
    try {
      await deleteAdvisorTimeSlot(slot.id);
      setSlots((current) => current.filter((item) => item.id !== slot.id));
      setSlotTabMessage({ type: "success", text: "Đã xóa khung giờ." });
    } catch (error) {
      setSlotTabMessage({ type: "error", text: error.message || "Không thể xóa khung giờ." });
    } finally {
      setActionLoading(false);
    }
  }

  async function removeHoliday(holiday) {
    setActionLoading(true);
    setHolidayTabMessage(null);
    try {
      await deleteAdvisorHoliday(holiday.id);
      setHolidays((current) => current.filter((item) => item.id !== holiday.id));
      setHolidayTabMessage({ type: "success", text: "Đã xóa ngày nghỉ." });
    } catch (error) {
      setHolidayTabMessage({ type: "error", text: error.message || "Không thể xóa ngày nghỉ." });
    } finally {
      setActionLoading(false);
    }
  }

  async function searchCustomers() {
    if (!customerKeyword.trim()) return;
    const showInWalkInModal = createWalkInOpen;
    const showInCreateModal = createAppointmentOpen;
    const showInVehiclePanel = tab === "vehicles" && !showInWalkInModal && !showInCreateModal;
    if (showInWalkInModal) setWalkInCustomerSearchMessage("");
    if (showInCreateModal) setAppointmentCustomerSearchMessage("");
    if (showInVehiclePanel) setVehicleCustomerSearchMessage("");
    setActionLoading(true);
    setMessage("");
    try {
      const result = await searchAdvisorCustomers({ keyword: customerKeyword, size: 8 });
      const nextCustomers = unwrapList(result);
      setCustomers(nextCustomers);
      if (!nextCustomers.length) {
        const notFoundMessage = "Không tìm thấy khách hàng phù hợp.";
        if (showInWalkInModal) setWalkInCustomerSearchMessage(notFoundMessage);
        else if (showInCreateModal) setAppointmentCustomerSearchMessage(notFoundMessage);
        else if (showInVehiclePanel) setVehicleCustomerSearchMessage(notFoundMessage);
        else setMessage(notFoundMessage);
      }
    } catch (error) {
      const errorMessage = error.message || "Không thể tìm khách hàng.";
      if (showInWalkInModal) setWalkInCustomerSearchMessage(errorMessage);
      else if (showInCreateModal) setAppointmentCustomerSearchMessage(errorMessage);
      else if (showInVehiclePanel) setVehicleCustomerSearchMessage(errorMessage);
      else setMessage(errorMessage);
    } finally {
      setActionLoading(false);
    }
  }

  function changeCustomerKeyword(value) {
    setCustomerKeyword(value);
    if (createAppointmentOpen) setAppointmentCustomerSearchMessage("");
    if (createWalkInOpen) setWalkInCustomerSearchMessage("");
    if (tab === "vehicles") setVehicleCustomerSearchMessage("");
  }

  async function chooseCustomer(customer) {
    const showInWalkInModal = createWalkInOpen;
    const showInCreateModal = createAppointmentOpen;
    const showInVehiclePanel = tab === "vehicles" && !showInWalkInModal && !showInCreateModal;
    if (showInWalkInModal) setWalkInCustomerSearchMessage("");
    if (showInCreateModal) setAppointmentCustomerSearchMessage("");
    if (showInVehiclePanel) setVehicleCustomerSearchMessage("");
    setSelectedCustomer(customer);
    setWalkInForm((current) => ({
      ...current,
      userId: customer.id,
      guestFullName: "",
      guestEmail: "",
      guestPhone: "",
      vinId: "",
    }));
    setAdvisorAppointmentForm((current) => ({
      ...current,
      customerMode: "user",
      userId: customer.id,
      guestFullName: "",
      guestEmail: "",
      guestPhone: "",
      vinId: "",
    }));
    setActionLoading(true);
    setMessage("");
    try {
      const result = await getCustomerVehicles(customer.id);
      setCustomerVehicles(unwrapList(result));
    } catch (error) {
      const errorMessage = error.message || "Không thể tải danh sách VIN của khách.";
      setCustomerVehicles([]);
      if (showInWalkInModal) setWalkInCustomerSearchMessage(errorMessage);
      else if (showInCreateModal) setAppointmentCustomerSearchMessage(errorMessage);
      else if (showInVehiclePanel) setVehicleCustomerSearchMessage(errorMessage);
      else setMessage(errorMessage);
    } finally {
      setActionLoading(false);
    }
  }

  function clearSelectedCustomer() {
    setAppointmentCustomerSearchMessage("");
    setWalkInCustomerSearchMessage("");
    setVehicleCustomerSearchMessage("");
    setSelectedCustomer(null);
    setCustomerVehicles([]);
    setWalkInForm((current) => ({ ...current, userId: "", vinId: "" }));
    setAdvisorAppointmentForm((current) => ({ ...current, customerMode: "guest", userId: "", vinId: "" }));
  }

  function changeAdvisorCustomerMode(nextMode) {
    setAppointmentCustomerSearchMessage("");
    setAdvisorAppointmentForm((current) => ({
      ...current,
      customerMode: nextMode,
      userId: nextMode === "guest" ? "" : current.userId,
      vinId: nextMode === "guest" ? "" : current.vinId,
    }));
    if (nextMode === "guest") {
      setSelectedCustomer(null);
      setCustomerVehicles([]);
    }
  }

  async function openTicket(id) {
    setActionLoading(true);
    setMessage("");
    try {
      const detail = await getAdvisorServiceTicketDetail(id);
      const ticket = detail?.serviceTicket || detail || {};
      const invoice = ticket.status === "COMPLETED"
        ? await getServiceInvoice(id).catch(() => null)
        : null;
      setSelectedTicket({ ...detail, invoice });
      setReassignMechanicId("");
      setReassignMessage("");
      setTicketCancelReason("");
      setTicketCancelMessage("");
      setActivePanel("ticket-detail");
    } catch (error) {
      setMessage(error.message || "Không thể tải chi tiết phiếu dịch vụ.");
    } finally {
      setActionLoading(false);
    }
  }

  async function cancelServiceTicket() {
    const ticket = selectedTicket?.serviceTicket || selectedTicket;
    const ticketId = ticket?.id;
    const reason = ticketCancelReason.trim();

    if (!ticketId) return;
    if (!reason) {
      setTicketCancelMessage("Vui lòng nhập lý do hủy phiếu dịch vụ.");
      return;
    }

    setTicketCancelMessage("");
    requestConfirmation({
      title: "Xác nhận hủy phiếu dịch vụ",
      description: "Phiếu dịch vụ chưa tiếp nhận sẽ được chuyển sang trạng thái đã hủy.",
      action: () => cancelAdvisorServiceTicket(ticketId, { reason }),
      successMessage: "Đã hủy phiếu dịch vụ.",
      afterSuccess: async () => {
        setTicketCancelReason("");
        await openTicket(ticketId);
        setMessage("Đã hủy phiếu dịch vụ.");
      },
      onError: (error) => {
        setTicketCancelMessage(error.message || "Không thể hủy phiếu dịch vụ.");
      },
    });
  }

  async function reassignTicket() {
    if (!selectedTicket?.serviceTicket?.id || !reassignMechanicId) {
      setReassignMessage("Vui lòng chọn kỹ thuật viên để phân công lại.");
      return;
    }
    setReassignMessage("");
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
    setCheckInMessage("");
    if (selectedAppointment.type === "TEST_DRIVE") {
      requestConfirmation({
        title: "Xác nhận check-in",
        description: "Khách sẽ được ghi nhận đã tới cho buổi hẹn lái thử.",
        action: () => checkInTestDriveAppointment(selectedAppointment.id),
        successMessage: "Check-in lái thử thành công.",
        onError: (error) => {
          setCheckInMessage(error.message || "Không thể check-in lái thử.");
        },
        afterSuccess: () => {
          backToList();
        },
      });
      return;
    }

    const payload = {
      mechanicId: checkInForm.mechanicId,
      mileageAtService: checkInForm.mileageAtService ? Number(checkInForm.mileageAtService) : null,
      vehicleCondition: checkInForm.vehicleCondition,
      notes: checkInForm.notes,
    };
    requestConfirmation({
      title: "Xác nhận check-in",
      description: "Lịch hẹn sẽ được check-in và phiếu dịch vụ sẽ được tạo từ thông tin tiếp nhận.",
      action: () => checkInServiceAppointment(selectedAppointment.id, payload),
      successMessage: "Check-in dịch vụ và tạo phiếu thành công.",
      onError: (error) => {
        setCheckInMessage(getAdvisorServiceActionError(error, "Không thể check-in dịch vụ. Vui lòng kiểm tra lại thông tin và thử lại."));
      },
      afterSuccess: () => {
        backToList();
      },
    });
  }

  async function submitSlot(event) {
    event.preventDefault();
    setSlotCreateMessage("");
    setSlotTabMessage(null);
    if (!slotForm.appointmentType || !slotForm.startTime || !slotForm.endTime) {
      setSlotCreateMessage("Vui lòng chọn loại lịch, giờ bắt đầu và giờ kết thúc.");
      return;
    }
    if (slotForm.startTime >= slotForm.endTime) {
      setSlotCreateMessage("Giờ kết thúc phải sau giờ bắt đầu.");
      return;
    }

    setActionLoading(true);
    try {
      await createAdvisorTimeSlot({ ...slotForm, active: true });
      setSlotForm({ appointmentType: "SERVICE", startTime: "", endTime: "" });
      setSlotCreateMessage("");
      setActivePanel("list");
      setSlotTabMessage({ type: "success", text: "Đã tạo khung giờ." });
      await loadBase();
    } catch (error) {
      setSlotCreateMessage(error.message || "Không thể tạo khung giờ. Vui lòng kiểm tra lại thông tin.");
    } finally {
      setActionLoading(false);
    }
  }

  async function submitHoliday(event) {
    event.preventDefault();
    setHolidayCreateMessage("");
    setHolidayTabMessage(null);
    if (!holidayForm.holidayDate) {
      setHolidayCreateMessage("Vui lòng chọn ngày nghỉ.");
      return;
    }

    setActionLoading(true);
    try {
      await createAdvisorHoliday({ ...holidayForm, active: true });
      setHolidayForm({ holidayDate: "", reason: "" });
      setHolidayCreateMessage("");
      setActivePanel("list");
      setHolidayTabMessage({ type: "success", text: "Đã tạo ngày nghỉ." });
      await loadBase();
    } catch (error) {
      setHolidayCreateMessage(error.message || "Không thể tạo ngày nghỉ. Vui lòng kiểm tra lại thông tin.");
    } finally {
      setActionLoading(false);
    }
  }

  async function submitWalkIn(event) {
    event.preventDefault();
    setWalkInMessage("");
    const validationMessage = getWalkInValidationMessage(walkInForm);
    if (validationMessage) {
      setWalkInMessage(validationMessage);
      return;
    }
    const hasUser = Boolean(walkInForm.userId);
    const payload = {
      ...walkInForm,
      mileageAtService: walkInForm.mileageAtService ? Number(walkInForm.mileageAtService) : null,
      userId: hasUser ? walkInForm.userId : null,
      guestFullName: hasUser ? "" : walkInForm.guestFullName,
      guestEmail: hasUser ? "" : walkInForm.guestEmail,
      guestPhone: hasUser ? "" : walkInForm.guestPhone,
    };
    requestConfirmation({
      title: "Xác nhận tạo phiếu dịch vụ",
      description: "Phiếu dịch vụ sẽ được tạo và phân công cho kỹ thuật viên đang chọn.",
      action: () => createWalkInServiceTicket(payload),
      successMessage: "Tạo phiếu dịch vụ trực tiếp thành công.",
      afterSuccess: () => {
        setWalkInForm(emptyWalkIn());
        setSelectedCustomer(null);
        setCustomerVehicles([]);
        closeWalkInModal();
      },
      onError: (error) => {
        setWalkInMessage(getAdvisorServiceActionError(error));
      },
    });
  }

  function openCreateAppointmentModal() {
    setAppointmentCustomerSearchMessage("");
    setCreateScheduleMessage("");
    setCreateAppointmentOpen(true);
  }

  function closeCreateAppointmentModal() {
    setAppointmentCustomerSearchMessage("");
    setCreateAppointmentOpen(false);
  }

  function openWalkInModal() {
    setWalkInCustomerSearchMessage("");
    setWalkInMessage("");
    setCreateWalkInOpen(true);
  }

  function closeWalkInModal() {
    setWalkInCustomerSearchMessage("");
    setCreateWalkInOpen(false);
  }

  function openSlotCreatePanel() {
    setSlotCreateMessage("");
    setActivePanel("slot-create");
  }

  function openHolidayCreatePanel() {
    setHolidayCreateMessage("");
    setActivePanel("holiday-create");
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

  const shouldShowAppointmentCheckIn = selectedAppointment?.status === "CONFIRMED"
    && String(selectedAppointment?.id || "") !== String(recentlyConfirmedAppointmentId || "");

  const appointmentDetail = selectedAppointment ? (
    <section className="ops-panel advisor-detail-panel advisor-detail-view wide">
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">Chi tiết lịch</p>
          <h2>{appointmentTypeLabel(selectedAppointment.type)}</h2>
        </div>
        <div className="advisor-detail-actions">
          <span className={statusPillClass(selectedAppointment.status)}>{statusLabel(selectedAppointment.status)}</span>
          <button className="advisor-detail-close" type="button" onClick={backToList} aria-label="Đóng chi tiết lịch hẹn">×</button>
        </div>
      </div>
      <dl className="summary-list compact">
        <div><dt>Mã lịch</dt><dd>{selectedAppointment.id || "Đang cập nhật"}</dd></div>
        <div><dt>Khách hàng</dt><dd>{selectedAppointment.customerFullName || "Đang cập nhật"}</dd></div>
        <div><dt>Liên hệ</dt><dd>{selectedAppointment.customerPhone || selectedAppointment.customerEmail || "Đang cập nhật"}</dd></div>
        <div><dt>VIN/Xe</dt><dd>{selectedAppointment.vinId || selectedAppointment.carVersionId || "Đang cập nhật"}</dd></div>
        <div><dt>Thời gian</dt><dd>{toDateTime(selectedAppointment) || "Đang cập nhật"}</dd></div>
      </dl>
      <div className="advisor-note-block">
        <strong>Ghi chú lịch hẹn</strong>
        <p>{selectedAppointment.notes || "Không có ghi chú."}</p>
      </div>
      {["CANCELED", "REJECTED"].includes(selectedAppointment.status) ? (
        <div className="advisor-note-block danger">
          <strong>Lý do hủy</strong>
          <p>{selectedAppointment.cancelReason || "Không có lý do hủy."}</p>
        </div>
      ) : null}

      {editSchedule ? (
        <section className="advisor-schedule-editor">
          <div className="ops-panel-head compact">
            <div>
              <p className="eyebrow">Đổi lịch</p>
              <h3>Chọn ngày làm việc và khung giờ</h3>
            </div>
            <button className="btn btn-ghost advisor-detail-button" type="button" onClick={() => setEditSchedule(false)}>Đóng</button>
          </div>
          <div className="advisor-calendar-card">
            <div className="booking-calendar-head">
              <button className="calendar-nav" type="button" onClick={() => setScheduleMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))} aria-label="Tháng trước">‹</button>
              <strong>{scheduleMonth.toLocaleDateString("vi-VN", { month: "long", year: "numeric" })}</strong>
              <button className="calendar-nav" type="button" onClick={() => setScheduleMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))} aria-label="Tháng sau">›</button>
            </div>
            <div className="booking-calendar-weekdays">{WEEKDAY_LABELS.map((label) => <span key={label}>{label}</span>)}</div>
            <div className="booking-calendar">
              {loadingScheduleDays ? <div className="status-box wide">Đang tải ngày làm việc...</div> : null}
              {!loadingScheduleDays && scheduleCells.map((cell) => {
                if (cell.blank) return <span className="calendar-blank" key={cell.key} />;
                const day = cell.meta;
                const disabled = !dayHasAvailableSlots(day);
                return (
                  <button
                    className={`calendar-day ${appointmentForm.appointmentDate === cell.date ? "selected" : ""} ${disabled ? "disabled" : ""}`}
                    disabled={disabled}
                    key={cell.key}
                    type="button"
                    onClick={() => setAppointmentForm((current) => ({ ...current, appointmentDate: cell.date, startTime: "" }))}
                  >
                    <strong>{cell.day}</strong>
                  </button>
                );
              })}
            </div>
          </div>
          <div className="advisor-slot-grid">
            {loadingScheduleSlots ? <div className="status-box wide">Đang tải khung giờ...</div> : null}
            {!loadingScheduleSlots && scheduleSlots.map((slot) => (
              <button className={`slot-button ${appointmentForm.startTime === slot.startTime ? "selected" : ""}`} key={slot.id || slot.startTime} type="button" onClick={() => setAppointmentForm((current) => ({ ...current, startTime: slot.startTime }))}>
                {slot.endTime ? `${slot.startTime} - ${slot.endTime}` : slot.startTime}
              </button>
            ))}
            {!loadingScheduleSlots && appointmentForm.appointmentDate && !scheduleSlots.length ? <div className="status-box wide">Ngày này chưa có khung giờ phù hợp.</div> : null}
          </div>
          {scheduleMessage ? <div className="advisor-detail-alert error">{scheduleMessage}</div> : null}
          <button className="btn btn-primary advisor-detail-button" type="button" disabled={actionLoading || !appointmentForm.appointmentDate || !appointmentForm.startTime} onClick={saveSchedule}>Lưu thay đổi ngày giờ</button>
        </section>
      ) : null}

      {!editSchedule && canEditAppointmentSchedule(selectedAppointment) ? (
        <div className="row-actions wrap">
          <button className="btn btn-ghost advisor-detail-button" type="button" disabled={actionLoading} onClick={() => setEditSchedule(true)}>Sửa ngày giờ</button>
        </div>
      ) : null}

      {shouldShowAppointmentCheckIn ? (
        <section className="advisor-schedule-editor">
          <div className="ops-panel-head compact">
            <div>
              <p className="eyebrow">Check-in</p>
              <h3>{selectedAppointment.type === "SERVICE" ? "Tiếp nhận xe dịch vụ" : "Xác nhận khách đã tới"}</h3>
            </div>
          </div>
          {selectedAppointment.type === "SERVICE" ? (
            <div className="form-grid compact-form-grid">
              <label className="label">Kỹ thuật viên<select className="field" value={checkInForm.mechanicId} onChange={(event) => setCheckInForm({ ...checkInForm, mechanicId: event.target.value })}><option value="">Chọn thợ</option>{mechanics.map((item) => <option key={item.id} value={item.id}>{item.fullName || item.id}</option>)}</select></label>
              <label className="label">Số km<input className="field" type="number" min="0" value={checkInForm.mileageAtService} onChange={(event) => setCheckInForm({ ...checkInForm, mileageAtService: event.target.value })} /></label>
              <label className="label wide">Tình trạng xe<textarea className="field compact-textarea" rows={3} value={checkInForm.vehicleCondition} onChange={(event) => setCheckInForm({ ...checkInForm, vehicleCondition: event.target.value })} /></label>
              <label className="label wide">Ghi chú tiếp nhận<textarea className="field compact-textarea" rows={3} value={checkInForm.notes} onChange={(event) => setCheckInForm({ ...checkInForm, notes: event.target.value })} /></label>
            </div>
          ) : null}
          {checkInMessage ? <div className="advisor-detail-alert error">{checkInMessage}</div> : null}
          <button className="btn btn-primary advisor-detail-button" type="button" disabled={actionLoading} onClick={submitCheckIn}>Check-in</button>
        </section>
      ) : null}

      {["PENDING", "CONFIRMED", "CHECKED_IN"].includes(selectedAppointment.status) ? (
        <label className="label">Lý do hủy<textarea className="field compact-textarea" rows={3} value={appointmentForm.cancelReason} onChange={(event) => setAppointmentForm({ ...appointmentForm, cancelReason: event.target.value })} /></label>
      ) : null}
      <div className="row-actions wrap">
        {selectedAppointment.status === "PENDING" ? (
          <>
            <button className="btn btn-danger advisor-detail-button" type="button" disabled={actionLoading || !appointmentForm.cancelReason.trim()} onClick={() => updateSelectedAppointment({ status: "CANCELED", cancelReason: appointmentForm.cancelReason }, "Đã hủy lịch hẹn.")}>Hủy lịch</button>
            <button className="btn btn-primary advisor-detail-button" type="button" disabled={actionLoading} onClick={() => updateSelectedAppointment({ status: "CONFIRMED" }, "Đã xác nhận lịch hẹn.")}>Xác nhận lịch</button>
          </>
        ) : null}
        {["CONFIRMED", "CHECKED_IN"].includes(selectedAppointment.status) ? (
          <button className="btn btn-danger advisor-detail-button" type="button" disabled={actionLoading || !appointmentForm.cancelReason.trim()} onClick={() => updateSelectedAppointment({ status: "CANCELED", cancelReason: appointmentForm.cancelReason }, "Đã hủy lịch hẹn.")}>Hủy lịch</button>
        ) : null}
        {selectedAppointment.type === "TEST_DRIVE" && selectedAppointment.status === "CHECKED_IN" ? (
          <button className="btn btn-primary advisor-detail-button" type="button" disabled={actionLoading} onClick={() => updateSelectedAppointment({ status: "COMPLETED" }, "Đã hoàn tất lịch lái thử.")}>Hoàn tất lái thử</button>
        ) : null}
      </div>
    </section>
  ) : (
    null
  );

  const slotCreatePanel = (
    <section className="ops-panel advisor-detail-panel advisor-detail-view advisor-slot-create-panel wide">
      <div className="ops-panel-head">
        <div><h2>Tạo khung giờ làm việc</h2></div>
        <button className="advisor-detail-close" type="button" onClick={backToList} aria-label="Đóng tạo khung giờ">×</button>
      </div>
      <form className="ops-form" onSubmit={submitSlot}>
        <div className="form-grid compact-form-grid">
          <label className="label">Loại lịch<select className="field" value={slotForm.appointmentType} onChange={(event) => { setSlotForm({ ...slotForm, appointmentType: event.target.value }); setSlotCreateMessage(""); }}><option value="SERVICE">Dịch vụ</option><option value="TEST_DRIVE">Lái thử</option></select></label>
          <label className="label">Bắt đầu<input className="field" type="time" value={slotForm.startTime} onChange={(event) => { setSlotForm({ ...slotForm, startTime: event.target.value }); setSlotCreateMessage(""); }} /></label>
          <label className="label">Kết thúc<input className="field" type="time" value={slotForm.endTime} onChange={(event) => { setSlotForm({ ...slotForm, endTime: event.target.value }); setSlotCreateMessage(""); }} /></label>
        </div>
        {slotCreateMessage ? <div className="form-alert error">{slotCreateMessage}</div> : null}
        <button className="btn btn-primary" type="submit" disabled={actionLoading}>Tạo khung giờ</button>
      </form>
    </section>
  );

  const holidayCreatePanel = (
    <section className="ops-panel advisor-detail-panel advisor-detail-view advisor-holiday-create-panel wide">
      <div className="ops-panel-head">
        <div><h2>Tạo ngày nghỉ đại lý</h2></div>
        <button className="advisor-detail-close" type="button" onClick={backToList} aria-label="Đóng tạo ngày nghỉ">×</button>
      </div>
      <form className="ops-form" onSubmit={submitHoliday}>
        <div className="form-grid compact-form-grid">
          <label className="label">Ngày nghỉ<input className="field" type="date" value={holidayForm.holidayDate} onChange={(event) => { setHolidayForm({ ...holidayForm, holidayDate: event.target.value }); setHolidayCreateMessage(""); }} /></label>
          <label className="label">Lý do<input className="field" value={holidayForm.reason} onChange={(event) => { setHolidayForm({ ...holidayForm, reason: event.target.value }); setHolidayCreateMessage(""); }} /></label>
        </div>
        {holidayCreateMessage ? <div className="form-alert error">{holidayCreateMessage}</div> : null}
        <button className="btn btn-primary" type="submit" disabled={actionLoading}>Tạo ngày nghỉ</button>
      </form>
    </section>
  );

  const advisorCreateAppointmentForm = (
    <form className="ops-form advisor-create-appointment" onSubmit={submitAdvisorAppointment}>
      <div className="form-grid compact-form-grid">
        <label className="label">Loại lịch<select className="field" value={advisorAppointmentForm.appointmentType} onChange={(event) => changeAdvisorAppointmentType(event.target.value)}><option value="SERVICE">Dịch vụ chăm sóc xe</option><option value="TEST_DRIVE">Buổi hẹn lái thử</option></select></label>
        <label className="label">Khách hàng<select className="field" value={advisorAppointmentForm.customerMode} onChange={(event) => changeAdvisorCustomerMode(event.target.value)}><option value="guest">Khách vãng lai</option><option value="user">Khách có tài khoản</option></select></label>
      </div>

      {advisorAppointmentForm.customerMode === "user" ? (
        <>
          <div className="advisor-customer-search">
            <div className="search-row">
              <input className="field" value={customerKeyword} onChange={(event) => changeCustomerKeyword(event.target.value)} placeholder={CUSTOMER_SEARCH_PLACEHOLDER} />
              <button className="btn btn-ghost advisor-create-button secondary" type="button" onClick={searchCustomers}>Tìm</button>
            </div>
            {appointmentCustomerSearchMessage ? <div className="form-alert error advisor-inline-alert">{appointmentCustomerSearchMessage}</div> : null}
          </div>
          <small className="form-hint">{CUSTOMER_SEARCH_HINT}</small>
          {selectedCustomer ? (
            <div className="row-actions wrap">
              <strong>Khách đang chọn: {selectedCustomer.fullName || selectedCustomer.email}</strong>
              <button className="btn btn-ghost compact-action advisor-create-button secondary" type="button" onClick={clearSelectedCustomer}>Gỡ khách đã chọn</button>
            </div>
          ) : null}
          <div className="chip-row">{customers.map((item) => <button className={advisorAppointmentForm.userId === item.id ? "selected" : ""} key={item.id} type="button" onClick={() => chooseCustomer(item)}>{item.fullName} · {item.phone || item.email}</button>)}</div>
        </>
      ) : (
        <div className="form-grid compact-form-grid">
          <label className="label">Họ tên<input className="field" value={advisorAppointmentForm.guestFullName} onChange={(event) => setAdvisorAppointmentForm({ ...advisorAppointmentForm, guestFullName: event.target.value })} /></label>
          <label className="label">Email<input className="field" type="email" value={advisorAppointmentForm.guestEmail} onChange={(event) => setAdvisorAppointmentForm({ ...advisorAppointmentForm, guestEmail: event.target.value })} /></label>
          <label className="label">Số điện thoại<input className="field" value={advisorAppointmentForm.guestPhone} onChange={(event) => setAdvisorAppointmentForm({ ...advisorAppointmentForm, guestPhone: event.target.value })} /></label>
        </div>
      )}

      <div className="form-grid compact-form-grid">
        {advisorAppointmentForm.appointmentType === "SERVICE" ? (
          advisorAppointmentForm.customerMode === "user" && advisorAppointmentForm.userId ? (
            <label className="label">VIN<select className="field" value={advisorAppointmentForm.vinId} onChange={(event) => setAdvisorAppointmentForm({ ...advisorAppointmentForm, vinId: event.target.value })}><option value="">Chọn VIN của khách</option>{customerVehicles.map((item) => <option key={item.vinId} value={item.vinId}>{item.vinId}{item.carVersionName ? ` · ${item.carVersionName}` : ""}</option>)}</select></label>
          ) : (
            <label className="label">VIN<input className="field" maxLength={17} value={advisorAppointmentForm.vinId} onChange={(event) => setAdvisorAppointmentForm({ ...advisorAppointmentForm, vinId: event.target.value.toUpperCase() })} /></label>
          )
        ) : (
          <label className="label">Phiên bản xe<select className="field" value={advisorAppointmentForm.carVersionId} onChange={(event) => setAdvisorAppointmentForm({ ...advisorAppointmentForm, carVersionId: event.target.value })}><option value="">Chọn phiên bản</option>{carVersions.map((item) => <option key={item.id || item.carVersionId} value={item.id || item.carVersionId}>{item.versionName || item.name || item.carVersionName || item.id}</option>)}</select></label>
        )}
        <label className="label wide">Ghi chú<textarea className="field compact-textarea" rows={3} value={advisorAppointmentForm.notes} onChange={(event) => setAdvisorAppointmentForm({ ...advisorAppointmentForm, notes: event.target.value })} /></label>
      </div>

      <div className="advisor-calendar-card">
        <div className="booking-calendar-head">
          <button className="calendar-nav" type="button" onClick={() => setCreateScheduleMonth((current) => new Date(current.getFullYear(), current.getMonth() - 1, 1))} aria-label="Tháng trước">‹</button>
          <strong>{createScheduleMonth.toLocaleDateString("vi-VN", { month: "long", year: "numeric" })}</strong>
          <button className="calendar-nav" type="button" onClick={() => setCreateScheduleMonth((current) => new Date(current.getFullYear(), current.getMonth() + 1, 1))} aria-label="Tháng sau">›</button>
        </div>
        <div className="booking-calendar-weekdays">{WEEKDAY_LABELS.map((label) => <span key={label}>{label}</span>)}</div>
        <div className="booking-calendar">
          {loadingCreateScheduleDays ? <div className="status-box wide">Đang tải ngày làm việc...</div> : null}
          {!loadingCreateScheduleDays && createScheduleCells.map((cell) => {
            if (cell.blank) return <span className="calendar-blank" key={cell.key} />;
            const day = cell.meta;
            const disabled = !dayHasAvailableSlots(day);
            return (
              <button className={`calendar-day ${advisorAppointmentForm.appointmentDate === cell.date ? "selected" : ""} ${disabled ? "disabled" : ""}`} disabled={disabled} key={cell.key} type="button" onClick={() => setAdvisorAppointmentForm((current) => ({ ...current, appointmentDate: cell.date, startTime: "" }))}>
                <strong>{cell.day}</strong>
              </button>
            );
          })}
        </div>
      </div>

      <div className="advisor-slot-grid">
        {loadingCreateScheduleSlots ? <div className="status-box wide">Đang tải khung giờ...</div> : null}
        {!loadingCreateScheduleSlots && createScheduleSlots.map((slot) => (
          <button className={`slot-button ${advisorAppointmentForm.startTime === slot.startTime ? "selected" : ""}`} key={slot.id || slot.startTime} type="button" onClick={() => setAdvisorAppointmentForm((current) => ({ ...current, startTime: slot.startTime }))}>
            {slot.endTime ? `${slot.startTime} - ${slot.endTime}` : slot.startTime}
          </button>
        ))}
      </div>
      {createScheduleMessage ? <div className="form-alert error">{createScheduleMessage}</div> : null}
      <button className="btn btn-primary advisor-create-button" type="submit" disabled={actionLoading}>Tạo lịch đã xác nhận</button>
    </form>
  );

  const walkInServiceForm = (
    <form className="ops-form" onSubmit={submitWalkIn}>
      <div className="advisor-customer-search">
        <div className="search-row">
          <input className="field" value={customerKeyword} onChange={(event) => changeCustomerKeyword(event.target.value)} placeholder={CUSTOMER_SEARCH_PLACEHOLDER} />
          <button className="btn btn-ghost advisor-create-button secondary" type="button" onClick={searchCustomers} disabled={actionLoading}>Tìm</button>
        </div>
        {walkInCustomerSearchMessage ? <div className="form-alert error advisor-inline-alert">{walkInCustomerSearchMessage}</div> : null}
      </div>
      <small className="form-hint">{CUSTOMER_SEARCH_HINT}</small>
      {selectedCustomer && walkInForm.userId ? (
        <div className="row-actions wrap">
          <strong>Khách đang chọn: {selectedCustomer.fullName || selectedCustomer.email}</strong>
          <button className="btn btn-ghost compact-action advisor-create-button secondary" type="button" onClick={clearSelectedCustomer}>Gỡ khách đã chọn</button>
        </div>
      ) : null}
      <div className="chip-row">{customers.map((item) => <button className={selectedCustomer?.id === item.id ? "selected" : ""} key={item.id} type="button" onClick={() => chooseCustomer(item)}>{item.fullName} · {item.phone || item.email}</button>)}</div>
      <div className="form-grid">
        {!walkInForm.userId ? (
          <>
            <label className="label">Họ tên<input className="field" value={walkInForm.guestFullName} onChange={(event) => setWalkInForm({ ...walkInForm, guestFullName: event.target.value })} /></label>
            <label className="label">Email<input className="field" type="email" value={walkInForm.guestEmail} onChange={(event) => setWalkInForm({ ...walkInForm, guestEmail: event.target.value })} /></label>
            <label className="label">Số điện thoại<input className="field" value={walkInForm.guestPhone} onChange={(event) => setWalkInForm({ ...walkInForm, guestPhone: event.target.value })} /></label>
          </>
        ) : null}
        {walkInForm.userId ? (
          <label className="label">VIN<select className="field" value={walkInForm.vinId} onChange={(event) => setWalkInForm({ ...walkInForm, vinId: event.target.value })}><option value="">Chọn VIN của khách</option>{customerVehicles.map((item) => <option key={item.vinId} value={item.vinId}>{item.vinId}{item.carVersionName ? ` · ${item.carVersionName}` : ""}</option>)}</select></label>
        ) : (
          <label className="label">VIN<input className="field" maxLength={17} value={walkInForm.vinId} onChange={(event) => setWalkInForm({ ...walkInForm, vinId: event.target.value.toUpperCase() })} /></label>
        )}
        <label className="label">Kỹ thuật viên<select className="field" value={walkInForm.mechanicId} onChange={(event) => setWalkInForm({ ...walkInForm, mechanicId: event.target.value })}><option value="">Chọn thợ</option>{mechanics.map((item) => <option key={item.id} value={item.id}>{item.fullName || item.id}</option>)}</select></label>
        <label className="label">Số km<input className="field" type="number" min="0" value={walkInForm.mileageAtService} onChange={(event) => setWalkInForm({ ...walkInForm, mileageAtService: event.target.value })} /></label>
        <label className="label wide">Tình trạng xe<textarea className="field" rows={3} value={walkInForm.vehicleCondition} onChange={(event) => setWalkInForm({ ...walkInForm, vehicleCondition: event.target.value })} /></label>
        <label className="label wide">Ghi chú<textarea className="field" rows={3} value={walkInForm.notes} onChange={(event) => setWalkInForm({ ...walkInForm, notes: event.target.value })} /></label>
      </div>
      {walkInMessage ? <div className="form-alert error">{walkInMessage}</div> : null}
      <button className="btn btn-primary advisor-create-button" type="submit" disabled={actionLoading}>Tạo phiếu dịch vụ</button>
    </form>
  );

  const ticketDetail = selectedTicket ? (() => {
    const ticket = selectedTicket.serviceTicket || selectedTicket;
    const invoice = selectedTicket.invoice || {};
    const items = invoice.items || selectedTicket.items || [];
    const totalAmount = invoice.totalAmount ?? ticket.totalAmount ?? 0;
    const cancelableTicket = canCancelServiceTicket(ticket);
    const canceledTicket = String(ticket.status || "").toUpperCase() === "CANCELED";

    return (
      <section className="ops-panel advisor-detail-panel advisor-detail-view wide advisor-service-detail-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Chi tiết phiếu</p>
            <h2>{getAdvisorTicketTitle({ ...ticket, invoice })}</h2>
          </div>
          <div className="advisor-detail-actions">
            <span className={statusPillClass(ticket.status)}>{statusLabel(ticket.status)}</span>
            <button className="advisor-detail-close" type="button" onClick={backToList} aria-label="Đóng chi tiết phiếu dịch vụ">×</button>
          </div>
        </div>
        <div className="service-total-card">
          <span>Tổng tiền dịch vụ</span>
          <strong>{formatVndZero(totalAmount)}</strong>
        </div>
        <dl className="summary-list compact">
          <div><dt>Mã phiếu</dt><dd>{ticket.id || "Đang cập nhật"}</dd></div>
          <div><dt>Khách hàng</dt><dd>{ticket.customerFullName || "Đang cập nhật"}</dd></div>
          <div><dt>VIN</dt><dd>{ticket.vinId || "Không có"}</dd></div>
          <div><dt>Đại lý</dt><dd>{getAdvisorDealershipLabel({ ...ticket, invoice })}</dd></div>
          <div><dt>Kỹ thuật viên</dt><dd>{invoice.mechanicName || mechanicNameById.get(String(ticket.mechanicId)) || ticket.mechanicId || "Đang cập nhật"}</dd></div>
          <div><dt>Số km</dt><dd>{ticket.mileageAtService ? `${ticket.mileageAtService} km` : "Chưa cập nhật"}</dd></div>
          <div><dt>Tiếp nhận</dt><dd>{serviceReceivedAtLabel(ticket)}</dd></div>
          <div><dt>Bắt đầu sửa</dt><dd>{serviceProcessingAtLabel(ticket)}</dd></div>
          <div><dt>Hoàn tất</dt><dd>{serviceCompletedAtLabel(ticket)}</dd></div>
          {ticket.canceledAt ? <div><dt>Đã hủy</dt><dd>{formatAdvisorDateTime(ticket.canceledAt)}</dd></div> : null}
          <div><dt>Tình trạng xe</dt><dd>{ticket.vehicleCondition || invoice.vehicleCondition || "Chưa cập nhật"}</dd></div>
          <div><dt>Ghi chú</dt><dd>{formatServiceTicketNote(ticket.notes) || "Không có"}</dd></div>
          {canceledTicket && ticket.cancelReason ? <div><dt>Lý do hủy</dt><dd>{ticket.cancelReason}</dd></div> : null}
        </dl>
        {cancelableTicket ? (
          <section className="advisor-schedule-editor advisor-ticket-cancel-panel">
            <div className="ops-panel-head compact">
              <div><p className="eyebrow">Hủy phiếu dịch vụ</p><h3>Áp dụng khi kỹ thuật viên chưa tiếp nhận</h3></div>
            </div>
            <label className="label">Lý do hủy<textarea className="field compact-textarea" rows={3} value={ticketCancelReason} onChange={(event) => { setTicketCancelReason(event.target.value); setTicketCancelMessage(""); }} /></label>
            <div className="advisor-ticket-cancel-actions">
              <button className="btn btn-danger advisor-detail-button" type="button" disabled={actionLoading || !ticketCancelReason.trim()} onClick={cancelServiceTicket}>Hủy phiếu dịch vụ</button>
              {ticketCancelMessage ? <div className="form-alert error advisor-ticket-cancel-alert">{ticketCancelMessage}</div> : null}
            </div>
          </section>
        ) : null}
        {ticket.status === "NEEDS_REASSIGNMENT" ? (
          <div className="form-grid compact-form-grid">
            <label className="label">Kỹ thuật viên mới<select className="field" value={reassignMechanicId} onChange={(event) => { setReassignMechanicId(event.target.value); setReassignMessage(""); }}><option value="">Chọn thợ</option>{mechanics.map((item) => <option key={item.id} value={item.id}>{item.fullName || item.id}</option>)}</select></label>
            <div className="label advisor-reassign-action"><span>&nbsp;</span><button className="btn btn-primary" type="button" disabled={actionLoading} onClick={reassignTicket}>Phân công lại</button>{reassignMessage ? <div className="form-alert error advisor-reassign-alert">{reassignMessage}</div> : null}</div>
          </div>
        ) : null}
        <ServiceBreakdown items={items} formatCurrency={formatVndZero} idPrefix="advisor-service" />
      </section>
    );
  })() : (
    null
  );

  return (
    <div className="ops-grid workspace-tabs-layout advisor-workspace">
      {message && tab !== "slots" && tab !== "holidays" ? <div className="status-box wide">{message}</div> : null}
      {loading ? <div className="status-box wide">Đang tải dữ liệu vận hành...</div> : null}
      {confirmDialog ? (
        <div className="detail-modal-backdrop advisor-confirm-backdrop" role="presentation" onClick={() => !actionLoading && setConfirmDialog(null)}>
          <section className="detail-modal advisor-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="advisor-confirm-title" onClick={(event) => event.stopPropagation()}>
            <div className="detail-modal-head">
              <div>
                <h2 id="advisor-confirm-title">{confirmDialog.title}</h2>
                <span>{confirmDialog.description}</span>
              </div>
            </div>
            <div className="detail-modal-body">
              {confirmMessage ? <div className="advisor-detail-alert error">{confirmMessage}</div> : null}
              <div className="row-actions wrap advisor-confirm-actions">
                <button className="btn btn-ghost" type="button" disabled={actionLoading} onClick={() => setConfirmDialog(null)}>Hủy</button>
                <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={confirmCurrentAction}>OK</button>
              </div>
            </div>
          </section>
        </div>
      ) : null}

      {tab === "profile" ? (
        <ProfilePanel heading="Hồ sơ cá nhân" />
      ) : null}

      {tab === "overview" ? (
        <section className="ops-panel wide advisor-overview-panel">
          <div className="ops-panel-head advisor-overview-head">
            <div>
              <h2>Tổng quan hôm nay</h2>
            </div>
            <span className="advisor-overview-date">{overviewDateLabel}</span>
          </div>
          <div className="advisor-overview-grid">
            <article className="advisor-overview-card tone-info">
              <header>
                <span className="advisor-overview-label">Số lịch hẹn</span>
                <strong>{overviewStats.appointmentsToday}</strong>
              </header>
              <p>Tất cả lịch đặt trong ngày</p>
            </article>
            <article className="advisor-overview-card tone-warning">
              <header>
                <span className="advisor-overview-label">Lịch đang chờ xác nhận</span>
                <strong>{overviewStats.pendingAppointments}</strong>
              </header>
              <p>Cần xử lý trước giờ hẹn</p>
            </article>
            <article className="advisor-overview-card tone-success">
              <header>
                <span className="advisor-overview-label">Khách đã check in</span>
                <strong>{overviewStats.checkedInCustomers}</strong>
              </header>
              <p>Khách đã đến đại lý</p>
            </article>
            <article className="advisor-overview-card tone-progress">
              <header>
                <span className="advisor-overview-label">Dịch vụ chăm sóc xe đang được xử lý</span>
                <strong>{overviewStats.activeServiceTickets}</strong>
              </header>
              <p>Phiếu chăm sóc xe đang tiếp nhận hoặc xử lý</p>
            </article>
            <article className="advisor-overview-card tone-complete">
              <header>
                <span className="advisor-overview-label">Buổi hẹn lái thử đã hoàn tất</span>
                <strong>{overviewStats.completedTestDrives}</strong>
              </header>
              <p>Lịch lái thử đã hoàn thành</p>
            </article>
            <article className="advisor-overview-card tone-done">
              <header>
                <span className="advisor-overview-label">Dịch vụ chăm sóc xe đã hoàn tất</span>
                <strong>{overviewStats.completedServices}</strong>
              </header>
              <p>Dịch vụ chăm sóc xe đã hoàn thành</p>
            </article>
          </div>
        </section>
      ) : null}

      {tab === "appointments" ? (
        activePanel === "appointment-detail" ? appointmentDetail : (
          <>
            <section className="ops-panel advisor-list-panel advisor-full-panel advisor-appointment-panel wide">
              <div className="ops-panel-head advisor-appointment-head">
                <div><h2>Quản lý lịch hẹn</h2></div>
                <button className="btn btn-primary advisor-create-trigger" type="button" onClick={openCreateAppointmentModal}>Tạo lịch hẹn</button>
              </div>
              <div className="advisor-filter-stack">
                <div className="advisor-filter-group">
                  <label htmlFor="advisor-appointment-filter">Trạng thái</label>
                  <select id="advisor-appointment-filter" className="advisor-filter-select" value={appointmentListFilter} onChange={(event) => setAppointmentListFilter(event.target.value)}>
                    {APPOINTMENT_LIST_FILTERS.map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                </div>
                <div className="advisor-filter-group">
                  <label htmlFor="advisor-appointment-type">Loại lịch</label>
                  <select id="advisor-appointment-type" className="advisor-filter-select" value={appointmentTypeFilter} onChange={(event) => setAppointmentTypeFilter(event.target.value)}>
                    {APPOINTMENT_TYPE_FILTERS.map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="advisor-row-list advisor-appointment-list">
                {pagedAppointments.items.map((item) => (
                  <article className={`advisor-row advisor-appointment-row ${selectedAppointment?.id === item.id ? "active" : ""}`} key={item.id}>
                    <div className="advisor-row-main advisor-appointment-person">
                      <span className={`advisor-row-kicker ${appointmentTypeClass(item.type)}`}>{appointmentTypeLabel(item.type)}</span>
                      <strong>{item.customerFullName || "Khách hàng"}</strong>
                      <span>Mã lịch: {item.id || "Đang cập nhật"}</span>
                    </div>
                    <div className="advisor-row-field">
                      <span className="advisor-row-field-label">Thời gian</span>
                      <span className="advisor-row-time">{toDateTime(item) || "Chưa có thời gian"}</span>
                    </div>
                    <div className="advisor-row-field">
                      <span className="advisor-row-field-label">Trạng thái</span>
                      <span className={statusPillClass(item.status)}>{statusLabel(item.status)}</span>
                    </div>
                    <div className="advisor-row-actions">
                      <button className="advisor-detail-link" type="button" disabled={actionLoading} onClick={() => openAppointment(item.id)}>Chi tiết</button>
                    </div>
                  </article>
                ))}
                {!pagedAppointments.items.length && !loading ? (
                  <div className="status-box">
                    {appointmentListFilter === "PENDING_REVIEW"
                      ? "Không có lịch chờ xác nhận trong bộ lọc này."
                      : appointmentListFilter === "UPCOMING"
                        ? "Không có lịch hẹn sắp tới trong bộ lọc này."
                        : appointmentListFilter === "TODAY"
                          ? "Không có lịch hẹn hôm nay trong bộ lọc này."
                          : "Không có lịch hẹn trong bộ lọc này."}
                  </div>
                ) : null}
              </div>
              <Pagination page={pagedAppointments.page} totalPages={pagedAppointments.totalPages} totalItems={sortedAppointments.length} start={pagedAppointments.start} count={pagedAppointments.items.length} onChange={setAppointmentPage} />
            </section>
            {createAppointmentOpen ? (
              <div className="detail-modal-backdrop advisor-create-modal-backdrop" role="presentation" onClick={closeCreateAppointmentModal}>
                <section className="detail-modal advisor-create-modal" role="dialog" aria-modal="true" aria-labelledby="advisor-create-appointment-title" onClick={(event) => event.stopPropagation()}>
                  <div className="detail-modal-head advisor-create-modal-head">
                    <div>
                      <h2 id="advisor-create-appointment-title">Tạo lịch hẹn</h2>
                      <span>Chọn khách hàng, ngày làm việc và khung giờ còn trống.</span>
                    </div>
                    <button className="advisor-detail-close" type="button" onClick={closeCreateAppointmentModal} aria-label="Đóng tạo lịch hẹn">×</button>
                  </div>
                  <div className="detail-modal-body advisor-create-modal-body">
                    {advisorCreateAppointmentForm}
                  </div>
                </section>
              </div>
            ) : null}
          </>
        )
      ) : null}

      {tab === "vehicles" ? (
        <section className="ops-panel wide">
          <h2>Gán VIN vào tài khoản khách</h2>
          <div className="advisor-customer-search">
            <div className="search-row">
              <input className="field" value={customerKeyword} onChange={(event) => changeCustomerKeyword(event.target.value)} placeholder={CUSTOMER_SEARCH_PLACEHOLDER} />
              <button className="btn btn-ghost" type="button" onClick={searchCustomers} disabled={actionLoading}>Tìm</button>
            </div>
            {vehicleCustomerSearchMessage ? <div className="form-alert error advisor-inline-alert">{vehicleCustomerSearchMessage}</div> : null}
          </div>
          <small className="form-hint">{CUSTOMER_SEARCH_HINT}</small>
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
        activePanel === "slot-create" ? slotCreatePanel : (
          <section className="ops-panel advisor-list-panel advisor-full-panel advisor-slot-panel wide">
            <div className="ops-panel-head advisor-appointment-head">
              <div><h2>Khung giờ làm việc</h2></div>
              <button className="btn btn-primary advisor-create-trigger" type="button" onClick={openSlotCreatePanel}>Tạo khung giờ</button>
            </div>
            <div className="advisor-filter-stack">
              <div className="advisor-filter-group">
                <label htmlFor="advisor-slot-type">Loại lịch</label>
                <select id="advisor-slot-type" className="advisor-filter-select" value={slotTypeFilter} onChange={(event) => setSlotTypeFilter(event.target.value)}>
                  {SLOT_TYPE_FILTERS.map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </div>
            </div>
            {slotTabMessage ? <div className={`advisor-tab-alert ${slotTabMessage.type}`}>{slotTabMessage.text}</div> : null}
            <div className="advisor-row-list">
              {filteredSlots.map((slot) => {
                const isServiceSlot = slot.appointmentType === "SERVICE";
                const slotVisual = isServiceSlot
                  ? {
                    accent: "#0f766e",
                    badgeBackground: "#ccfbf1",
                    border: "#5eead4",
                    className: "advisor-slot-row-service",
                    description: "Áp dụng cho lịch dịch vụ",
                    rowBackground: "linear-gradient(135deg, #ecfdf5 0%, #f0fdfa 58%, #ffffff 100%)",
                    title: "Khung giờ dịch vụ",
                  }
                  : {
                    accent: "#2563eb",
                    badgeBackground: "#dbeafe",
                    border: "#93c5fd",
                    className: "advisor-slot-row-test-drive",
                    description: "Áp dụng cho lịch lái thử",
                    rowBackground: "linear-gradient(135deg, #eff6ff 0%, #f8fbff 58%, #ffffff 100%)",
                    title: "Khung giờ lái thử",
                  };

                return (
                  <article
                    className={`advisor-row advisor-schedule-row advisor-slot-row ${slotVisual.className}`}
                    key={slot.id}
                    style={{
                      "--slot-accent": slotVisual.accent,
                      "--slot-border": slotVisual.border,
                      "--slot-row-bg": slotVisual.rowBackground,
                      "--slot-title-bg": slotVisual.badgeBackground,
                      "--slot-title-color": slotVisual.accent,
                      background: slotVisual.rowBackground,
                      borderColor: slotVisual.border,
                      borderLeft: `5px solid ${slotVisual.accent}`,
                    }}
                  >
                    <div className="advisor-row-main advisor-slot-copy">
                      <strong
                        className="advisor-slot-type-badge"
                        style={{
                          background: slotVisual.badgeBackground,
                          borderColor: slotVisual.border,
                          color: slotVisual.accent,
                        }}
                      >
                        {slotVisual.title}
                      </strong>
                      <span className="advisor-slot-description" style={{ color: slotVisual.accent }}>{slotVisual.description}</span>
                    </div>
                    <div className="advisor-row-field"><span className="advisor-row-field-label">Thời gian</span><span className="advisor-row-time">{slot.startTime} - {slot.endTime}</span></div>
                    <span className={`status-pill ${slot.active === false ? "status-neutral" : "status-success"}`}>{slot.active === false ? "Đã tắt" : "Đang mở"}</span>
                    <div className="advisor-row-actions">
                      <button className="btn btn-ghost compact-action" type="button" disabled={actionLoading} onClick={() => toggleTimeSlot(slot)}>{slot.active === false ? "Bật lại" : "Tắt"}</button>
                      <button className="btn btn-danger compact-action" type="button" disabled={actionLoading} onClick={() => removeTimeSlot(slot)}>Xóa</button>
                    </div>
                  </article>
                );
              })}
              {!filteredSlots.length && !loading ? <div className="status-box">Chưa có khung giờ trong bộ lọc này.</div> : null}
            </div>
          </section>
        )
      ) : null}

      {tab === "holidays" ? (
        activePanel === "holiday-create" ? holidayCreatePanel : (
          <section className="ops-panel advisor-list-panel advisor-full-panel advisor-holiday-panel wide">
            <div className="ops-panel-head advisor-appointment-head">
              <div><h2>Ngày nghỉ đại lý</h2></div>
              <button className="btn btn-primary advisor-create-trigger" type="button" onClick={openHolidayCreatePanel}>Tạo ngày nghỉ</button>
            </div>
            {holidayTabMessage ? <div className={`advisor-tab-alert ${holidayTabMessage.type}`}>{holidayTabMessage.text}</div> : null}
            <div className="advisor-row-list">
              {sortedHolidays.map((holiday) => (
                <article className="advisor-row advisor-schedule-row advisor-holiday-row" key={holiday.id}>
                  <div className="advisor-row-main"><strong>{holiday.holidayDate}</strong><span>{holiday.reason || "Ngày nghỉ đại lý"}</span></div>
                  <div className="advisor-row-field"><span className="advisor-row-field-label">Tác động</span><span className="advisor-row-time">{holiday.active === false ? "Không chặn đặt lịch" : "Đang chặn đặt lịch"}</span></div>
                  <span className={`status-pill ${holiday.active === false ? "status-neutral" : "status-warning"}`}>{holiday.active === false ? "Đã tắt" : "Đang áp dụng"}</span>
                  <div className="advisor-row-actions">
                    <button className="btn btn-ghost compact-action" type="button" disabled={actionLoading} onClick={() => toggleHoliday(holiday)}>{holiday.active === false ? "Bật lại" : "Tắt"}</button>
                    <button className="btn btn-danger compact-action" type="button" disabled={actionLoading} onClick={() => removeHoliday(holiday)}>Xóa</button>
                  </div>
                </article>
              ))}
              {!holidays.length && !loading ? <div className="status-box">Chưa có ngày nghỉ.</div> : null}
            </div>
          </section>
        )
      ) : null}

      {tab === "tickets" ? (
        activePanel === "ticket-detail" ? ticketDetail : (
          <>
            <section className="ops-panel advisor-list-panel advisor-full-panel advisor-ticket-panel wide">
              <div className="ops-panel-head advisor-appointment-head">
                <div><h2>Phiếu dịch vụ đại lý</h2></div>
                <button className="btn btn-primary advisor-create-trigger" type="button" onClick={openWalkInModal}>Tạo phiếu dịch vụ</button>
              </div>
              <div className="advisor-filter-stack">
                <div className="advisor-filter-group">
                  <label htmlFor="advisor-ticket-status">Trạng thái</label>
                  <select id="advisor-ticket-status" className="advisor-filter-select" value={ticketStatus} onChange={(event) => setTicketStatus(event.target.value)}>
                    {TICKET_STATUS_FILTERS.map(([value, label]) => (
                      <option key={value} value={value}>{label}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="advisor-row-list advisor-ticket-list">
                {pagedTickets.items.map((ticket) => (
                  <article className={`advisor-row advisor-ticket-row ${selectedTicket?.serviceTicket?.id === ticket.id ? "active" : ""}`} key={ticket.id}>
                    <div className="advisor-row-main advisor-appointment-person">
                      <span className="advisor-row-kicker advisor-ticket-badge">Dịch vụ chăm sóc xe</span>
                      <strong>{ticket.customerFullName || "Khách vãng lai"}</strong>
                      <span>Mã phiếu: {ticket.id || "Đang cập nhật"}</span>
                    </div>
                    <div className="advisor-row-field">
                      <span className="advisor-row-field-label">Cập nhật</span>
                      <span className="advisor-row-time">{formatAdvisorDateTime(serviceTimeLabel(ticket)) || "Chưa cập nhật"}</span>
                    </div>
                    <div className="advisor-row-field">
                      <span className="advisor-row-field-label">Tổng tiền</span>
                      <span className="advisor-row-time">{serviceTicketAmountLabel(ticket)}</span>
                    </div>
                    <div className="advisor-row-field">
                      <span className="advisor-row-field-label">Trạng thái</span>
                      <span className={statusPillClass(ticket.status)}>{statusLabel(ticket.status)}</span>
                    </div>
                    <div className="advisor-row-actions">
                      <button className="advisor-detail-link" type="button" disabled={actionLoading} onClick={() => openTicket(ticket.id)}>Chi tiết</button>
                    </div>
                  </article>
                ))}
                {!pagedTickets.items.length && !loading ? <div className="status-box">Không có phiếu dịch vụ trong bộ lọc này.</div> : null}
              </div>
              <Pagination page={pagedTickets.page} totalPages={pagedTickets.totalPages} totalItems={tickets.length} start={pagedTickets.start} count={pagedTickets.items.length} onChange={setTicketPage} />
            </section>
            {createWalkInOpen ? (
              <div className="detail-modal-backdrop advisor-create-modal-backdrop" role="presentation" onClick={closeWalkInModal}>
                <section className="detail-modal advisor-create-modal" role="dialog" aria-modal="true" aria-labelledby="advisor-create-walkin-title" onClick={(event) => event.stopPropagation()}>
                  <div className="detail-modal-head advisor-create-modal-head">
                    <div>
                      <h2 id="advisor-create-walkin-title">Tạo phiếu dịch vụ</h2>
                    </div>
                    <button className="advisor-detail-close" type="button" onClick={closeWalkInModal} aria-label="Đóng tạo phiếu dịch vụ">×</button>
                  </div>
                  <div className="detail-modal-body advisor-create-modal-body">
                    {walkInServiceForm}
                  </div>
                </section>
              </div>
            ) : null}
          </>
        )
      ) : null}

      {tab === "reports" ? (
        <section className="ops-panel advisor-report-panel wide">
          <div className="ops-panel-head advisor-appointment-head">
            <div><h2>Báo cáo đại lý</h2></div>
          </div>
          <div className="advisor-report-filters">
            <label className="advisor-filter-group">
              <span>Khoảng thời gian</span>
              <select className="advisor-filter-select" value={reportRange} onChange={(event) => setReportRange(event.target.value)}>
                {REPORT_RANGE_FILTERS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            <label className="advisor-filter-group">
              <span>Loại lịch</span>
              <select className="advisor-filter-select" value={reportType} onChange={(event) => setReportType(event.target.value)}>
                {REPORT_TYPE_FILTERS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </label>
            {reportRange === "CUSTOM" ? (
              <>
                <label className="advisor-filter-group">
                  <span>Từ ngày</span>
                  <ReportDateInput label="Từ ngày" value={reportCustomFrom} onChange={setReportCustomFrom} />
                </label>
                <label className="advisor-filter-group">
                  <span>Đến ngày</span>
                  <ReportDateInput label="Đến ngày" value={reportCustomTo} onChange={setReportCustomTo} />
                </label>
              </>
            ) : null}
          </div>
          {reportMessage ? <div className="advisor-tab-alert error">{reportMessage}</div> : null}
          {reportLoading ? <div className="advisor-tab-alert success">Đang tải báo cáo đại lý...</div> : null}
          <div className="advisor-report-summary-stack">
            <section className="advisor-report-summary-section">
              <div className="advisor-report-summary-grid advisor-report-summary-primary">
                <article><span>Tổng số lịch hẹn</span><strong>{advisorReport?.summary?.totalAppointments ?? 0}</strong></article>
                <article><span>Số lịch đã hoàn tất</span><strong>{advisorReport?.summary?.completedAppointments ?? 0}</strong></article>
                <article><span>Lịch bị hủy hoặc quá hạn</span><strong>{advisorReport?.summary?.canceledOrExpiredAppointments ?? 0}</strong></article>
              </div>
            </section>
            <section className="advisor-report-summary-section advisor-report-summary-section-secondary">
              <div className="advisor-report-summary-grid advisor-report-summary-secondary">
                <article><span>Phiếu dịch vụ</span><strong>{advisorReport?.summary?.serviceTickets ?? 0}</strong></article>
                <article><span>Doanh thu từ phiếu dịch vụ hoàn tất</span><strong>{formatVndZero(advisorReport?.summary?.completedServiceRevenue)}</strong></article>
                <article><span>Điểm đánh giá trung bình của khách</span><strong>{ratingLabel(advisorReport?.summary?.averageRating)}</strong></article>
              </div>
            </section>
          </div>
          <div className="advisor-report-chart-grid">
            <ReportDonutChart title="Trạng thái lịch hẹn" items={advisorReport?.appointmentStatus || []} />
            <ReportDonutChart title="Trạng thái phiếu dịch vụ" items={advisorReport?.serviceTicketStatus || []} />
            <RevenueChart items={advisorReport?.revenueByDay || []} />
            <RatingDistributionChart items={advisorReport?.ratingDistribution || []} />
          </div>
          <div className="advisor-report-tables">
            <section className="advisor-report-table-card">
              <div className="advisor-report-table-head"><h3>Phiếu dịch vụ hoàn tất gần đây</h3></div>
              <div className="advisor-report-table">
                <div className="advisor-report-table-header ticket">
                  <span>Mã phiếu</span>
                  <span>Khách hàng</span>
                  <span>Xe</span>
                  <span>Kỹ thuật viên</span>
                  <span>Tổng tiền</span>
                  <span>Ngày hoàn tất</span>
                </div>
                {(advisorReport?.recentCompletedServiceTickets || []).map((ticket) => (
                  <article className="advisor-report-table-row ticket" key={ticket.id}>
                    <strong className="advisor-report-cell" data-label="Mã phiếu">{ticket.id}</strong>
                    <span className="advisor-report-cell" data-label="Khách hàng">{ticket.customerFullName || "Khách hàng"}</span>
                    <span className="advisor-report-cell" data-label="Xe">{ticket.vehicle || "Đang cập nhật"}</span>
                    <span className="advisor-report-cell" data-label="Kỹ thuật viên">{ticket.mechanicName || ticket.mechanicId || "Chưa có thợ"}</span>
                    <span className="advisor-report-cell" data-label="Tổng tiền">{formatVndZero(ticket.totalAmount)}</span>
                    <span className="advisor-report-cell" data-label="Ngày hoàn tất">{formatAdvisorDateTime(ticket.completedAt)}</span>
                  </article>
                ))}
                {!(advisorReport?.recentCompletedServiceTickets || []).length ? <div className="advisor-report-empty">Chưa có phiếu dịch vụ hoàn tất trong bộ lọc này.</div> : null}
              </div>
            </section>
            <section className="advisor-report-table-card">
              <div className="advisor-report-table-head"><h3>Đánh giá khách hàng gần đây</h3></div>
              <div className="advisor-report-table">
                <div className="advisor-report-table-header review">
                  <span>Khách hàng</span>
                  <span>Loại dịch vụ</span>
                  <span>Số sao</span>
                  <span>Nhận xét</span>
                  <span>Ngày đánh giá</span>
                </div>
                {(advisorReport?.recentCustomerReviews || []).map((review) => (
                  <article className="advisor-report-table-row review" key={review.id}>
                    <strong className="advisor-report-cell" data-label="Khách hàng">{review.customerFullName || "Khách hàng"}</strong>
                    <span className="advisor-report-cell" data-label="Loại dịch vụ">{review.reviewType === "SERVICE" ? "Dịch vụ" : "Lái thử"}</span>
                    <span className="advisor-report-cell" data-label="Số sao">{review.serviceRating ? `${review.serviceRating} sao` : "Chưa có sao"}</span>
                    <span className="advisor-report-cell" data-label="Nhận xét">{review.serviceComment || "Không có nhận xét"}</span>
                    <span className="advisor-report-cell" data-label="Ngày đánh giá">{formatAdvisorDateTime(review.submittedAt)}</span>
                  </article>
                ))}
                {!(advisorReport?.recentCustomerReviews || []).length ? <div className="advisor-report-empty">Chưa có đánh giá trong bộ lọc này.</div> : null}
              </div>
            </section>
          </div>
        </section>
      ) : null}
    </div>
  );
}
