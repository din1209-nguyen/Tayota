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
  createWalkInServiceTicket,
  getAdvisorServiceTicketDetail,
  getAdvisorServiceTickets,
} from "@/lib/services/workorders";
import { getAdvisorReviewSummary } from "@/lib/services/reviews";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import { getValidDashboardTab } from "@/lib/dashboard-nav";
import { statusLabel, unwrapList } from "@/lib/format";

const APPOINTMENT_TYPE_FILTERS = [
  ["SERVICE", "Dịch vụ"],
  ["TEST_DRIVE", "Lái thử"],
  ["CREATE", "Tạo lịch hộ"],
];
const SLOT_TYPE_FILTERS = [
  ["SERVICE", "Dịch vụ"],
  ["TEST_DRIVE", "Lái thử"],
];
const APPOINTMENT_STATUS_FILTERS = [
  ["ALL", "Tất cả"],
  ["PENDING", "Đang chờ"],
  ["CONFIRMED", "Đã xác nhận"],
  ["CHECKED_IN", "Đã check-in"],
  ["COMPLETED", "Đã hoàn thành"],
  ["CANCELED", "Đã hủy"],
  ["EXPIRED", "Đã hết hạn"],
];
const APPOINTMENT_STATUS_QUERY = {
  CANCELED: "ALL",
};
const TICKET_STATUS_FILTERS = [
  ["ALL", "Tất cả"],
  ["CONFIRMED", "Đã xác nhận"],
  ["NEEDS_REASSIGNMENT", "Chờ phân công lại"],
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
  ["today", "Hôm nay"],
  ["month", "Theo tháng"],
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

function toMonthInputValue(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;
}

function getReportDateValue(item) {
  return item?.appointmentDate || item?.createdAt || item?.receivingAt || item?.completedAt || item?.updatedAt || "";
}

function isInReportRange(item, range, monthValue) {
  const value = getReportDateValue(item);
  if (!value) return false;
  const dateKey = String(value).slice(0, 10);
  if (range === "month") return dateKey.startsWith(monthValue);
  return dateKey === toDateInputValue(new Date());
}

function getReportRangeBounds(range, monthValue) {
  if (range === "month") {
    const [year, month] = monthValue.split("-").map(Number);
    const start = new Date(year, month - 1, 1);
    const end = new Date(year, month, 1);
    return { from: start.toISOString(), to: end.toISOString() };
  }

  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
  return { from: start.toISOString(), to: end.toISOString() };
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
  return type === "SERVICE" ? "Dịch vụ" : "Lái thử";
}

function formatVndZero(value) {
  const numeric = Number(value || 0);
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number.isFinite(numeric) ? numeric : 0);
}

function serviceTicketAmountLabel(ticket) {
  if (ticket?.status !== "COMPLETED") return "Chưa chốt";
  return formatVndZero(ticket?.totalAmount);
}

