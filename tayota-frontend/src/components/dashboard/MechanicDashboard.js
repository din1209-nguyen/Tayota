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
import { formatVnd, statusLabel, unwrapList } from "@/lib/format";

const TAB_LABELS = {
  queue: "Cần tiếp nhận",
  active: "Đang sửa",
  history: "Lịch sử",
};
const REVIEW_FILTERS = [
  ["ALL", "Tất cả"],
  ["SUBMITTED", "Đã có"],
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

function ticketAmountLabel(ticket) {
  return ticket?.status === "COMPLETED" ? formatVndZero(ticket?.totalAmount) : "Chưa chốt";
}

function ratingLabel(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return "Chưa có";
  return `${numeric.toFixed(1)} / 5`;
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
        <article className={`advisor-row ${activeId === ticket.id ? "active" : ""}`} key={ticket.id}>
          <div className="advisor-row-main">
            <strong>{ticket.vinId}</strong>
            <span>{ticket.customerFullName || "Khách hàng"} · {ticket.customerPhone || ticket.customerEmail || "Chưa có liên hệ"}</span>
            <small>{ticket.mechanicId ? "Đã phân công cho bạn" : "Chưa có kỹ thuật viên"}</small>
          </div>
          <span className="advisor-row-time">{ticketAmountLabel(ticket)}</span>
          <span className="status-pill">{statusLabel(ticket.status)}</span>
          <div className="advisor-row-actions">
            <button className="btn btn-ghost compact-action" type="button" onClick={() => onOpen(ticket.id)}>Mở phiếu</button>
          </div>
        </article>
      ))}
    </div>
  );
}

