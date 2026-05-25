"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createAdminUser,
  deleteAiDocument,
  getAiDocumentJob,
  getAiDocuments,
  uploadAiDocument,
} from "@/lib/services/admin";
import { getDealerships } from "@/lib/services/car";
import { getMe } from "@/lib/services/auth";
import { roleLabel, statusLabel, unwrapList } from "@/lib/format";
import { getDashboardPath, setCurrentUser } from "@/lib/session";

const ROLES = ["ADMIN", "MANAGER", "MECHANIC", "ASSISTANT", "USER", "SERVICE_ADVISOR"];
const EMPTY_FORM = {
  email: "",
  password: "",
  role: "USER",
  dealershipId: "",
};

export default function AdminDashboard() {
  const router = useRouter();
  const [admin, setAdmin] = useState(null);
  const [tab, setTab] = useState("accounts");
  const [message, setMessage] = useState("");
  const [dealerships, setDealerships] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [documents, setDocuments] = useState([]);
  const [job, setJob] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    getMe()
      .then((user) => {
        if (!active) return;
        setCurrentUser(user);
        if (user?.role !== "ADMIN") {
          router.replace(getDashboardPath(user?.role));
          return;
        }
        setAdmin(user);
      })
      .catch(() => router.replace("/auth/login"));
    return () => {
      active = false;
    };
  }, [router]);

  const loadDocuments = useCallback(async () => {
    const result = await getAiDocuments();
    setDocuments(result?.documents || []);
  }, []);

  useEffect(() => {
    if (!admin) return;
    getDealerships()
      .then((dealers) => setDealerships(unwrapList(dealers)))
      .catch((error) => setMessage(error.message));
  }, [admin]);

  useEffect(() => {
    if (!admin || tab !== "documents") return;
    loadDocuments().catch((error) => setMessage(error.message));
  }, [admin, tab, loadDocuments]);

  useEffect(() => {
    if (!job || !["queued", "running"].includes(job.status)) return undefined;
    const timer = window.setInterval(async () => {
      try {
        const current = await getAiDocumentJob(job.job_id);
        setJob(current);
        if (!["queued", "running"].includes(current.status)) await loadDocuments();
      } catch (error) {
        setMessage(error.message);
      }
    }, 2000);
    return () => window.clearInterval(timer);
  }, [job, loadDocuments]);

  const requiresDealership = useMemo(
    () => form.role === "MECHANIC" || form.role === "SERVICE_ADVISOR",
    [form.role],
  );

  function changeField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submitUser(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");
    try {
      await createAdminUser({
        email: form.email.trim(),
        password: form.password,
        role: form.role,
        dealershipId: form.dealershipId || null,
      });
      setForm(EMPTY_FORM);
      setMessage("Tạo tài khoản thành công.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function uploadDocument(event) {
    event.preventDefault();
    const file = event.currentTarget.elements.file.files[0];
    if (!file) return;
    setBusy(true);
    setMessage("");
    try {
      const nextJob = await uploadAiDocument(file);
      setJob(nextJob);
      setMessage("Đã tải PDF lên. Hệ thống đang lập chỉ mục.");
      event.currentTarget.reset();
      await loadDocuments();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function removeDocument(documentId) {
    setBusy(true);
    setMessage("");
    try {
      await deleteAiDocument(documentId);
      setMessage("Đã xóa tài liệu khỏi kho AI.");
      await loadDocuments();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  if (!admin) return <div className="status-box">Đang kiểm tra quyền quản trị...</div>;

  return (
    <div className="admin-dashboard">
      <nav className="role-tabs wide" aria-label="Các mục quản trị">
        <button className={tab === "accounts" ? "active" : ""} type="button" onClick={() => setTab("accounts")}>Tài khoản</button>
        <button className={tab === "documents" ? "active" : ""} type="button" onClick={() => setTab("documents")}>Dữ liệu AI</button>
      </nav>
      <div className="dashboard-feedback" aria-live="polite">
        {message ? <div className="status-box">{message}</div> : null}
      </div>

      {tab === "accounts" ? (
        <section className="ops-panel admin-user-editor">
          <p className="eyebrow">Tài khoản</p>
          <h2>Tạo tài khoản mới</h2>
          <form className="ops-form" onSubmit={submitUser}>
            <input className="field" required name="email" type="email" placeholder="Email" value={form.email} onChange={changeField} />
            <input className="field" required name="password" type="password" placeholder="Mật khẩu" value={form.password} onChange={changeField} />
            <select className="field" name="role" value={form.role} onChange={changeField}>
              {ROLES.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}
            </select>
            {requiresDealership ? (
              <select className="field" required name="dealershipId" value={form.dealershipId} onChange={changeField}>
                <option value="">Chọn đại lý</option>
                {dealerships.map((dealer) => <option key={dealer.id} value={dealer.id}>{dealer.name}</option>)}
              </select>
            ) : null}
            <button className="btn btn-primary" disabled={busy} type="submit">Tạo tài khoản</button>
          </form>
          <p className="dashboard-note">Cập nhật hồ sơ tài khoản sử dụng luồng hồ sơ hiện có của hệ thống.</p>
        </section>
      ) : (
        <section className="ops-panel admin-documents">
          <div className="ops-panel-head">
            <div>
              <p className="eyebrow">AI Chatbot</p>
              <h2>Dữ liệu huấn luyện</h2>
            </div>
            <form className="document-upload" onSubmit={uploadDocument}>
              <input className="field" type="file" name="file" accept="application/pdf,.pdf" required />
              <button className="btn btn-primary" disabled={busy} type="submit">Tải PDF lên</button>
            </form>
          </div>
          {job ? <div className="status-box">Indexing job: {job.status}{job.message ? ` - ${job.message}` : ""}</div> : null}
          <div className="ops-list document-list">
            {documents.map((document) => (
              <article key={document.document_id}>
                <strong>{document.filename}</strong>
                <span>Trạng thái: {statusLabel(document.status)}</span>
                <span>{new Intl.NumberFormat("vi-VN").format(document.size_bytes || 0)} bytes</span>
                <button className="btn btn-ghost" disabled={busy} type="button" onClick={() => removeDocument(document.document_id)}>Xóa khỏi kho AI</button>
              </article>
            ))}
            {!documents.length ? <div className="status-box">Chưa có tài liệu AI.</div> : null}
          </div>
        </section>
      )}
    </div>
  );
}