function ratingLabel(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return "Chưa có";
  return `${numeric.toFixed(1)} / 5`;
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
  const [status, setStatus] = useState("PENDING");
  const [appointmentTypeFilter, setAppointmentTypeFilter] = useState("SERVICE");
  const [slotTypeFilter, setSlotTypeFilter] = useState("SERVICE");
  const [ticketStatus, setTicketStatus] = useState("ALL");
  const [appointments, setAppointments] = useState([]);
  const [allAppointments, setAllAppointments] = useState([]);
  const [selectedAppointment, setSelectedAppointment] = useState(null);
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
  const [walkInForm, setWalkInForm] = useState(emptyWalkIn());
  const [vehicleForm, setVehicleForm] = useState({ vinId: "", note: "" });
  const [reassignMechanicId, setReassignMechanicId] = useState("");
  const [slotForm, setSlotForm] = useState({ appointmentType: "SERVICE", startTime: "", endTime: "" });
  const [holidayForm, setHolidayForm] = useState({ holidayDate: "", reason: "" });
  const [reportRange, setReportRange] = useState("today");
  const [reportMonth, setReportMonth] = useState(() => toMonthInputValue(new Date()));
  const [reviewSummary, setReviewSummary] = useState(null);

  const visibleAppointments = useMemo(() => {
    const typeFiltered = appointments.filter((item) => item.type === appointmentTypeFilter);
    if (status === "ALL") {
      return typeFiltered;
    }
    if (status === "CANCELED") {
      return typeFiltered.filter((item) => ["CANCELED", "REJECTED"].includes(item.status));
    }
    return typeFiltered.filter((item) => item.status === status);
  }, [appointmentTypeFilter, appointments, status]);
  const sortedAppointments = useMemo(() => sortByNewest(visibleAppointments), [visibleAppointments]);
  const pagedAppointments = useMemo(() => paginate(sortedAppointments, appointmentPage), [appointmentPage, sortedAppointments]);
  const pagedTickets = useMemo(() => paginate(tickets, ticketPage), [ticketPage, tickets]);
  const filteredSlots = useMemo(() => (
    slots
      .filter((slot) => slot.appointmentType === slotTypeFilter)
      .sort((left, right) => String(left.startTime || "").localeCompare(String(right.startTime || "")))
  ), [slotTypeFilter, slots]);
  const sortedHolidays = useMemo(() => (
    [...holidays].sort((left, right) => String(right.holidayDate || "").localeCompare(String(left.holidayDate || "")))
  ), [holidays]);
  const daysByDate = useMemo(() => new Map(scheduleDays.map((day) => [day.date, day])), [scheduleDays]);
  const scheduleCells = useMemo(() => getCalendarCells(scheduleMonth, daysByDate), [daysByDate, scheduleMonth]);
  const createDaysByDate = useMemo(() => new Map(createScheduleDays.map((day) => [day.date, day])), [createScheduleDays]);
  const createScheduleCells = useMemo(() => getCalendarCells(createScheduleMonth, createDaysByDate), [createDaysByDate, createScheduleMonth]);

  const reportAppointments = useMemo(
    () => allAppointments.filter((item) => isInReportRange(item, reportRange, reportMonth)),
    [allAppointments, reportMonth, reportRange]
  );
  const reportTickets = useMemo(
    () => allTickets.filter((item) => isInReportRange(item, reportRange, reportMonth)),
    [allTickets, reportMonth, reportRange]
  );
  const stats = useMemo(() => ({
    pending: reportAppointments.filter((item) => item.status === "PENDING").length,
    confirmed: reportAppointments.filter((item) => item.status === "CONFIRMED").length,
    activeTickets: reportTickets.filter((item) => ["CONFIRMED", "RECEIVING", "IN_PROGRESS"].includes(item.status)).length,
    completedTickets: reportTickets.filter((item) => item.status === "COMPLETED").length,
  }), [reportAppointments, reportTickets]);
  const reviewSummaryRange = useMemo(() => getReportRangeBounds(reportRange, reportMonth), [reportMonth, reportRange]);

  const loadBase = useCallback(async function loadBase() {
    setLoading(true);
    setMessage("");
    try {
      const [nextAppointments, nextSlots, nextHolidays, nextMechanics, nextTickets, dealership, versions, nextAllAppointments, nextAllTickets, nextReviewSummary] = await Promise.all([
        getAdvisorAppointments(APPOINTMENT_STATUS_QUERY[status] || status),
        getAdvisorTimeSlots(),
        getAdvisorHolidays(),
        getActiveAdvisorMechanics(),
        getAdvisorServiceTickets({ status: ticketStatus }),
        getAdvisorDealership(),
        getAllCarVersions(),
        status === "ALL" ? Promise.resolve(null) : getAdvisorAppointments("ALL"),
        ticketStatus === "ALL" ? Promise.resolve(null) : getAdvisorServiceTickets({ status: "ALL" }),
        getAdvisorReviewSummary(reviewSummaryRange),
      ]);
      setAppointments(unwrapList(nextAppointments));
      setSlots(unwrapList(nextSlots));
      setHolidays(unwrapList(nextHolidays));
      setMechanics(unwrapList(nextMechanics));
      setTickets(unwrapList(nextTickets));
      setAllAppointments(status === "ALL" ? unwrapList(nextAppointments) : unwrapList(nextAllAppointments));
      setAllTickets(ticketStatus === "ALL" ? unwrapList(nextTickets) : unwrapList(nextAllTickets));
      setAdvisorDealershipId(dealership?.dealershipId || "");
      setCarVersions(unwrapList(versions));
      setReviewSummary(nextReviewSummary || null);
    } catch (error) {
      setMessage(error.message || "Không thể tải dashboard cố vấn.");
    } finally {
      setLoading(false);
    }
  }, [reviewSummaryRange, status, ticketStatus]);

  useEffect(() => {
    loadBase();
  }, [loadBase]);

  useEffect(() => {
    setAppointmentPage(1);
  }, [appointmentTypeFilter, status]);

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
      if (appointmentTypeFilter !== "CREATE" || !advisorDealershipId) {
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
  }, [advisorAppointmentForm.appointmentType, advisorDealershipId, appointmentTypeFilter, createScheduleMonth]);

  useEffect(() => {
    let alive = true;

    async function loadCreateScheduleSlots() {
      if (appointmentTypeFilter !== "CREATE" || !advisorDealershipId || !advisorAppointmentForm.appointmentDate) {
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
  }, [advisorAppointmentForm.appointmentDate, advisorAppointmentForm.appointmentType, advisorDealershipId, appointmentTypeFilter]);

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

  function backToList() {
    setActivePanel("list");
    setSelectedAppointment(null);
    setSelectedTicket(null);
    setEditSchedule(false);
    setScheduleMessage("");
  }

  async function openAppointment(id, { edit = false, panel = "appointment-detail" } = {}) {
    setActionLoading(true);
    setMessage("");
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
    const result = await run(() => updateAdvisorAppointment(selectedAppointment.id, payload), successMessage);
    if (result) {
      setSelectedAppointment(result);
    }
  }

  async function saveSchedule() {
    if (!appointmentForm.appointmentDate || !appointmentForm.startTime) {
      setScheduleMessage("Vui lòng chọn ngày làm việc và khung giờ phù hợp.");
      return;
    }
    const result = await run(
      () => updateAdvisorAppointment(selectedAppointment.id, {
        appointmentDate: appointmentForm.appointmentDate,
        startTime: appointmentForm.startTime,
      }),
      "Đã đổi lịch hẹn."
    );
    if (result) {
      setSelectedAppointment(result);
      setEditSchedule(false);
      setScheduleMessage("");
    }
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

    const result = await run(() => createAdvisorAppointment(payload), "Đã tạo lịch hẹn đã xác nhận.");
    if (result) {
      setAdvisorAppointmentForm(emptyAdvisorAppointment());
      setSelectedCustomer(null);
      setCustomerVehicles([]);
      setCreateScheduleDays([]);
      setCreateScheduleSlots([]);
      setAppointmentTypeFilter(result.type || "SERVICE");
      setStatus("CONFIRMED");
    }
  }

  async function toggleTimeSlot(slot) {
    const isActive = slot.active !== false;
    const nextActive = !isActive;
    setActionLoading(true);
    setMessage("");
    try {
      await updateAdvisorTimeSlot(slot.id, { active: nextActive });
      setSlots((current) => current.map((item) => (
        item.id === slot.id ? { ...item, active: nextActive } : item
      )));
      setMessage(nextActive ? "Đã bật lại khung giờ." : "Đã tắt khung giờ.");
    } catch (error) {
      setMessage(error.message || "Không thể cập nhật khung giờ.");
    } finally {
      setActionLoading(false);
    }
  }

  async function toggleHoliday(holiday) {
    const isActive = holiday.active !== false;
    const nextActive = !isActive;
    setActionLoading(true);
    setMessage("");
    try {
      await updateAdvisorHoliday(holiday.id, { active: nextActive });
      setHolidays((current) => current.map((item) => (
        item.id === holiday.id ? { ...item, active: nextActive } : item
      )));
      setMessage(nextActive ? "Đã bật lại ngày nghỉ." : "Đã tắt ngày nghỉ.");
    } catch (error) {
      setMessage(error.message || "Không thể cập nhật ngày nghỉ.");
    } finally {
      setActionLoading(false);
    }
  }

  async function removeTimeSlot(slot) {
    setActionLoading(true);
    setMessage("");
    try {
      await deleteAdvisorTimeSlot(slot.id);
      setSlots((current) => current.filter((item) => item.id !== slot.id));
      setMessage("Đã xóa khung giờ.");
    } catch (error) {
      setMessage(error.message || "Không thể xóa khung giờ.");
    } finally {
      setActionLoading(false);
    }
  }

  async function removeHoliday(holiday) {
    setActionLoading(true);
    setMessage("");
    try {
      await deleteAdvisorHoliday(holiday.id);
      setHolidays((current) => current.filter((item) => item.id !== holiday.id));
      setMessage("Đã xóa ngày nghỉ.");
    } catch (error) {
      setMessage(error.message || "Không thể xóa ngày nghỉ.");
    } finally {
      setActionLoading(false);
    }
  }

  async function searchCustomers() {
    if (!customerKeyword.trim()) return;
    const result = await run(() => searchAdvisorCustomers({ keyword: customerKeyword, size: 8 }), "");
    setCustomers(unwrapList(result));
  }

  async function chooseCustomer(customer) {
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
    const result = await run(() => getCustomerVehicles(customer.id), "");
    setCustomerVehicles(unwrapList(result));
  }

  function clearSelectedCustomer() {
    setSelectedCustomer(null);
    setCustomerVehicles([]);
    setWalkInForm((current) => ({ ...current, userId: "", vinId: "" }));
    setAdvisorAppointmentForm((current) => ({ ...current, customerMode: "guest", userId: "", vinId: "" }));
  }

  function changeAdvisorCustomerMode(nextMode) {
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
    const detail = await run(() => getAdvisorServiceTicketDetail(id), "");
    if (detail) {
      setSelectedTicket(detail);
      setReassignMechanicId("");
      setActivePanel("ticket-detail");
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
      backToList();
      return;
    }

    const payload = {
      mechanicId: checkInForm.mechanicId,
      mileageAtService: checkInForm.mileageAtService ? Number(checkInForm.mileageAtService) : null,
      vehicleCondition: checkInForm.vehicleCondition,
      notes: checkInForm.notes,
    };
    await run(() => checkInServiceAppointment(selectedAppointment.id, payload), "Check-in dịch vụ và tạo phiếu thành công.");
    backToList();
  }

  async function submitSlot(event) {
    event.preventDefault();
    const result = await run(() => createAdvisorTimeSlot({ ...slotForm, active: true }), "Đã tạo khung giờ.");
    if (result) {
      setSlotForm({ appointmentType: "SERVICE", startTime: "", endTime: "" });
      setActivePanel("list");
    }
  }

  async function submitHoliday(event) {
    event.preventDefault();
    const result = await run(() => createAdvisorHoliday({ ...holidayForm, active: true }), "Đã tạo ngày nghỉ.");
    if (result) {
      setHolidayForm({ holidayDate: "", reason: "" });
      setActivePanel("list");
    }
  }

  async function submitWalkIn(event) {
    event.preventDefault();
    const hasUser = Boolean(walkInForm.userId);
    const payload = {
      ...walkInForm,
      mileageAtService: walkInForm.mileageAtService ? Number(walkInForm.mileageAtService) : null,
      userId: hasUser ? walkInForm.userId : null,
      guestFullName: hasUser ? "" : walkInForm.guestFullName,
      guestEmail: hasUser ? "" : walkInForm.guestEmail,
      guestPhone: hasUser ? "" : walkInForm.guestPhone,
    };
    const result = await run(() => createWalkInServiceTicket(payload), "Tạo phiếu dịch vụ trực tiếp thành công.");
    if (result) {
      setWalkInForm(emptyWalkIn());
      setSelectedCustomer(null);
      setCustomerVehicles([]);
    }
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

  const appointmentDetail = selectedAppointment ? (
    <section className="ops-panel advisor-detail-panel advisor-detail-view wide">
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">Chi tiết lịch</p>
          <h2>{appointmentTypeLabel(selectedAppointment.type)}</h2>
        </div>
        <div className="advisor-detail-actions">
          <span className="status-pill">{statusLabel(selectedAppointment.status)}</span>
          <button className="btn btn-ghost advisor-back-button" type="button" onClick={backToList}>Quay lại</button>
        </div>
      </div>
      <dl className="summary-list compact">
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
            <button className="btn btn-ghost" type="button" onClick={() => setEditSchedule(false)}>Đóng</button>
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
          {scheduleMessage ? <div className="status-box">{scheduleMessage}</div> : null}
          <button className="btn btn-primary" type="button" disabled={actionLoading || !appointmentForm.appointmentDate || !appointmentForm.startTime} onClick={saveSchedule}>Lưu thay đổi ngày giờ</button>
        </section>
      ) : null}

      {!editSchedule && canEditAppointmentSchedule(selectedAppointment) ? (
        <div className="row-actions wrap">
          <button className="btn btn-ghost" type="button" disabled={actionLoading} onClick={() => setEditSchedule(true)}>Sửa ngày giờ</button>
        </div>
      ) : null}

      {selectedAppointment.status === "CONFIRMED" ? (
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
          <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={submitCheckIn}>Check-in</button>
        </section>
      ) : null}

      {["PENDING", "CONFIRMED", "CHECKED_IN"].includes(selectedAppointment.status) ? (
        <label className="label">Lý do hủy<textarea className="field compact-textarea" rows={3} value={appointmentForm.cancelReason} onChange={(event) => setAppointmentForm({ ...appointmentForm, cancelReason: event.target.value })} /></label>
      ) : null}
      <div className="row-actions wrap">
        {selectedAppointment.status === "PENDING" ? (
          <>
            <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => updateSelectedAppointment({ status: "CONFIRMED" }, "Đã xác nhận lịch hẹn.")}>Xác nhận lịch</button>
            <button className="btn btn-danger" type="button" disabled={actionLoading || !appointmentForm.cancelReason.trim()} onClick={() => updateSelectedAppointment({ status: "CANCELED", cancelReason: appointmentForm.cancelReason }, "Đã hủy lịch hẹn.")}>Hủy lịch</button>
          </>
        ) : null}
        {["CONFIRMED", "CHECKED_IN"].includes(selectedAppointment.status) ? (
          <button className="btn btn-danger" type="button" disabled={actionLoading || !appointmentForm.cancelReason.trim()} onClick={() => updateSelectedAppointment({ status: "CANCELED", cancelReason: appointmentForm.cancelReason }, "Đã hủy lịch hẹn.")}>Hủy lịch</button>
        ) : null}
        {selectedAppointment.type === "TEST_DRIVE" && selectedAppointment.status === "CHECKED_IN" ? (
          <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => updateSelectedAppointment({ status: "COMPLETED" }, "Đã hoàn tất lịch lái thử.")}>Hoàn tất lái thử</button>
        ) : null}
      </div>
    </section>
  ) : (
    null
  );

  const slotCreatePanel = (
    <section className="ops-panel advisor-detail-panel advisor-detail-view wide">
      <div className="ops-panel-head">
        <div><p className="eyebrow">Khung giờ</p><h2>Tạo khung giờ làm việc</h2></div>
        <button className="btn btn-ghost advisor-back-button" type="button" onClick={backToList}>Quay lại</button>
      </div>
      <form className="ops-form" onSubmit={submitSlot}>
        <div className="form-grid compact-form-grid">
          <label className="label">Loại lịch<select className="field" value={slotForm.appointmentType} onChange={(event) => setSlotForm({ ...slotForm, appointmentType: event.target.value })}><option value="SERVICE">Dịch vụ</option><option value="TEST_DRIVE">Lái thử</option></select></label>
          <label className="label">Bắt đầu<input className="field" type="time" value={slotForm.startTime} onChange={(event) => setSlotForm({ ...slotForm, startTime: event.target.value })} /></label>
          <label className="label">Kết thúc<input className="field" type="time" value={slotForm.endTime} onChange={(event) => setSlotForm({ ...slotForm, endTime: event.target.value })} /></label>
        </div>
        <button className="btn btn-primary" type="submit" disabled={actionLoading}>Tạo khung giờ</button>
      </form>
    </section>
  );

  const holidayCreatePanel = (
    <section className="ops-panel advisor-detail-panel advisor-detail-view wide">
      <div className="ops-panel-head">
        <div><p className="eyebrow">Ngày nghỉ</p><h2>Tạo ngày nghỉ đại lý</h2></div>
        <button className="btn btn-ghost advisor-back-button" type="button" onClick={backToList}>Quay lại</button>
      </div>
      <form className="ops-form" onSubmit={submitHoliday}>
        <div className="form-grid compact-form-grid">
          <label className="label">Ngày nghỉ<input className="field" type="date" value={holidayForm.holidayDate} onChange={(event) => setHolidayForm({ ...holidayForm, holidayDate: event.target.value })} /></label>
          <label className="label">Lý do<input className="field" value={holidayForm.reason} onChange={(event) => setHolidayForm({ ...holidayForm, reason: event.target.value })} /></label>
        </div>
        <button className="btn btn-primary" type="submit" disabled={actionLoading}>Tạo ngày nghỉ</button>
      </form>
    </section>
  );

  const ticketDetail = selectedTicket ? (
    <section className="ops-panel advisor-detail-panel advisor-detail-view wide">
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">Chi tiết phiếu</p>
          <h2>{selectedTicket.serviceTicket?.vinId || "Phiếu dịch vụ"}</h2>
        </div>
        <div className="advisor-detail-actions">
          <span className="status-pill">{statusLabel(selectedTicket.serviceTicket?.status)}</span>
          <button className="btn btn-ghost advisor-back-button" type="button" onClick={backToList}>Quay lại</button>
        </div>
      </div>
      <dl className="summary-list compact">
        <div><dt>Khách hàng</dt><dd>{selectedTicket.serviceTicket?.customerFullName || "Đang cập nhật"}</dd></div>
        <div><dt>Trạng thái</dt><dd>{statusLabel(selectedTicket.serviceTicket?.status)}</dd></div>
        <div><dt>Tổng tiền</dt><dd>{formatVndZero(selectedTicket.serviceTicket?.totalAmount)}</dd></div>
        <div><dt>Ghi chú</dt><dd>{selectedTicket.serviceTicket?.notes || "Không có"}</dd></div>
      </dl>
      {selectedTicket.serviceTicket?.status === "NEEDS_REASSIGNMENT" ? (
        <div className="form-grid compact-form-grid">
          <label className="label">Kỹ thuật viên mới<select className="field" value={reassignMechanicId} onChange={(event) => setReassignMechanicId(event.target.value)}><option value="">Chọn thợ</option>{mechanics.map((item) => <option key={item.id} value={item.id}>{item.fullName || item.id}</option>)}</select></label>
          <div className="label"><span>&nbsp;</span><button className="btn btn-primary" type="button" disabled={actionLoading} onClick={reassignTicket}>Phân công lại</button></div>
        </div>
      ) : null}
      <div className="ops-list advisor-detail-list">{selectedTicket.items?.map((item) => <article key={item.id}><strong>{item.itemName}</strong><span>{statusLabel(item.itemType)} · {item.quantity} x {formatVndZero(item.unitPrice)}</span><small>{formatVndZero(item.finalPrice)}</small></article>)}</div>
    </section>
  ) : (
    null
  );

  return (
    <div className="ops-grid workspace-tabs-layout advisor-workspace">
      {message ? <div className="status-box wide">{message}</div> : null}
      {loading ? <div className="status-box wide">Đang tải dữ liệu vận hành...</div> : null}

      {tab === "profile" ? (
        <ProfilePanel eyebrow="Cố vấn dịch vụ" heading="Hồ sơ cá nhân" />
      ) : null}

      {tab === "overview" ? (
        <section className="ops-panel wide">
          <div className="ops-panel-head">
            <div><p className="eyebrow">Advisor</p><h2>{reportRange === "today" ? "Tổng quan hôm nay" : "Tổng quan theo tháng"}</h2></div>
            <button className="btn btn-ghost" type="button" onClick={loadBase}>Tải lại</button>
          </div>
          <div className="advisor-filter-stack">
            <div className="segmented-tabs secondary">
              {REPORT_RANGE_FILTERS.map(([value, label]) => (
                <button className={reportRange === value ? "active" : ""} key={value} type="button" onClick={() => setReportRange(value)}>{label}</button>
              ))}
            </div>
            {reportRange === "month" ? (
              <label className="label compact-field">Tháng<input className="field" type="month" value={reportMonth} onChange={(event) => setReportMonth(event.target.value)} /></label>
            ) : null}
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
        activePanel === "appointment-detail" ? appointmentDetail : (
          <section className="ops-panel advisor-list-panel advisor-full-panel wide">
            <div className="ops-panel-head">
              <div><p className="eyebrow">Lịch hẹn</p><h2>Quản lý lịch hẹn</h2></div>
              <button className="btn btn-ghost" type="button" onClick={loadBase}>Tải lại</button>
            </div>
            <div className="advisor-filter-stack">
              <div className="segmented-tabs">
                {APPOINTMENT_TYPE_FILTERS.map(([value, label]) => (
                  <button className={appointmentTypeFilter === value ? "active" : ""} key={value} type="button" onClick={() => setAppointmentTypeFilter(value)}>{label}</button>
                ))}
              </div>
              {appointmentTypeFilter !== "CREATE" ? (
                <div className="segmented-tabs secondary">
                  {APPOINTMENT_STATUS_FILTERS.map(([value, label]) => (
                    <button className={status === value ? "active" : ""} key={value} type="button" onClick={() => setStatus(value)}>{label}</button>
                  ))}
                </div>
              ) : null}
            </div>

            {appointmentTypeFilter === "CREATE" ? (
              <form className="ops-form advisor-create-appointment" onSubmit={submitAdvisorAppointment}>
                <div className="form-grid compact-form-grid">
                  <label className="label">Loại lịch<select className="field" value={advisorAppointmentForm.appointmentType} onChange={(event) => changeAdvisorAppointmentType(event.target.value)}><option value="SERVICE">Dịch vụ</option><option value="TEST_DRIVE">Lái thử</option></select></label>
                  <label className="label">Khách hàng<select className="field" value={advisorAppointmentForm.customerMode} onChange={(event) => changeAdvisorCustomerMode(event.target.value)}><option value="guest">Khách vãng lai</option><option value="user">User có tài khoản</option></select></label>
                </div>

                {advisorAppointmentForm.customerMode === "user" ? (
                  <>
                    <div className="search-row">
                      <input className="field" value={customerKeyword} onChange={(event) => setCustomerKeyword(event.target.value)} placeholder={CUSTOMER_SEARCH_PLACEHOLDER} />
                      <button className="btn btn-ghost" type="button" onClick={searchCustomers}>Tìm</button>
                    </div>
                    <small className="form-hint">{CUSTOMER_SEARCH_HINT}</small>
                    {selectedCustomer ? (
                      <div className="row-actions wrap">
                        <strong>Khách đang chọn: {selectedCustomer.fullName || selectedCustomer.email}</strong>
                        <button className="btn btn-ghost compact-action" type="button" onClick={clearSelectedCustomer}>Gỡ khách đã chọn</button>
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
                {createScheduleMessage ? <div className="status-box">{createScheduleMessage}</div> : null}
                <button className="btn btn-primary" type="submit" disabled={actionLoading}>Tạo lịch đã xác nhận</button>
              </form>
            ) : (
              <>
                <div className="advisor-row-list">
                  {pagedAppointments.items.map((item) => (
                    <article className={`advisor-row ${selectedAppointment?.id === item.id ? "active" : ""}`} key={item.id}>
                      <div className="advisor-row-main">
                        <strong>{item.customerFullName || "Khách hàng"}</strong>
                        <span>{item.customerPhone || item.customerEmail || item.customerType || item.id}</span>
                        <small>{item.notes || "Không có ghi chú"}</small>
                      </div>
                      <span className="advisor-row-time">{toDateTime(item)}</span>
                      <span className="status-pill">{statusLabel(item.status)}</span>
                      <div className="advisor-row-actions">
                        <button className="btn btn-ghost compact-action" type="button" disabled={actionLoading} onClick={() => openAppointment(item.id)}>Chi tiết</button>
                      </div>
                    </article>
                  ))}
                  {!pagedAppointments.items.length && !loading ? <div className="status-box">Không có lịch hẹn trong bộ lọc này.</div> : null}
                </div>
                <Pagination page={pagedAppointments.page} totalPages={pagedAppointments.totalPages} totalItems={sortedAppointments.length} start={pagedAppointments.start} count={pagedAppointments.items.length} onChange={setAppointmentPage} />
              </>
            )}
          </section>
        )
      ) : null}

      {tab === "walkin" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Walk-in service</p>
          <h2>Tạo phiếu không cần lịch hẹn</h2>
          <div className="search-row">
            <input className="field" value={customerKeyword} onChange={(event) => setCustomerKeyword(event.target.value)} placeholder={CUSTOMER_SEARCH_PLACEHOLDER} />
            <button className="btn btn-ghost" type="button" onClick={searchCustomers}>Tìm</button>
          </div>
          <small className="form-hint">{CUSTOMER_SEARCH_HINT}</small>
          {selectedCustomer && walkInForm.userId ? (
            <div className="row-actions wrap">
              <strong>Khách đang chọn: {selectedCustomer.fullName || selectedCustomer.email}</strong>
              <button className="btn btn-ghost compact-action" type="button" onClick={clearSelectedCustomer}>Gỡ khách đã chọn</button>
            </div>
          ) : null}
          <div className="chip-row">{customers.map((item) => <button className={selectedCustomer?.id === item.id ? "selected" : ""} key={item.id} type="button" onClick={() => chooseCustomer(item)}>{item.fullName} · {item.phone || item.email}</button>)}</div>
          <form className="ops-form" onSubmit={submitWalkIn}>
            <div className="form-grid">
              {!walkInForm.userId ? (
                <>
                  <label className="label">Họ tên guest<input className="field" value={walkInForm.guestFullName} onChange={(event) => setWalkInForm({ ...walkInForm, guestFullName: event.target.value })} /></label>
                  <label className="label">Email guest<input className="field" type="email" value={walkInForm.guestEmail} onChange={(event) => setWalkInForm({ ...walkInForm, guestEmail: event.target.value })} /></label>
                  <label className="label">Số điện thoại guest<input className="field" value={walkInForm.guestPhone} onChange={(event) => setWalkInForm({ ...walkInForm, guestPhone: event.target.value })} /></label>
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
            <button className="btn btn-primary" type="submit" disabled={actionLoading}>Tạo phiếu dịch vụ</button>
          </form>
        </section>
      ) : null}

      {tab === "vehicles" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Customer vehicles</p>
          <h2>Gán VIN vào tài khoản khách</h2>
          <div className="search-row">
            <input className="field" value={customerKeyword} onChange={(event) => setCustomerKeyword(event.target.value)} placeholder={CUSTOMER_SEARCH_PLACEHOLDER} />
            <button className="btn btn-ghost" type="button" onClick={searchCustomers}>Tìm</button>
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
          <section className="ops-panel advisor-list-panel advisor-full-panel wide">
            <div className="ops-panel-head">
              <div><p className="eyebrow">Time slots</p><h2>Khung giờ làm việc</h2></div>
              <button className="btn btn-primary" type="button" onClick={() => setActivePanel("slot-create")}>Tạo khung giờ</button>
            </div>
            <div className="segmented-tabs secondary">
              {SLOT_TYPE_FILTERS.map(([value, label]) => (
                <button className={slotTypeFilter === value ? "active" : ""} key={value} type="button" onClick={() => setSlotTypeFilter(value)}>{label}</button>
              ))}
            </div>
            <div className="advisor-row-list">
              {filteredSlots.map((slot) => (
                <article className="advisor-row" key={slot.id}>
                  <div className="advisor-row-main"><strong>{slot.appointmentType === "SERVICE" ? "Khung giờ dịch vụ" : "Khung giờ lái thử"}</strong><span>Áp dụng cho {slot.appointmentType === "SERVICE" ? "lịch dịch vụ" : "lịch lái thử"}</span></div>
                  <span className="advisor-row-time">{slot.startTime} - {slot.endTime}</span>
                  <span className="status-pill">{slot.active === false ? "Đã tắt" : "Đang mở"}</span>
                  <div className="advisor-row-actions">
                    <button className="btn btn-ghost compact-action" type="button" disabled={actionLoading} onClick={() => toggleTimeSlot(slot)}>{slot.active === false ? "Bật lại" : "Tắt"}</button>
                    <button className="btn btn-danger compact-action" type="button" disabled={actionLoading} onClick={() => removeTimeSlot(slot)}>Xóa</button>
                  </div>
                </article>
              ))}
              {!filteredSlots.length && !loading ? <div className="status-box">Chưa có khung giờ trong bộ lọc này.</div> : null}
            </div>
          </section>
        )
      ) : null}

      {tab === "holidays" ? (
        activePanel === "holiday-create" ? holidayCreatePanel : (
          <section className="ops-panel advisor-list-panel advisor-full-panel wide">
            <div className="ops-panel-head">
              <div><p className="eyebrow">Holidays</p><h2>Ngày nghỉ đại lý</h2></div>
              <button className="btn btn-primary" type="button" onClick={() => setActivePanel("holiday-create")}>Tạo ngày nghỉ</button>
            </div>
            <div className="advisor-row-list">
              {sortedHolidays.map((holiday) => (
                <article className="advisor-row" key={holiday.id}>
                  <div className="advisor-row-main"><strong>{holiday.holidayDate}</strong><span>{holiday.reason || "Ngày nghỉ đại lý"}</span></div>
                  <span className="advisor-row-time">{holiday.active === false ? "Không chặn đặt lịch" : "Đang chặn đặt lịch"}</span>
                  <span className="status-pill">{holiday.active === false ? "Đã tắt" : "Đang áp dụng"}</span>
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
          <section className="ops-panel advisor-list-panel advisor-full-panel wide">
            <div className="ops-panel-head">
              <div><p className="eyebrow">Service tickets</p><h2>Phiếu dịch vụ đại lý</h2></div>
              <button className="btn btn-ghost" type="button" onClick={loadBase}>Tải lại</button>
            </div>
            <div className="segmented-tabs secondary">
              {TICKET_STATUS_FILTERS.map(([value, label]) => (
                <button className={ticketStatus === value ? "active" : ""} key={value} type="button" onClick={() => setTicketStatus(value)}>{label}</button>
              ))}
            </div>
            <div className="advisor-row-list">{pagedTickets.items.map((ticket) => (
              <article className={`advisor-row ${selectedTicket?.serviceTicket?.id === ticket.id ? "active" : ""}`} key={ticket.id}>
                <div className="advisor-row-main"><strong>{ticket.customerFullName || "Khách vãng lai"}</strong><span>Mã phiếu: {ticket.id}</span></div>
                <span className="advisor-row-time">{serviceTicketAmountLabel(ticket)}</span>
                <span className="status-pill">{statusLabel(ticket.status)}</span>
                <div className="advisor-row-actions"><button className="btn btn-ghost compact-action" type="button" onClick={() => openTicket(ticket.id)}>Chi tiết</button></div>
              </article>
            ))}</div>
            <Pagination page={pagedTickets.page} totalPages={pagedTickets.totalPages} totalItems={tickets.length} start={pagedTickets.start} count={pagedTickets.items.length} onChange={setTicketPage} />
          </section>
        )
      ) : null}

      {tab === "reports" ? (
        <section className="ops-panel wide">
          <div className="ops-panel-head">
            <div><p className="eyebrow">Reports</p><h2>Báo cáo vận hành</h2></div>
            <button className="btn btn-ghost" type="button" onClick={loadBase}>Tải lại</button>
          </div>
          <div className="advisor-filter-stack">
            <div className="segmented-tabs secondary">
              {REPORT_RANGE_FILTERS.map(([value, label]) => (
                <button className={reportRange === value ? "active" : ""} key={value} type="button" onClick={() => setReportRange(value)}>{label}</button>
              ))}
            </div>
            {reportRange === "month" ? (
              <label className="label compact-field">Tháng<input className="field" type="month" value={reportMonth} onChange={(event) => setReportMonth(event.target.value)} /></label>
            ) : null}
          </div>
          <div className="metric-grid">
            <article><strong>{reportAppointments.length}</strong><span>Lịch hẹn</span></article>
            <article><strong>{reportTickets.length}</strong><span>Phiếu dịch vụ</span></article>
            <article><strong>{formatVndZero(reportTickets.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0))}</strong><span>Tổng tiền</span></article>
            <article><strong>{ratingLabel(reviewSummary?.serviceAverageRating)}</strong><span>Sao dịch vụ</span></article>
            <article><strong>{ratingLabel(reviewSummary?.testDriveAverageRating)}</strong><span>Sao lái thử</span></article>
          </div>
        </section>
      ) : null}
    </div>
  );
}
