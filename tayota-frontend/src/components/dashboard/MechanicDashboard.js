"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import {
  addServiceItem,
  completeServiceTicket,
  deleteServiceItem,
  getRecommendedAccessories,
  getServiceInvoice,
  getMyServiceTickets,
  getServiceTicketDetail,
  receiveServiceTicket,
  rejectServiceTicket,
  startServiceTicket,
  updateServiceItem,
} from "@/lib/services/workorders";
import { getMyMechanicReviews, getMyMechanicReviewSummary } from "@/lib/services/reviews";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import { getValidDashboardTab } from "@/lib/dashboard-nav";
import { formatServiceTicketNote, statusLabel, statusPillClass, unwrapList } from "@/lib/format";

const TAB_LABELS = {
  queue: "Cần tiếp nhận",
  active: "Đang sửa",
  history: "Lịch sử",
};
const REVIEW_FILTERS = [
  ["ALL", "Tất cả"],
  ["SUBMITTED", "Đã gửi"],
  ["PENDING", "Đang chờ"],
];
const PAGE_SIZE = 18;

function emptyItem() {
  return {
    itemType: "PART",
    accessoryId: "",
    itemName: "",
    quantity: 1,
    unitPrice: "",
    billingType: "NORMAL",
    note: "",
  };
}

function formatVndZero(value) {
  const numeric = Number(value || 0);
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(Number.isFinite(numeric) ? numeric : 0);
}

function ratingLabel(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return "Chưa có";
  return `${numeric.toFixed(1)} / 5`;
}

function ratingValue(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return 0;
  return Math.max(0, Math.min(5, Math.round(numeric)));
}

function RatingStars({ value, showScore = false }) {
  const rating = ratingValue(value);
  if (!rating) return <span className="rating-stars-display muted">Chưa có</span>;

  return (
    <span className="rating-stars-display" aria-label={`${rating} trên 5 sao`}>
      <span className="rating-stars-filled" aria-hidden="true">{"★".repeat(rating)}</span>
      {rating < 5 ? <span className="rating-stars-empty" aria-hidden="true">{"★".repeat(5 - rating)}</span> : null}
      {showScore ? <span className="rating-stars-score">{rating}/5</span> : null}
    </span>
  );
}

function positiveNumber(value) {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric > 0;
}

function positiveInteger(value) {
  const numeric = Number(value);
  return Number.isInteger(numeric) && numeric >= 1;
}

function itemFinalAmount(form) {
  if (form.billingType !== "NORMAL") return 0;
  const unitPrice = Number(form.unitPrice || 0);
  const quantity = form.itemType === "PART" ? Number(form.quantity || 0) : 1;
  if (!Number.isFinite(unitPrice) || !Number.isFinite(quantity)) return 0;
  return Math.max(0, unitPrice) * Math.max(0, quantity);
}

function formatMechanicDateTime(value) {
  if (!value) return "Chưa cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
}

function formatInvoiceDate(value) {
  if (!value) return "Đang cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium" }).format(date);
}

function plainText(value, fallback = "Đang cập nhật") {
  const text = String(value || "").trim();
  return text || fallback;
}

function invoiceItemAmount(item) {
  const numeric = Number(item?.finalPrice ?? 0);
  return Number.isFinite(numeric) ? numeric : 0;
}

function sumInvoiceItems(items, type) {
  return items
    .filter((item) => String(item?.itemType || "").toUpperCase() === type)
    .reduce((total, item) => total + invoiceItemAmount(item), 0);
}

function buildInvoiceEmail(invoice, ticket) {
  const subject = encodeURIComponent(`Hoa don dich vu ${invoice.invoiceCode || ticket?.id || ""}`.trim());
  const body = encodeURIComponent([
    `Kinh gui ${invoice.customerFullName || ticket?.customerFullName || "Quy khach"},`,
    "",
    `Tayota gui hoa don dich vu ${invoice.invoiceCode || ticket?.id || ""}.`,
    `Tong thanh toan: ${formatVndZero(invoice.totalAmount ?? ticket?.totalAmount ?? 0)}.`,
    "",
    "Tran trong,",
    invoice.dealershipName || "Tayota",
  ].join("\n"));
  return `mailto:${invoice.customerEmail || ""}?subject=${subject}&body=${body}`;
}

function hasReachedServiceStatus(ticket, statuses) {
  return statuses.includes(String(ticket?.status || "").toUpperCase());
}

function serviceReceivedAtLabel(ticket) {
  if (!hasReachedServiceStatus(ticket, ["RECEIVING", "IN_PROGRESS", "COMPLETED"])) return "Chưa tiếp nhận";
  return formatMechanicDateTime(ticket?.receivingAt);
}

function serviceProcessingAtLabel(ticket) {
  if (!hasReachedServiceStatus(ticket, ["IN_PROGRESS", "COMPLETED"])) return "Chưa bắt đầu";
  return formatMechanicDateTime(ticket?.processingAt);
}

function serviceCompletedAtLabel(ticket) {
  if (!hasReachedServiceStatus(ticket, ["COMPLETED"])) return "Chưa hoàn tất";
  return formatMechanicDateTime(ticket?.completedAt);
}

function mechanicServiceTitle(ticket, invoice) {
  return invoice?.carVersionName
    || ticket?.carVersionName
    || ticket?.versionName
    || ticket?.modelName
    || ticket?.vinId
    || "Chi tiết dịch vụ";
}