export default function MechanicDashboard() {
  const searchParams = useSearchParams();
  const tab = getValidDashboardTab("MECHANIC", searchParams.get("tab"));
  const [tickets, setTickets] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [reviewSummary, setReviewSummary] = useState(null);
  const [reviewStatus, setReviewStatus] = useState("ALL");
  const [selected, setSelected] = useState(null);
  const [accessories, setAccessories] = useState([]);
  const [invoice, setInvoice] = useState(null);
  const [itemForm, setItemForm] = useState(emptyItem());
  const [editingItemId, setEditingItemId] = useState("");
  const [partEntryMode, setPartEntryMode] = useState("recommended");
  const [rejectReason, setRejectReason] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [ticketPage, setTicketPage] = useState(1);

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
    setEditingItemId("");
    setItemForm(emptyItem());
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
    try {
      const [detail, recommended] = await Promise.all([
        getServiceTicketDetail(id),
        getRecommendedAccessories(id),
      ]);
      setSelected(detail);
      setAccessories(unwrapList(recommended));
      setInvoice(null);
      setEditingItemId("");
      setRejectReason("");
      setPartEntryMode("recommended");
      setItemForm(emptyItem());
    } catch (error) {
      setMessage(error.message || "Không thể tải chi tiết phiếu.");
    } finally {
      setActionLoading(false);
    }
  }

  function backToList() {
    setSelected(null);
    setInvoice(null);
    setEditingItemId("");
    setItemForm(emptyItem());
    setRejectReason("");
  }

  async function submitItem(event) {
    event.preventDefault();
    if (!selected?.serviceTicket?.id) return;

    const payload = {
      ...itemForm,
      accessoryId: itemForm.itemType === "PART" && itemForm.accessoryId ? itemForm.accessoryId : null,
      quantity: Number(itemForm.quantity || 1),
      unitPrice: itemForm.billingType === "NORMAL" ? Number(itemForm.unitPrice || 0) : null,
    };

    const result = await run(
      () => editingItemId
        ? updateServiceItem(selected.serviceTicket.id, editingItemId, payload)
        : addServiceItem(selected.serviceTicket.id, payload),
      editingItemId ? "Đã cập nhật hạng mục dịch vụ." : "Đã thêm hạng mục dịch vụ."
    );
    if (result) {
      setSelected(result);
      setEditingItemId("");
      setPartEntryMode("recommended");
      setItemForm(emptyItem());
    }
  }

  function chooseAccessory(accessory) {
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

  function editItem(item) {
    setEditingItemId(item.id);
    setPartEntryMode(item.itemType === "PART" && item.accessoryId ? "recommended" : "manual");
    setItemForm({
      itemType: item.itemType || "PART",
      accessoryId: item.accessoryId || "",
      itemName: item.itemName || "",
      quantity: item.quantity || 1,
      unitPrice: item.unitPrice || "",
      billingType: item.billingType || "NORMAL",
      note: item.note || "",
    });
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

  async function openInvoice() {
    if (!ticket?.id) return;
    const result = await run(() => getServiceInvoice(ticket.id), "", { refreshSelected: false });
    if (result) setInvoice(result);
  }

  function printInvoice() {
    window.print();
  }

  const listPanel = (
    <section className="ops-panel advisor-list-panel advisor-full-panel wide">
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">Service tickets</p>
          <h2>{TAB_LABELS[tab] || "Phiếu dịch vụ"}</h2>
        </div>
        <button className="btn btn-ghost" type="button" onClick={load}>Tải lại</button>
      </div>
      <TicketRows tickets={pagedTickets.items} activeId={ticket?.id} loading={loading} onOpen={openTicket} />
      <Pagination page={pagedTickets.page} totalPages={pagedTickets.totalPages} totalItems={visibleTickets.length} start={pagedTickets.start} count={pagedTickets.items.length} onChange={setTicketPage} />
    </section>
  );

  const reviewPanel = (
    <section className="ops-panel advisor-list-panel advisor-full-panel wide">
      <div className="ops-panel-head">
        <div><p className="eyebrow">Reviews</p><h2>Đánh giá từ khách hàng</h2></div>
        <button className="btn btn-ghost" type="button" onClick={load}>Tải lại</button>
      </div>
      <div className="metric-grid">
        <article><strong>{ratingLabel(reviewSummary?.averageMechanicRating)}</strong><span>Sao trung bình</span></article>
        <article><strong>{reviewSummary?.submittedCount || 0}</strong><span>Đánh giá đã có</span></article>
        <article><strong>{reviewSummary?.pendingCount || 0}</strong><span>Đang chờ</span></article>
        <article><strong>{reviewSummary?.totalCount || 0}</strong><span>Tất cả</span></article>
      </div>
      <div className="segmented-tabs secondary">
        {REVIEW_FILTERS.map(([value, label]) => (
          <button className={reviewStatus === value ? "active" : ""} key={value} type="button" onClick={() => setReviewStatus(value)}>{label}</button>
        ))}
      </div>
      <div className="advisor-row-list">
        {visibleReviews.map((review) => (
          <article className="advisor-row" key={review.id}>
            <div className="advisor-row-main">
              <strong>{review.customerFullName || review.customerEmail || "Khách hàng"}</strong>
              <span>{review.mechanicComment || review.serviceComment || "Chưa có nhận xét"}</span>
              <small>{review.vinId || review.serviceId || review.appointmentId || "Không có VIN"}</small>
            </div>
            <span className="advisor-row-time">{review.mechanicRating ? `${review.mechanicRating} / 5` : "Chưa có điểm"}</span>
            <span className="status-pill">{statusLabel(review.status)}</span>
            <span />
          </article>
        ))}
        {!visibleReviews.length && !loading ? <div className="status-box">Chưa có đánh giá trong bộ lọc này.</div> : null}
      </div>
    </section>
  );

  const detailPanel = ticket ? (
    <section className="ops-panel advisor-detail-panel advisor-detail-view wide mechanic-detail-panel">
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">Chi tiết phiếu</p>
          <h2>{ticket.vinId}</h2>
        </div>
        <div className="advisor-detail-actions">
          <span className="status-pill">{statusLabel(ticket.status)}</span>
          <button className="btn btn-ghost advisor-back-button" type="button" onClick={backToList}>Quay lại</button>
        </div>
      </div>

      <dl className="summary-list compact">
        <div><dt>Khách hàng</dt><dd>{ticket.customerFullName || "Đang cập nhật"}</dd></div>
        <div><dt>Liên hệ</dt><dd>{ticket.customerPhone || ticket.customerEmail || "Đang cập nhật"}</dd></div>
        <div><dt>Số km</dt><dd>{ticket.mileageAtService ?? "Chưa ghi nhận"}</dd></div>
        <div><dt>Tổng tiền</dt><dd>{ticket.status === "COMPLETED" ? formatVndZero(ticket.totalAmount) : "Chưa chốt"}</dd></div>
        <div><dt>Tình trạng xe</dt><dd>{ticket.vehicleCondition || "Chưa ghi nhận"}</dd></div>
        <div><dt>Ghi chú</dt><dd>{ticket.notes || "Không có"}</dd></div>
      </dl>

      <div className="row-actions wrap">
        {canReceive ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => receiveServiceTicket(ticket.id), "Đã tiếp nhận xe.")}>Tiếp nhận</button> : null}
        {canStart ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => startServiceTicket(ticket.id), "Đã bắt đầu sửa.")}>Bắt đầu sửa</button> : null}
        {canEdit ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => completeServiceTicket(ticket.id), "Đã hoàn tất phiếu dịch vụ.")}>Hoàn tất</button> : null}
        {ticket.status === "COMPLETED" ? <button className="btn btn-secondary" type="button" disabled={actionLoading} onClick={openInvoice}>Xuất phiếu thu</button> : null}
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

      <section className="mechanic-detail-grid">
        <div className="mechanic-section">
          <div className="ops-panel-head compact">
            <div><p className="eyebrow">Hạng mục</p><h3>Dịch vụ và phụ tùng</h3></div>
          </div>
          <div className="advisor-row-list">
            {selected.items?.map((item) => (
              <article className="advisor-row mechanic-item-row" key={item.id}>
                <div className="advisor-row-main">
                  <strong>{item.itemName}</strong>
                  <span>{statusLabel(item.itemType)} · {item.quantity} x {formatVnd(item.unitPrice)} · {statusLabel(item.billingType)}</span>
                </div>
                <span className="advisor-row-time">{formatVnd(item.finalPrice)}</span>
                {canEdit ? (
                  <div className="advisor-row-actions">
                    <button className="btn btn-ghost compact-action" type="button" onClick={() => editItem(item)}>Sửa</button>
                    <button className="btn btn-ghost compact-action" type="button" onClick={() => removeItem(item.id)}>Xóa</button>
                  </div>
                ) : <span />}
              </article>
            ))}
            {!selected.items?.length ? <div className="status-box">Chưa có hạng mục sửa chữa.</div> : null}
          </div>
        </div>

        {canEdit ? (
          <form className="mechanic-section service-item-form" onSubmit={submitItem}>
            <div className="ops-panel-head compact">
              <div><p className="eyebrow">{editingItemId ? "Sửa hạng mục" : "Thêm hạng mục"}</p><h3>{itemForm.itemType === "PART" ? "Chọn phụ tùng phù hợp" : "Thêm công thợ"}</h3></div>
            </div>
            <div className="segmented-tabs secondary">
              <button className={itemForm.itemType === "PART" ? "active" : ""} type="button" onClick={() => { setItemForm({ ...emptyItem(), itemType: "PART" }); setPartEntryMode("recommended"); }}>Phụ tùng</button>
              <button className={itemForm.itemType === "LABOR" ? "active" : ""} type="button" onClick={() => { setItemForm({ ...emptyItem(), itemType: "LABOR", itemName: "Công thợ" }); setPartEntryMode("manual"); }}>Công thợ</button>
            </div>

            {itemForm.itemType === "PART" ? (
              <>
                <div className="segmented-tabs secondary">
                  <button className={partEntryMode === "recommended" ? "active" : ""} type="button" onClick={() => setPartEntryMode("recommended")}>Phụ tùng phù hợp</button>
                  <button className={partEntryMode === "manual" ? "active" : ""} type="button" onClick={() => { setPartEntryMode("manual"); setItemForm((current) => ({ ...current, accessoryId: "" })); }}>Khác</button>
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

            {(itemForm.itemType === "LABOR" || partEntryMode === "manual" || itemForm.accessoryId) ? (
              <div className="form-grid compact-form-grid">
                <label className="label">Tên hạng mục<input className="field" value={itemForm.itemName} onChange={(event) => setItemForm({ ...itemForm, itemName: event.target.value })} readOnly={itemForm.itemType === "PART" && partEntryMode === "recommended" && Boolean(itemForm.accessoryId)} /></label>
                <label className="label">Số lượng<input className="field" type="number" min="1" value={itemForm.quantity} onChange={(event) => setItemForm({ ...itemForm, quantity: event.target.value })} /></label>
                <label className="label">Hình thức tính phí<select className="field" value={itemForm.billingType} onChange={(event) => setItemForm({ ...itemForm, billingType: event.target.value })}><option value="NORMAL">Tính phí</option><option value="WARRANTY">Bảo hành</option><option value="GIFT">Quà tặng</option></select></label>
                <label className="label">Đơn giá<input className="field" type="number" min="0" value={itemForm.unitPrice} onChange={(event) => setItemForm({ ...itemForm, unitPrice: event.target.value })} disabled={itemForm.billingType !== "NORMAL" || (itemForm.itemType === "PART" && partEntryMode === "recommended" && Boolean(itemForm.accessoryId))} /></label>
                <label className="label wide">Ghi chú<input className="field" value={itemForm.note} onChange={(event) => setItemForm({ ...itemForm, note: event.target.value })} /></label>
              </div>
            ) : null}
            <div className="row-actions wrap">
              <button className="btn btn-primary" type="submit" disabled={actionLoading || !itemForm.itemName}>{editingItemId ? "Lưu hạng mục" : "Thêm vào phiếu"}</button>
              {editingItemId ? <button className="btn btn-ghost" type="button" onClick={() => { setEditingItemId(""); setPartEntryMode("recommended"); setItemForm(emptyItem()); }}>Hủy sửa</button> : null}
            </div>
          </form>
        ) : null}
      </section>

      {invoice ? (
        <section className="invoice-preview">
          <div className="ops-panel-head">
            <div><p className="eyebrow">Phiếu thu</p><h3>{invoice.invoiceCode}</h3></div>
            <button className="btn btn-primary no-print" type="button" onClick={printInvoice}>In / Lưu PDF</button>
          </div>
          <dl className="summary-list compact">
            <div><dt>Khách hàng</dt><dd>{invoice.customerFullName || "Khách hàng"}</dd></div>
            <div><dt>VIN</dt><dd>{invoice.vinId}</dd></div>
            <div><dt>Xe</dt><dd>{invoice.carVersionName || "Tayota"}</dd></div>
            <div><dt>Đại lý</dt><dd>{invoice.dealershipName || invoice.dealershipId}</dd></div>
            <div><dt>Tổng tiền</dt><dd>{formatVndZero(invoice.totalAmount)}</dd></div>
          </dl>
          <div className="advisor-row-list">
            {invoice.items?.map((item) => <article className="advisor-row" key={item.id}><div className="advisor-row-main"><strong>{item.itemName}</strong><span>{item.quantity} x {formatVnd(item.unitPrice)} · {statusLabel(item.billingType)}</span></div><span className="advisor-row-time">{formatVnd(item.finalPrice)}</span><span /></article>)}
          </div>
        </section>
      ) : null}
    </section>
  ) : null;

  return (
    <div className="ops-grid workspace-tabs-layout mechanic-workspace">
      {message ? <div className="status-box wide">{message}</div> : null}
      {loading ? <div className="status-box wide">Đang tải phiếu dịch vụ...</div> : null}
      {tab === "profile" ? <ProfilePanel eyebrow="Kỹ thuật viên" heading="Hồ sơ cá nhân" /> : tab === "reviews" ? reviewPanel : ticket ? detailPanel : listPanel}
    </div>
  );
}
