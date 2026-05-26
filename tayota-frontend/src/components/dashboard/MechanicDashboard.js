"use client";

import { useEffect, useMemo, useState } from "react";
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
import { getMyMechanicReviews } from "@/lib/services/reviews";
import { formatVnd, statusLabel, unwrapList } from "@/lib/format";

const TABS = [
  ["queue", "Cần tiếp nhận"],
  ["active", "Đang sửa"],
  ["history", "Lịch sử"],
  ["reviews", "Đánh giá"],
];

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

function TicketList({ tickets, activeId, onOpen }) {
  if (!tickets.length) return <div className="status-box">Chưa có phiếu dịch vụ.</div>;

  return (
    <div className="ops-list">
      {tickets.map((ticket) => (
        <article className={activeId === ticket.id ? "active" : ""} key={ticket.id}>
          <strong>{ticket.vinId}</strong>
          <span>{ticket.customerFullName || "Khách hàng"} · {statusLabel(ticket.status)}</span>
          <small>{formatVnd(ticket.totalAmount)}</small>
          <div className="row-actions">
            <button className="btn btn-ghost" type="button" onClick={() => onOpen(ticket.id)}>Mở phiếu</button>
          </div>
        </article>
      ))}
    </div>
  );
}

export default function MechanicDashboard() {
  const [tab, setTab] = useState("queue");
  const [tickets, setTickets] = useState([]);
  const [reviews, setReviews] = useState([]);
  const [selected, setSelected] = useState(null);
  const [accessories, setAccessories] = useState([]);
  const [invoice, setInvoice] = useState(null);
  const [itemForm, setItemForm] = useState(emptyItem());
  const [editingItemId, setEditingItemId] = useState("");
  const [rejectReason, setRejectReason] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const queueTickets = useMemo(() => tickets.filter((ticket) => ticket.status === "CONFIRMED"), [tickets]);
  const activeTickets = useMemo(() => tickets.filter((ticket) => ["RECEIVING", "IN_PROGRESS"].includes(ticket.status)), [tickets]);
  const historyTickets = useMemo(() => tickets.filter((ticket) => ["COMPLETED", "CANCELED", "EXPIRED"].includes(ticket.status)), [tickets]);

  async function load() {
    setLoading(true);
    setMessage("");
    try {
      const [ticketResult, reviewResult] = await Promise.all([
        getMyServiceTickets(),
        getMyMechanicReviews(),
      ]);
      setTickets(unwrapList(ticketResult));
      setReviews(unwrapList(reviewResult));
    } catch (error) {
      setMessage(error.message || "Không thể tải dashboard kỹ thuật viên.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function run(action, successMessage) {
    setActionLoading(true);
    setMessage("");
    try {
      const result = await action();
      setMessage(successMessage);
      await load();
      if (selected?.serviceTicket?.id) {
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
    const detail = await run(() => getServiceTicketDetail(id), "");
    if (detail) {
      setSelected(detail);
      setInvoice(null);
      setEditingItemId("");
      setItemForm(emptyItem());
      const recommended = await run(() => getRecommendedAccessories(id), "");
      setAccessories(unwrapList(recommended));
    }
  }

  async function submitItem(event) {
    event.preventDefault();
    if (!selected?.serviceTicket?.id) return;

    const payload = {
      ...itemForm,
      accessoryId: itemForm.accessoryId || null,
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
      setItemForm(emptyItem());
    }
  }

  function chooseAccessory(id) {
    const accessory = accessories.find((item) => String(item.id) === String(id));
    setItemForm((current) => ({
      ...current,
      accessoryId: id,
      itemName: accessory?.model || current.itemName,
      unitPrice: accessory?.price || current.unitPrice,
    }));
  }

  function editItem(item) {
    setEditingItemId(item.id);
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
    const result = await run(() => rejectServiceTicket(ticket.id, { reason: rejectReason }), "Đã từ chối phiếu và chuyển về cố vấn.");
    if (result) {
      setSelected(null);
      setRejectReason("");
    }
  }

  async function openInvoice() {
    if (!ticket?.id) return;
    const result = await run(() => getServiceInvoice(ticket.id), "");
    if (result) setInvoice(result);
  }

  function printInvoice() {
    window.print();
  }

  const visibleTickets = tab === "queue" ? queueTickets : tab === "active" ? activeTickets : historyTickets;
  const ticket = selected?.serviceTicket;
  const canReceive = ticket?.status === "CONFIRMED";
  const canStart = ticket?.status === "RECEIVING";
  const canEdit = ticket?.status === "IN_PROGRESS";

  return (
    <div className="ops-grid workspace-tabs-layout mechanic-workspace">
      <nav className="role-tabs wide" aria-label="Các mục kỹ thuật viên">
        {TABS.map(([id, label]) => (
          <button className={tab === id ? "active" : ""} key={id} type="button" onClick={() => setTab(id)}>{label}</button>
        ))}
      </nav>

      {message ? <div className="status-box wide">{message}</div> : null}
      {loading ? <div className="status-box wide">Đang tải phiếu dịch vụ...</div> : null}

      {tab !== "reviews" ? (
        <section className="ops-panel">
          <div className="ops-panel-head">
            <div>
              <p className="eyebrow">Service tickets</p>
              <h2>{TABS.find(([id]) => id === tab)?.[1]}</h2>
            </div>
            <button className="btn btn-ghost" type="button" onClick={load}>Tải lại</button>
          </div>
          <TicketList tickets={visibleTickets} activeId={ticket?.id} onOpen={openTicket} />
        </section>
      ) : null}

      {tab === "reviews" ? (
        <section className="ops-panel wide">
          <p className="eyebrow">Reviews</p>
          <h2>Đánh giá từ khách hàng</h2>
          <div className="ops-list">
            {reviews.map((review) => (
              <article key={review.id}>
                <strong>{review.mechanicRating || "Chưa có điểm"} / 5</strong>
                <span>{review.mechanicComment || review.serviceComment || "Không có nhận xét"}</span>
                <small>{statusLabel(review.status)}</small>
              </article>
            ))}
            {!reviews.length && !loading ? <div className="status-box">Chưa có đánh giá.</div> : null}
          </div>
        </section>
      ) : null}

      {tab !== "reviews" && ticket ? (
        <section className="ops-panel ticket-detail-panel">
          <div className="ops-panel-head">
            <div>
              <p className="eyebrow">Chi tiết phiếu</p>
              <h2>{ticket.vinId}</h2>
            </div>
            <span className="status-pill">{statusLabel(ticket.status)}</span>
          </div>

          <dl className="summary-list compact">
            <div><dt>Khách hàng</dt><dd>{ticket.customerFullName || "Đang cập nhật"}</dd></div>
            <div><dt>Liên hệ</dt><dd>{ticket.customerPhone || ticket.customerEmail || "Đang cập nhật"}</dd></div>
            <div><dt>Số km</dt><dd>{ticket.mileageAtService ?? "Chưa ghi nhận"}</dd></div>
            <div><dt>Tổng tiền</dt><dd>{formatVnd(ticket.totalAmount)}</dd></div>
            <div><dt>Tình trạng xe</dt><dd>{ticket.vehicleCondition || "Chưa ghi nhận"}</dd></div>
            <div><dt>Ghi chú</dt><dd>{ticket.notes || "Không có"}</dd></div>
          </dl>

          <div className="row-actions wrap">
            {canReceive ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => receiveServiceTicket(ticket.id), "Đã tiếp nhận xe.")}>Tiếp nhận</button> : null}
            {canStart ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => startServiceTicket(ticket.id), "Đã bắt đầu sửa.")}>Bắt đầu sửa</button> : null}
            {canEdit ? <button className="btn btn-primary" type="button" disabled={actionLoading} onClick={() => run(() => completeServiceTicket(ticket.id), "Đã hoàn tất phiếu dịch vụ.")}>Hoàn tất</button> : null}
            {ticket?.status === "COMPLETED" ? <button className="btn btn-secondary" type="button" disabled={actionLoading} onClick={openInvoice}>Xuất phiếu thu</button> : null}
          </div>

          {canReceive ? (
            <div className="inline-form">
              <label className="label">Lý do từ chối tiếp nhận<textarea className="field" rows={3} value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} /></label>
              <button className="btn btn-ghost" type="button" disabled={actionLoading} onClick={rejectTicket}>Từ chối phiếu</button>
            </div>
          ) : null}

          <div className="ops-list service-item-list">
            {selected.items?.map((item) => (
              <article key={item.id}>
                <strong>{item.itemName}</strong>
                <span>{statusLabel(item.itemType)} · {item.quantity} x {formatVnd(item.unitPrice)} · {statusLabel(item.billingType)}</span>
                <small>{formatVnd(item.finalPrice)}</small>
                {canEdit ? <div className="row-actions"><button className="btn btn-ghost" type="button" onClick={() => editItem(item)}>Sửa</button><button className="btn btn-ghost" type="button" onClick={() => removeItem(item.id)}>Xóa</button></div> : null}
              </article>
            ))}
            {!selected.items?.length ? <div className="status-box">Chưa có hạng mục sửa chữa.</div> : null}
          </div>

          {canEdit ? (
            <form className="ops-form service-item-form" onSubmit={submitItem}>
              <h3>{editingItemId ? "Sửa hạng mục" : "Thêm hạng mục"}</h3>
              <div className="form-grid">
                <label className="label">Loại hạng mục<select className="field" value={itemForm.itemType} onChange={(event) => setItemForm({ ...itemForm, itemType: event.target.value })}><option value="PART">Phụ tùng cần thay</option><option value="LABOR">Công thợ</option></select></label>
                {itemForm.itemType === "PART" ? (
                  <label className="label">Phụ tùng gợi ý<select className="field" value={itemForm.accessoryId} onChange={(event) => chooseAccessory(event.target.value)}><option value="">Nhập thủ công</option>{accessories.map((item) => <option key={item.id} value={item.id}>{item.model}</option>)}</select></label>
                ) : null}
                <label className="label">Tên hạng mục<input className="field" value={itemForm.itemName} onChange={(event) => setItemForm({ ...itemForm, itemName: event.target.value })} /></label>
                <label className="label">Số lượng<input className="field" type="number" min="1" value={itemForm.quantity} onChange={(event) => setItemForm({ ...itemForm, quantity: event.target.value })} /></label>
                <label className="label">Hình thức tính phí<select className="field" value={itemForm.billingType} onChange={(event) => setItemForm({ ...itemForm, billingType: event.target.value })}><option value="NORMAL">Tính phí</option><option value="WARRANTY">Bảo hành</option><option value="GIFT">Quà tặng</option></select></label>
                <label className="label">Đơn giá<input className="field" type="number" min="0" value={itemForm.unitPrice} onChange={(event) => setItemForm({ ...itemForm, unitPrice: event.target.value })} disabled={itemForm.billingType !== "NORMAL"} /></label>
                <label className="label wide">Ghi chú<input className="field" value={itemForm.note} onChange={(event) => setItemForm({ ...itemForm, note: event.target.value })} /></label>
              </div>
              <div className="row-actions wrap">
                <button className="btn btn-primary" type="submit" disabled={actionLoading}>{editingItemId ? "Lưu hạng mục" : "Thêm vào phiếu"}</button>
                {editingItemId ? <button className="btn btn-ghost" type="button" onClick={() => { setEditingItemId(""); setItemForm(emptyItem()); }}>Hủy sửa</button> : null}
              </div>
            </form>
          ) : null}

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
                <div><dt>Tổng tiền</dt><dd>{formatVnd(invoice.totalAmount)}</dd></div>
              </dl>
              <div className="ops-list">
                {invoice.items?.map((item) => <article key={item.id}><strong>{item.itemName}</strong><span>{item.quantity} x {formatVnd(item.unitPrice)} · {statusLabel(item.billingType)}</span><small>{formatVnd(item.finalPrice)}</small></article>)}
              </div>
            </section>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
