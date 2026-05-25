"use client";

import { useEffect, useMemo, useState } from "react";
import {
  completeServiceTicket,
  getMyServiceTickets,
  receiveServiceTicket,
  startServiceTicket,
} from "@/lib/services/workorders";
import { unwrapList } from "@/lib/format";

function TicketList({ tickets, onRun }) {
  if (!tickets.length) return <p>Chưa có phiếu dịch vụ.</p>;

  return (
    <div className="ops-list">
      {tickets.map((ticket) => (
        <article key={ticket.id}>
          <strong>{ticket.vinId}</strong>
          <span>{ticket.status} · {ticket.totalAmount || 0} VND</span>
          <small>{ticket.receivingAt || ticket.appointmentId}</small>
          <div className="row-actions">
            <button className="btn btn-ghost" type="button" onClick={() => onRun(receiveServiceTicket, ticket.id)}>Nhận</button>
            <button className="btn btn-ghost" type="button" onClick={() => onRun(startServiceTicket, ticket.id)}>Bắt đầu</button>
            <button className="btn btn-ghost" type="button" onClick={() => onRun(completeServiceTicket, ticket.id)}>Hoàn tất</button>
          </div>
        </article>
      ))}
    </div>
  );
}

export default function MechanicDashboard() {
  const [tickets, setTickets] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const activeTickets = useMemo(() => tickets.filter((ticket) => !["COMPLETED", "CANCELLED"].includes(ticket.status)), [tickets]);
  const doneTickets = useMemo(() => tickets.filter((ticket) => ["COMPLETED", "CANCELLED"].includes(ticket.status)), [tickets]);

  async function load() {
    setLoading(true);
    setError("");
    try {
      setTickets(unwrapList(await getMyServiceTickets()));
    } catch (caughtError) {
      setError(caughtError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function run(action, id) {
    await action(id);
    load();
  }

  return (
    <div className="ops-grid">
      <nav className="role-tabs wide" aria-label="Mechanic sections">
        <a href="#mechanic-tickets">Tất cả phiếu</a>
        <a href="#mechanic-active">Đang xử lý</a>
        <a href="#mechanic-done">Hoàn tất</a>
      </nav>

      <section className="ops-panel wide" id="mechanic-tickets">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Mechanic</p>
            <h2>Phiếu dịch vụ của tôi</h2>
          </div>
          <button className="btn btn-ghost" type="button" onClick={load}>Tải lại</button>
        </div>
        {loading ? <p>Đang tải...</p> : null}
        {error ? <div className="status-box">{error}</div> : null}
        <TicketList tickets={tickets} onRun={run} />
      </section>

      <section className="ops-panel" id="mechanic-active">
        <p className="eyebrow">Active</p>
        <h2>Đang xử lý</h2>
        <TicketList tickets={activeTickets} onRun={run} />
      </section>

      <section className="ops-panel" id="mechanic-done">
        <p className="eyebrow">Done</p>
        <h2>Hoàn tất</h2>
        <TicketList tickets={doneTickets} onRun={run} />
      </section>
    </div>
  );
}