function mechanicDealershipLabel(ticket, invoice) {
  return invoice?.dealershipName || ticket?.dealershipName || ticket?.dealershipId || "Đang cập nhật";
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

function TicketRows({ tickets, activeId, onOpen, loading }) {
  if (!tickets.length && !loading) return <div className="status-box">Chưa có phiếu dịch vụ trong tab này.</div>;

  return (
    <div className="advisor-row-list">
      {tickets.map((ticket) => (
        <article className={`advisor-row mechanic-ticket-row ${activeId === ticket.id ? "active" : ""}`} key={ticket.id}>
          <div className="advisor-row-main mechanic-ticket-customer">
            <strong>{ticket.customerFullName || "Khách hàng"}</strong>
            <span>Mã dịch vụ: {ticket.id || "Đang cập nhật"}</span>
          </div>
          <div className="advisor-row-field mechanic-ticket-vin">
            <span className="advisor-row-field-label">Số VIN</span>
            <span className="advisor-row-time">{ticket.vinId || "Chưa có VIN"}</span>
          </div>
          <div className="advisor-row-field mechanic-ticket-status">
            <span className="advisor-row-field-label">Trạng thái</span>
            <span className={statusPillClass(ticket.status)}>{statusLabel(ticket.status)}</span>
          </div>
          <div className="advisor-row-actions">
            <button className="advisor-detail-link mechanic-ticket-open" type="button" onClick={() => onOpen(ticket.id)}>Mở phiếu</button>
          </div>
        </article>
      ))}
    </div>
  );
}

function MechanicItemCards({ items = [], canDelete = false, onDelete, scroll = false, emptyText = "Chưa có hạng mục sửa chữa." }) {
  return (
    <div className={`mechanic-item-list ${scroll ? "scrollable" : ""}`}>
      {items.map((item) => (
        <article className="mechanic-item-row" key={item.id || `${item.itemName}-${item.itemType}`}>
          <div className="mechanic-item-main">
            <strong>{item.itemName}</strong>
            <span>{statusLabel(item.itemType)}</span>
          </div>
          <dl className="mechanic-item-meta">
            <div><dt>Số lượng</dt><dd>{item.quantity}</dd></div>
            <div><dt>Đơn giá</dt><dd>{formatVndZero(item.unitPrice)}</dd></div>
            <div><dt>Hình thức</dt><dd>{statusLabel(item.billingType)}</dd></div>
            <div className="mechanic-item-total-field"><dt>Thành tiền</dt><dd>{formatVndZero(item.finalPrice)}</dd></div>
          </dl>
          {canDelete ? (
            <div className="mechanic-item-actions">
              <button className="btn btn-ghost compact-action" type="button" onClick={() => onDelete?.(item.id)}>Xóa</button>
            </div>
          ) : null}
        </article>
      ))}
      {!items.length ? <div className="status-box">{emptyText}</div> : null}
    </div>
  );
}

export default function MechanicDashboard() {
  const searchParams = useSearchParams();
  const tab = getValidDashboardTab("MECHANIC", searchParams.get("tab"));
  const [tickets, setTickets] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [reviewSummary, setReviewSummary] = useState(null);
  const [reviewStatus, setReviewStatus] = useState("SUBMITTED");
  const [selected, setSelected] = useState(null);
  const [accessories, setAccessories] = useState([]);
  const [invoice, setInvoice] = useState(null);
  const [invoicePreviewOpen, setInvoicePreviewOpen] = useState(false);
  const [printInvoiceAfterOpen, setPrintInvoiceAfterOpen] = useState(false);
  const [itemForm, setItemForm] = useState(emptyItem());
  const [editingItemId, setEditingItemId] = useState("");
  const [partEntryMode, setPartEntryMode] = useState("recommended");
  const [itemFormError, setItemFormError] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [ticketPage, setTicketPage] = useState(1);
  const [receiveConfirmOpen, setReceiveConfirmOpen] = useState(false);

  const queueTickets = useMemo(() => tickets.filter((ticket) => ticket.status === "CONFIRMED"), [tickets]);
  const activeTickets = useMemo(() => tickets.filter((ticket) => ["RECEIVING", "IN_PROGRESS"].includes(ticket.status)), [tickets]);
  const historyTickets = useMemo(() => tickets.filter((ticket) => ["COMPLETED", "CANCELED", "EXPIRED"].includes(ticket.status)), [tickets]);
  const visibleTickets = tab === "queue" ? queueTickets : tab === "active" ? activeTickets : historyTickets;
  const pagedTickets = useMemo(() => paginate(visibleTickets, ticketPage), [ticketPage, visibleTickets]);
  const visibleReviews = useMemo(() => (
    reviewStatus === "ALL" ? reviews : reviews.filter((review) => review.status === reviewStatus)
  ), [reviewStatus, reviews]);

  const ticket = selected?.serviceTicket;
  const canReceive = ticket?.status === "CONFIRMED";
  const canStart = ticket?.status === "RECEIVING";
  const canEdit = ticket?.status === "IN_PROGRESS";
  const canViewServiceItems = ticket?.status === "COMPLETED";
  const shouldShowServiceItems = Boolean(ticket && (canEdit || canViewServiceItems));
  const serviceItemsLayoutClass = canEdit ? "editing" : "readonly";
  const isLaborItem = itemForm.itemType === "LABOR";
  const isPartItem = itemForm.itemType === "PART";
  const isRecommendedPart = isPartItem && partEntryMode === "recommended";
  const shouldShowItemFields = isLaborItem || partEntryMode === "manual" || itemForm.accessoryId;
  const isFreeBilling = ["GIFT", "WARRANTY"].includes(itemForm.billingType);
  const itemFormTotal = itemFinalAmount(itemForm);

  async function load() {
    setLoading(true);
    setMessage("");
    try {
      const [ticketResult, reviewResult, summaryResult] = await Promise.all([
        getMyServiceTickets(),
        getMyMechanicReviews(),
        getMyMechanicReviewSummary(),
      ]);
      setTickets(unwrapList(ticketResult));
      setReviews(unwrapList(reviewResult));
      setReviewSummary(summaryResult || null);
    } catch (error) {
      setMessage(error.message || "Không thể tải dashboard kỹ thuật viên.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    setTicketPage(1);
    setSelected(null);
    setInvoice(null);
    setInvoicePreviewOpen(false);
    setPrintInvoiceAfterOpen(false);
    setEditingItemId("");
    setItemForm(emptyItem());
    setItemFormError("");
    setReceiveConfirmOpen(false);
  }, [tab]);

  useEffect(() => {
    if (ticketPage > pagedTickets.totalPages) {
      setTicketPage(pagedTickets.totalPages);
    }
  }, [pagedTickets.totalPages, ticketPage]);

  async function run(action, successMessage, { refreshSelected = true } = {}) {
    setActionLoading(true);
    setMessage("");
    try {
      const result = await action();
      setMessage(successMessage);
      await load();
      if (refreshSelected && selected?.serviceTicket?.id) {
        const refreshed = await getServiceTicketDetail(selected.serviceTicket.id);
        setSelected(refreshed);
      }
      return result;
    } catch (error) {
      setMessage(error.message || "Thao tác thất bại.");
      return null;
    } finally {
      setActionLoading(false);
    }
  }

  async function openTicket(id) {
    setActionLoading(true);
    setMessage("");
    setInvoicePreviewOpen(false);
    setPrintInvoiceAfterOpen(false);
    try {
      const isHistoryDetail = tab === "history";
      const [detail, recommended, historyInvoice] = await Promise.all([
        getServiceTicketDetail(id),
        isHistoryDetail ? Promise.resolve([]) : getRecommendedAccessories(id),
        isHistoryDetail ? getServiceInvoice(id).catch(() => null) : Promise.resolve(null),
      ]);
      setSelected(detail);
      setAccessories(unwrapList(recommended));
      setInvoice(historyInvoice);
      setEditingItemId("");
      setRejectReason("");
      setPartEntryMode("recommended");
      setItemForm(emptyItem());
      setItemFormError("");
      setReceiveConfirmOpen(false);
    } catch (error) {
      setMessage(error.message || "Không thể tải chi tiết phiếu.");
    } finally {
      setActionLoading(false);
    }
  }

  function backToList() {
    setSelected(null);
    setInvoice(null);
    setInvoicePreviewOpen(false);
    setPrintInvoiceAfterOpen(false);
    setEditingItemId("");
    setItemForm(emptyItem());
    setItemFormError("");
    setRejectReason("");
    setReceiveConfirmOpen(false);
  }

  async function confirmReceiveTicket() {
    if (!ticket?.id) return;
    const result = await run(() => receiveServiceTicket(ticket.id), "Đã tiếp nhận xe.");
    if (result) {
      setReceiveConfirmOpen(false);
    }
  }

  async function submitItem(event) {
    event.preventDefault();
    if (!selected?.serviceTicket?.id) return;

    const isLabor = itemForm.itemType === "LABOR";
    const itemName = itemForm.itemName.trim();
    const quantity = isLabor ? 1 : Number(itemForm.quantity);
    const unitPrice = Number(itemForm.unitPrice);
    const billingType = itemForm.billingType || "NORMAL";
    const note = itemForm.note.trim();

    if (!isLabor && partEntryMode === "recommended" && !itemForm.accessoryId) {
      setItemFormError("Vui lòng chọn phụ tùng phù hợp hoặc chuyển sang Khác để nhập thủ công.");
      return;
    }

    if (!itemName) {
      setItemFormError(isLabor ? "Vui lòng nhập hạng mục công thợ." : "Vui lòng nhập tên hạng mục phụ tùng.");
      return;
    }

    if (!isLabor && !positiveInteger(quantity)) {
      setItemFormError("Vui lòng nhập số lượng phụ tùng hợp lệ.");
      return;
    }

    if (billingType === "NORMAL" && !positiveNumber(unitPrice)) {
      setItemFormError(isLabor ? "Vui lòng nhập thành tiền công thợ hợp lệ." : "Vui lòng nhập đơn giá phụ tùng hợp lệ.");
      return;
    }

    if (isLabor && !note) {
      setItemFormError("Vui lòng nhập ghi chú công thợ.");
      return;
    }

    setItemFormError("");

    const payload = {
      ...itemForm,
      itemName,
      accessoryId: itemForm.itemType === "PART" && itemForm.accessoryId ? itemForm.accessoryId : null,
      quantity,
      unitPrice: Number.isFinite(unitPrice) ? unitPrice : null,
      billingType,
      note,
    };

    setActionLoading(true);
    setMessage("");
    try {
      const result = editingItemId
        ? await updateServiceItem(selected.serviceTicket.id, editingItemId, payload)
        : await addServiceItem(selected.serviceTicket.id, payload);
      setMessage(editingItemId ? "Đã cập nhật hạng mục dịch vụ." : "Đã thêm hạng mục dịch vụ.");
      await load();
      setSelected(result);
      setEditingItemId("");
      setPartEntryMode("recommended");
      setItemForm(emptyItem());
      setItemFormError("");
    } catch (error) {
      setItemFormError(error.message || "Không thể lưu hạng mục dịch vụ.");
    } finally {
      setActionLoading(false);
    }
  }

  function chooseAccessory(accessory) {
    setItemFormError("");
    setItemForm((current) => ({
      ...current,
      itemType: "PART",
      accessoryId: accessory?.id || "",
      itemName: accessory?.model || "",
      quantity: current.quantity || 1,
      unitPrice: accessory?.price || "",
      billingType: "NORMAL",
    }));
  }

  async function removeItem(itemId) {
    if (!selected?.serviceTicket?.id) return;
    const result = await run(() => deleteServiceItem(selected.serviceTicket.id, itemId), "Đã xóa hạng mục dịch vụ.");
    if (result) setSelected(result);
  }

  async function rejectTicket() {
    if (!ticket?.id) return;
    if (!rejectReason.trim()) {
      setMessage("Vui lòng nhập lý do từ chối.");
      return;
    }
    const result = await run(
      () => rejectServiceTicket(ticket.id, { reason: rejectReason }),
      "Đã từ chối phiếu và chuyển về cố vấn.",
      { refreshSelected: false }
    );
    if (result) {
      backToList();
    }
  }

  async function openInvoice(options = {}) {
    if (!ticket?.id) return;
    const shouldPrint = Boolean(options?.printAfterOpen);
    const result = await run(() => getServiceInvoice(ticket.id), "", { refreshSelected: false });
    if (result) {
      setInvoice(result);
      setInvoicePreviewOpen(true);
      setPrintInvoiceAfterOpen(shouldPrint);
    }
  }

  function printInvoice() {
    window.print();
  }

  function closeInvoicePreview() {
    setInvoicePreviewOpen(false);
    setPrintInvoiceAfterOpen(false);
  }

  useEffect(() => {
    if (!printInvoiceAfterOpen || !invoicePreviewOpen || !invoice) return undefined;

    const timer = window.setTimeout(() => {
      window.print();
      setPrintInvoiceAfterOpen(false);
    }, 120);

    return () => window.clearTimeout(timer);
  }, [invoice, invoicePreviewOpen, printInvoiceAfterOpen]);

  useEffect(() => {
    if (!invoicePreviewOpen) return undefined;

    function handleAfterPrint() {
      setInvoicePreviewOpen(false);
      setPrintInvoiceAfterOpen(false);
    }

    window.addEventListener("afterprint", handleAfterPrint);
    return () => window.removeEventListener("afterprint", handleAfterPrint);
  }, [invoicePreviewOpen]);

  const invoicePreview = invoicePreviewOpen && invoice ? (() => {
    const items = invoice.items || selected?.items || [];
    const partItems = items.filter((item) => String(item?.itemType || "").toUpperCase() === "PART");
    const laborItems = items.filter((item) => String(item?.itemType || "").toUpperCase() === "LABOR");
    const laborTotal = sumInvoiceItems(items, "LABOR");
    const partsTotal = sumInvoiceItems(items, "PART");
    const discountAmount = Number(invoice.discountAmount || 0);
    const vatAmount = Number(invoice.vatAmount || 0);
    const finalTotal = Number(invoice.totalAmount ?? ticket?.totalAmount ?? laborTotal + partsTotal);
    const vehicleBrand = invoice.carBrand || ticket?.carBrand || "Toyota";
    const customerProblem = invoice.customerProblem || ticket?.customerProblem || ticket?.notes;
    const mechanicNote = invoice.mechanicNote || ticket?.mechanicNote || invoice.vehicleCondition || ticket?.vehicleCondition;

    return (
      <section className="invoice-preview">
        <div className="invoice-toolbar no-print">
          <div>
            <span>Hóa đơn dịch vụ</span>
            <strong>{invoice.invoiceCode || ticket?.id || "Đang cập nhật"}</strong>
          </div>
          <div className="invoice-toolbar-actions">
            <button className="btn btn-primary" type="button" onClick={printInvoice}>In hóa đơn</button>
            <button className="btn btn-secondary" type="button" onClick={printInvoice}>Xuất PDF</button>
            <a className={`btn btn-secondary ${invoice.customerEmail ? "" : "disabled"}`} href={buildInvoiceEmail(invoice, ticket)} aria-disabled={!invoice.customerEmail}>Gửi email cho khách hàng</a>
            <button className="btn btn-ghost" type="button" onClick={closeInvoicePreview}>Quay lại phiếu dịch vụ</button>
          </div>
        </div>

        <article className="invoice-sheet">
          <header className="invoice-header">
            <div className="invoice-brand">
              <div className="invoice-logo" aria-label="Tayota">T</div>
              <div>
                <strong>{plainText(invoice.dealershipName, "Tayota Service Center")}</strong>
                <span>{plainText(invoice.dealershipAddress, "Địa chỉ đại lý đang cập nhật")}</span>
              </div>
            </div>
            <div className="invoice-title-block">
              <p>HÓA ĐƠN DỊCH VỤ</p>
              <dl>
                <div><dt>Mã hóa đơn</dt><dd>{plainText(invoice.invoiceCode || ticket?.id)}</dd></div>
                <div><dt>Ngày lập</dt><dd>{formatInvoiceDate(invoice.issuedAt || ticket?.completedAt)}</dd></div>
              </dl>
            </div>
          </header>

          <section className="invoice-info-grid">
            <div className="invoice-info-card">
              <h4>Thông tin khách hàng</h4>
              <dl>
                <div><dt>Họ tên</dt><dd>{plainText(invoice.customerFullName || ticket?.customerFullName, "Khách hàng")}</dd></div>
                <div><dt>Số điện thoại</dt><dd>{plainText(invoice.customerPhone || ticket?.customerPhone)}</dd></div>
                <div className="invoice-email-field"><dt>Email</dt><dd>{plainText(invoice.customerEmail || ticket?.customerEmail)}</dd></div>
              </dl>
            </div>
            <div className="invoice-info-card invoice-vehicle-card">
              <h4>Thông tin xe</h4>
              <dl>
                <div className="invoice-vin-field"><dt>Số VIN</dt><dd>{plainText(invoice.vinId || ticket?.vinId)}</dd></div>
                <div><dt>Hãng xe</dt><dd>{vehicleBrand}</dd></div>
                <div><dt>Dòng xe / phiên bản</dt><dd>{plainText(invoice.carVersionName || ticket?.carVersionName || ticket?.versionName)}</dd></div>
                <div><dt>Số km hiện tại</dt><dd>{invoice.mileageAtService || ticket?.mileageAtService ? `${invoice.mileageAtService || ticket?.mileageAtService} km` : "Đang cập nhật"}</dd></div>
              </dl>
            </div>
          </section>

          <section className="invoice-service-card">
            <h4>Thông tin phiếu dịch vụ</h4>
            <dl>
              <div><dt>Mã phiếu dịch vụ</dt><dd>{plainText(invoice.serviceTicketId || ticket?.id)}</dd></div>
              <div><dt>Ngày tiếp nhận</dt><dd>{serviceReceivedAtLabel(ticket)}</dd></div>
              <div><dt>Ngày hoàn thành</dt><dd>{serviceCompletedAtLabel(ticket)}</dd></div>
              <div><dt>Kỹ thuật viên</dt><dd>{plainText(invoice.mechanicName || ticket?.mechanicFullName || ticket?.mechanicName)}</dd></div>
              <div className="wide"><dt>Mô tả vấn đề khách hàng báo</dt><dd>{plainText(formatServiceTicketNote(customerProblem), "Không có")}</dd></div>
              <div className="wide"><dt>Ghi chú kiểm tra của kỹ thuật viên</dt><dd>{plainText(mechanicNote, "Không có")}</dd></div>
            </dl>
          </section>

          <section className="invoice-table-section">
            <h4>Chi tiết dịch vụ</h4>
            <h5>Phụ tùng đã thay</h5>
            <div className="invoice-table-wrap">
              <table className="invoice-table">
                <thead>
                  <tr>
                    <th className="invoice-cell-center">STT</th>
                    <th>Tên phụ tùng</th>
                    <th className="invoice-cell-center">Số lượng</th>
                    <th className="invoice-cell-money">Đơn giá</th>
                    <th className="invoice-cell-billing">Hình thức</th>
                    <th className="invoice-cell-money">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {partItems.map((item, index) => (
                    <tr key={item.id || `${item.itemName}-${index}`}>
                      <td className="invoice-cell-center">{index + 1}</td>
                      <td><strong>{plainText(item.itemName, "Hạng mục dịch vụ")}</strong>{item.note ? <span>{item.note}</span> : null}</td>
                      <td className="invoice-cell-center">{item.quantity || 1}</td>
                      <td className="invoice-cell-money">{formatVndZero(item.unitPrice)}</td>
                      <td className="invoice-cell-billing">{statusLabel(item.billingType)}</td>
                      <td className="invoice-cell-money">{formatVndZero(item.finalPrice)}</td>
                    </tr>
                  ))}
                  {!partItems.length ? (
                    <tr>
                      <td colSpan={6} className="invoice-empty-cell">Chưa có phụ tùng đã thay.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
            <h5>Công thợ</h5>
            <div className="invoice-table-wrap">
              <table className="invoice-table invoice-labor-table">
                <thead>
                  <tr>
                    <th className="invoice-cell-center">STT</th>
                    <th>Tên công việc</th>
                    <th>Hạng mục</th>
                    <th className="invoice-cell-money">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {laborItems.map((item, index) => (
                    <tr key={item.id || `${item.itemName}-${index}`}>
                      <td className="invoice-cell-center">{index + 1}</td>
                      <td><strong>{plainText(item.itemName, "Công thợ")}</strong></td>
                      <td>{plainText(item.note || statusLabel(item.billingType), "Công thợ")}</td>
                      <td className="invoice-cell-money">{formatVndZero(item.finalPrice)}</td>
                    </tr>
                  ))}
                  {!laborItems.length ? (
                    <tr>
                      <td colSpan={4} className="invoice-empty-cell">Chưa có hạng mục công thợ.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </section>

          <section className="invoice-summary-section">
            <div className="invoice-warranty-note">
              <h4>Ghi chú bảo hành dịch vụ</h4>
              <p>Phụ tùng chính hãng và hạng mục sửa chữa được bảo hành theo chính sách của đại lý. Vui lòng giữ hóa đơn này khi cần đối chiếu hoặc hỗ trợ sau dịch vụ.</p>
              <p>Cảm ơn Quý khách đã tin tưởng sử dụng dịch vụ của {plainText(invoice.dealershipName, "Tayota")}.</p>
            </div>
            <dl className="invoice-totals">
              <div><dt>Tổng tiền công</dt><dd>{formatVndZero(laborTotal)}</dd></div>
              <div><dt>Tổng tiền phụ tùng</dt><dd>{formatVndZero(partsTotal)}</dd></div>
              <div><dt>Giảm giá</dt><dd>{formatVndZero(discountAmount)}</dd></div>
              <div><dt>Thuế VAT</dt><dd>{formatVndZero(vatAmount)}</dd></div>
              <div className="invoice-grand-total"><dt>Tổng thanh toán</dt><dd>{formatVndZero(finalTotal)}</dd></div>
            </dl>
          </section>

          <footer className="invoice-footer">
            <div><span>Khách hàng</span><strong>Ký và ghi rõ họ tên</strong></div>
            <div><span>Cố vấn dịch vụ</span><strong>Ký và ghi rõ họ tên</strong></div>
            <div><span>Kỹ thuật viên</span><strong>Ký và ghi rõ họ tên</strong></div>
          </footer>
        </article>
      </section>
    );
  })() : null;

  const listPanel = (
    <section className="ops-panel advisor-list-panel advisor-full-panel wide">
      <div className="ops-panel-head">
        <div>
          <h2>{TAB_LABELS[tab] || "Phiếu dịch vụ"}</h2>
        </div>
      </div>
      <TicketRows tickets={pagedTickets.items} activeId={ticket?.id} loading={loading} onOpen={openTicket} />
      <Pagination page={pagedTickets.page} totalPages={pagedTickets.totalPages} totalItems={visibleTickets.length} start={pagedTickets.start} count={pagedTickets.items.length} onChange={setTicketPage} />
    </section>
  );

  const reviewPanel = (
    <section className="ops-panel advisor-list-panel advisor-full-panel wide">
      <div className="ops-panel-head">
        <div><h2>Đánh giá từ khách hàng</h2></div>
      </div>
      <div className="mechanic-review-summary-grid">
        <article className="mechanic-review-summary-card tone-info">
          <header>
            <span className="advisor-overview-label">Sao trung bình</span>
            <strong>{ratingLabel(reviewSummary?.averageMechanicRating)}</strong>
          </header>
          <p><RatingStars value={reviewSummary?.averageMechanicRating} /></p>
        </article>
        <article className="mechanic-review-summary-card tone-success">
          <header>
            <span className="advisor-overview-label">Đánh giá đã gửi</span>
            <strong>{reviewSummary?.submittedCount || 0}</strong>
          </header>
          <p>Phản hồi đã có nhận xét từ khách hàng.</p>
        </article>
        <article className="mechanic-review-summary-card tone-warning">
          <header>
            <span className="advisor-overview-label">Đang chờ</span>
            <strong>{reviewSummary?.pendingCount || 0}</strong>
          </header>
          <p>Phiếu còn chờ khách hàng gửi đánh giá.</p>
        </article>
        <article className="mechanic-review-summary-card tone-progress">
          <header>
            <span className="advisor-overview-label">Tất cả</span>
            <strong>{reviewSummary?.totalCount || 0}</strong>
          </header>
          <p>Tổng số đánh giá liên quan đến kỹ thuật viên.</p>
        </article>
      </div>
      <div className="mechanic-review-filter-bar">
        <label className="advisor-filter-group">
          <span>Tiêu chí lọc</span>
          <select className="advisor-filter-select" value={reviewStatus} onChange={(event) => setReviewStatus(event.target.value)}>
            {REVIEW_FILTERS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </label>
      </div>
      <div className="advisor-row-list">
        {visibleReviews.map((review) => (
          <article className="advisor-row" key={review.id}>
            <div className="advisor-row-main">
              <strong>{review.customerFullName || review.customerEmail || "Khách hàng"}</strong>
              <span>{review.mechanicComment || review.serviceComment || "Chưa có nhận xét"}</span>
            </div>
            <span className="advisor-row-time mechanic-review-rating"><RatingStars value={review.mechanicRating || review.serviceRating} showScore /></span>
            <span className={statusPillClass(review.status)}>{statusLabel(review.status)}</span>
            <span />
          </article>
        ))}
        {!visibleReviews.length && !loading ? <div className="status-box">Chưa có đánh giá trong bộ lọc này.</div> : null}
      </div>
    </section>
  );

  const historyDetailPanel = ticket && tab === "history" ? (() => {
    const historyInvoice = invoice || {};
    const historyItems = historyInvoice.items || selected.items || [];
    const totalAmount = historyInvoice.totalAmount ?? ticket.totalAmount ?? 0;
    const historyStatus = String(ticket.status || "").toUpperCase();
    const canceledTicket = historyStatus === "CANCELED";
    const canExportHistoryInvoice = historyStatus === "COMPLETED";

    return (
      <section className="ops-panel advisor-detail-panel advisor-detail-view wide mechanic-detail-panel advisor-service-detail-panel mechanic-history-detail-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Chi tiết dịch vụ</p>
            <h2>{mechanicServiceTitle(ticket, historyInvoice)}</h2>
          </div>
          <div className="advisor-detail-actions">
            <span className={statusPillClass(ticket.status)}>{statusLabel(ticket.status)}</span>
            {canExportHistoryInvoice ? <button className="btn btn-secondary" type="button" disabled={actionLoading} onClick={() => openInvoice({ printAfterOpen: true })}>In hóa đơn</button> : null}
            <button className="advisor-detail-close" type="button" onClick={backToList} aria-label="Đóng chi tiết dịch vụ">×</button>
          </div>
        </div>
        <div className="service-total-card">
          <span>Tổng tiền dịch vụ</span>
          <strong>{formatVndZero(totalAmount)}</strong>
        </div>
        <dl className="summary-list compact">
          <div><dt>Mã phiếu</dt><dd>{ticket.id || "Đang cập nhật"}</dd></div>
          <div><dt>VIN</dt><dd>{ticket.vinId || "Không có"}</dd></div>
          <div><dt>Đại lý</dt><dd>{mechanicDealershipLabel(ticket, historyInvoice)}</dd></div>
          <div><dt>Kỹ thuật viên</dt><dd>{historyInvoice.mechanicName || ticket.mechanicFullName || ticket.mechanicName || ticket.mechanicId || "Đang cập nhật"}</dd></div>
          <div><dt>Số km</dt><dd>{ticket.mileageAtService ? `${ticket.mileageAtService} km` : "Chưa cập nhật"}</dd></div>
          <div><dt>Tiếp nhận</dt><dd>{serviceReceivedAtLabel(ticket)}</dd></div>
          <div><dt>Bắt đầu sửa</dt><dd>{serviceProcessingAtLabel(ticket)}</dd></div>
          <div><dt>Hoàn tất</dt><dd>{serviceCompletedAtLabel(ticket)}</dd></div>
          {ticket.canceledAt ? <div><dt>Đã hủy</dt><dd>{formatMechanicDateTime(ticket.canceledAt)}</dd></div> : null}
          <div><dt>Tình trạng xe</dt><dd>{ticket.vehicleCondition || historyInvoice.vehicleCondition || "Chưa cập nhật"}</dd></div>
          <div><dt>Ghi chú</dt><dd>{formatServiceTicketNote(ticket.notes) || "Không có"}</dd></div>
          {canceledTicket && ticket.cancelReason ? <div><dt>Lý do hủy</dt><dd>{ticket.cancelReason}</dd></div> : null}
        </dl>
        <section className="mechanic-history-items-section">
          <div className="ops-panel-head compact">
            <div><p className="eyebrow">Hạng mục</p><h3>Dịch vụ và phụ tùng</h3></div>
          </div>
          <MechanicItemCards items={historyItems} />
        </section>
        {invoicePreview}
      </section>
    );
  })() : null;

  const detailPanel = ticket ? (
    <section className="ops-panel advisor-detail-panel advisor-detail-view wide mechanic-detail-panel">
      <div className="ops-panel-head">
        <div>
          <h2>Chi tiết phiếu</h2>
        </div>
        <div className="advisor-detail-actions">
          <span className={statusPillClass(ticket.status)}>{statusLabel(ticket.status)}</span>
          <button className="advisor-detail-close" type="button" onClick={backToList} aria-label="Đóng chi tiết phiếu">×</button>
        </div>
      </div>

      <dl className="summary-list compact">
        <div><dt>Mã dịch vụ</dt><dd>{ticket.id || "Đang cập nhật"}</dd></div>
        <div><dt>Tên khách</dt><dd>{ticket.customerFullName || "Đang cập nhật"}</dd></div>
        <div><dt>Số điện thoại</dt><dd>{ticket.customerPhone || "Đang cập nhật"}</dd></div>
        <div><dt>Số VIN</dt><dd>{ticket.vinId || "Chưa có VIN"}</dd></div>
        <div><dt>Tổng tiền</dt><dd>{ticket.status === "COMPLETED" ? formatVndZero(ticket.totalAmount) : "Chưa chốt"}</dd></div>
        <div><dt>Tình trạng xe</dt><dd>{ticket.vehicleCondition || "Chưa ghi nhận"}</dd></div>
        <div><dt>Ghi chú</dt><dd>{formatServiceTicketNote(ticket.notes) || "Không có"}</dd></div>
      </dl>

      <div className="row-actions wrap">
        {canReceive ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => setReceiveConfirmOpen(true)}>Tiếp nhận</button> : null}
        {canStart ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => startServiceTicket(ticket.id), "Đã bắt đầu sửa.")}>Bắt đầu sửa</button> : null}
        {canEdit ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => completeServiceTicket(ticket.id), "Đã hoàn tất phiếu dịch vụ.")}>Hoàn tất</button> : null}
        {ticket.status === "COMPLETED" ? <button className="btn btn-secondary" type="button" disabled={actionLoading} onClick={() => openInvoice({ printAfterOpen: true })}>Xuất PDF</button> : null}
      </div>

      {canReceive ? (
        <section className="advisor-schedule-editor">
          <div className="ops-panel-head compact">
            <div><p className="eyebrow">Từ chối tiếp nhận</p><h3>Chuyển phiếu về cố vấn phân công lại</h3></div>
          </div>
          <label className="label">Lý do<textarea className="field compact-textarea" rows={3} value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} /></label>
          <button className="btn btn-danger" type="button" disabled={actionLoading || !rejectReason.trim()} onClick={rejectTicket}>Từ chối phiếu</button>
        </section>
      ) : null}

      {shouldShowServiceItems ? <section className={`mechanic-detail-grid ${serviceItemsLayoutClass}`}>
        {canEdit ? (
          <form className="mechanic-section service-item-form" onSubmit={submitItem}>
            <div className="ops-panel-head compact">
              <div><p className="eyebrow">{editingItemId ? "Sửa hạng mục" : "Thêm hạng mục"}</p><h3>{itemForm.itemType === "PART" ? "Chọn phụ tùng phù hợp" : "Thêm công thợ"}</h3></div>
            </div>
            <div className="segmented-tabs secondary">
              <button className={itemForm.itemType === "PART" ? "active" : ""} type="button" onClick={() => { setItemFormError(""); setItemForm({ ...emptyItem(), itemType: "PART" }); setPartEntryMode("recommended"); }}>Phụ tùng</button>
              <button className={itemForm.itemType === "LABOR" ? "active" : ""} type="button" onClick={() => { setItemFormError(""); setItemForm({ ...emptyItem(), itemType: "LABOR", quantity: 1, billingType: "NORMAL" }); setPartEntryMode("manual"); }}>Công thợ</button>
            </div>
            {itemFormError ? <div className="form-alert error">{itemFormError}</div> : null}

            {itemForm.itemType === "PART" ? (
              <>
                <div className="segmented-tabs secondary">
                  <button className={partEntryMode === "recommended" ? "active" : ""} type="button" onClick={() => { setItemFormError(""); setPartEntryMode("recommended"); setItemForm({ ...emptyItem(), itemType: "PART" }); }}>Phụ tùng phù hợp</button>
                  <button className={partEntryMode === "manual" ? "active" : ""} type="button" onClick={() => { setItemFormError(""); setPartEntryMode("manual"); setItemForm((current) => ({ ...current, accessoryId: "", itemName: "", unitPrice: "" })); }}>Khác</button>
                </div>
                {partEntryMode === "recommended" ? (
                  <div className="mechanic-accessory-grid">
                    {accessories.map((accessory) => (
                      <button className={`mechanic-accessory ${String(itemForm.accessoryId) === String(accessory.id) ? "selected" : ""}`} key={accessory.id} type="button" onClick={() => chooseAccessory(accessory)}>
                        <strong>{accessory.model}</strong>
                        <span>{accessory.brand || accessory.type || "Phụ tùng"}</span>
                        <small>{formatVndZero(accessory.price)}</small>
                      </button>
                    ))}
                    {!accessories.length ? <div className="status-box">Chưa có phụ tùng phù hợp với VIN này. Có thể chuyển sang Khác để nhập thủ công.</div> : null}
                  </div>
                ) : null}
              </>
            ) : null}

            {shouldShowItemFields ? (
              <div className={`form-grid compact-form-grid mechanic-item-form-grid ${isLaborItem ? "labor" : "part"}`}>
                {isLaborItem ? (
                  <>
                    <label className="label">Hạng mục<input className="field" value={itemForm.itemName} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, itemName: event.target.value }); }} /></label>
                    <label className="label wide">Ghi chú<input className="field" value={itemForm.note} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, note: event.target.value }); }} /></label>
                    <label className="label">Hình thức<select className="field" value={itemForm.billingType} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, billingType: event.target.value }); }}><option value="NORMAL">Tính phí</option><option value="GIFT">Gift</option><option value="WARRANTY">Bảo trì</option></select></label>
                    <label className="label">Thành tiền<input className={`field ${isFreeBilling ? "mechanic-readonly-field" : ""}`} type="number" min="0" value={isFreeBilling ? 0 : itemForm.unitPrice} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, unitPrice: event.target.value }); }} disabled={isFreeBilling} readOnly={isFreeBilling} /></label>
                  </>
                ) : (
                  <>
                    <label className="label">Tên hạng mục<input className={`field ${isRecommendedPart && itemForm.accessoryId ? "mechanic-readonly-field" : ""}`} value={itemForm.itemName} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, itemName: event.target.value }); }} disabled={isRecommendedPart && Boolean(itemForm.accessoryId)} readOnly={isRecommendedPart && Boolean(itemForm.accessoryId)} /></label>
                    <label className="label">Số lượng<input className="field" type="number" min="1" value={itemForm.quantity} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, quantity: event.target.value }); }} /></label>
                    <label className="label">Đơn giá<input className={`field ${isRecommendedPart && itemForm.accessoryId ? "mechanic-readonly-field" : ""}`} type="number" min="0" value={itemForm.unitPrice} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, unitPrice: event.target.value }); }} disabled={isRecommendedPart && Boolean(itemForm.accessoryId)} readOnly={isRecommendedPart && Boolean(itemForm.accessoryId)} /></label>
                    <label className="label">Hình thức<select className="field" value={itemForm.billingType} onChange={(event) => { setItemFormError(""); setItemForm({ ...itemForm, billingType: event.target.value }); }}><option value="NORMAL">Tính phí</option><option value="GIFT">Gift</option><option value="WARRANTY">Bảo trì</option></select></label>
                    <label className="label">Thành tiền<input className="field mechanic-readonly-field" value={formatVndZero(itemFormTotal)} disabled readOnly /></label>
                  </>
                )}
              </div>
            ) : null}
            <div className="row-actions wrap">
              <button className="btn btn-primary" type="submit" disabled={actionLoading}>{editingItemId ? "Lưu hạng mục" : "Thêm vào phiếu"}</button>
              {editingItemId ? <button className="btn btn-ghost" type="button" onClick={() => { setItemFormError(""); setEditingItemId(""); setPartEntryMode("recommended"); setItemForm(emptyItem()); }}>Hủy sửa</button> : null}
            </div>
          </form>
        ) : null}

        <div className="mechanic-section mechanic-added-items-section">
          <div className="ops-panel-head compact">
            <div><p className="eyebrow">Hạng mục</p><h3>Dịch vụ và phụ tùng</h3></div>
          </div>
          <MechanicItemCards items={selected.items || []} canDelete={canEdit} onDelete={removeItem} scroll />
        </div>
      </section> : null}

      {invoicePreview}
    </section>
  ) : null;

  return (
    <div className="ops-grid workspace-tabs-layout mechanic-workspace">
      {message ? <div className="status-box wide">{message}</div> : null}
      {loading ? <div className="status-box wide">Đang tải phiếu dịch vụ...</div> : null}
      {receiveConfirmOpen && ticket ? (
        <div className="detail-modal-backdrop advisor-confirm-backdrop" role="presentation" onClick={() => !actionLoading && setReceiveConfirmOpen(false)}>
          <section className="detail-modal advisor-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="mechanic-receive-confirm-title" onClick={(event) => event.stopPropagation()}>
            <div className="detail-modal-head">
              <div>
                <h2 id="mechanic-receive-confirm-title">Xác nhận tiếp nhận</h2>
                <span>Phiếu dịch vụ sẽ được chuyển sang trạng thái đã tiếp nhận.</span>
              </div>
            </div>
            <div className="detail-modal-body">
              <div className="row-actions wrap advisor-confirm-actions">
                <button className="btn btn-ghost" type="button" disabled={actionLoading} onClick={() => setReceiveConfirmOpen(false)}>Hủy</button>
                <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={confirmReceiveTicket}>OK</button>
              </div>
            </div>
          </section>
        </div>
      ) : null}
      {tab === "profile" ? <ProfilePanel heading="Hồ sơ cá nhân" /> : tab === "reviews" ? reviewPanel : ticket ? (tab === "history" ? historyDetailPanel : detailPanel) : listPanel}
    </div>
  );
}
