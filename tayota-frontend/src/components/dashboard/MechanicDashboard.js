"use client";

import { useEffect, useState } from "react";
import {
  completeServiceTicket,
  getMyServiceTickets,
  receiveServiceTicket,
  startServiceTicket,
} from "@/lib/services/workorders";
import { unwrapList } from "@/lib/format";

export default function MechanicDashboard() {
  const [tickets, setTickets] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

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
      <section className="ops-panel wide">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Mechanic</p>
            <h2>Phiếu dịch vụ của tôi</h2>
          </div>
          <button className="btn btn-ghost" type="button" onClick={load}>Tải lại</button>
        </div>
        {loading ? <p>Đang tải...</p> : null}
        {error ? <div className="status-box">{error}</div> : null}
        <div className="ops-list">
          {tickets.map((ticket) => (
            <article key={ticket.id}>
              <strong>{ticket.vinId}</strong>
              <span>{ticket.status} · {ticket.totalAmount || 0} VND</span>
              <small>{ticket.receivingAt || ticket.appointmentId}</small>
              <div className="row-actions">
                <button className="btn btn-ghost" type="button" onClick={() => run(receiveServiceTicket, ticket.id)}>Nhận</button>
                <button className="btn btn-ghost" type="button" onClick={() => run(startServiceTicket, ticket.id)}>Bắt đầu</button>
                <button className="btn btn-ghost" type="button" onClick={() => run(completeServiceTicket, ticket.id)}>Hoàn tất</button>
              </div>
            </article>
          ))}
          {!tickets.length && !loading ? <p>Chưa có phiếu dịch vụ.</p> : null}
        </div>
      </section>
    </div>
  );
}
