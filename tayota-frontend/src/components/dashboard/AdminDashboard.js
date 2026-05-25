"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  createAdminUser,
  deleteAiDocument,
  getAdminUsers,
  getAiDocumentJob,
  getAiDocuments,
  resetAdminUserPassword,
  updateAdminUserStatus,
  uploadAiDocument,
} from "@/lib/services/admin";
import { getDealerships } from "@/lib/services/car";
import { getMe } from "@/lib/services/auth";
import { roleLabel, statusLabel, unwrapList } from "@/lib/format";
import { getDashboardPath, setCurrentUser } from "@/lib/session";

const ROLES = ["ADMIN", "MANAGER", "SERVICE_ADVISOR", "ASSISTANT", "MECHANIC", "USER"];
const USER_STATUSES = ["ACTIVE", "BANNED"];
const DOCUMENT_STATUSES = ["uploaded", "indexing", "indexed", "failed"];
const EMPTY_CREATE_FORM = {
  email: "",
  password: "",
  role: "USER",
  dealershipId: "",
};
const EMPTY_FILTERS = {
  keyword: "",
  role: "",
  status: "",
};

function needsDealership(role) {
  return role === "MECHANIC" || role === "SERVICE_ADVISOR";
}

function getUserId(user) {
  return user?.id || user?.userId;
}

export default function AdminDashboard() {
  const router = useRouter();
  const [admin, setAdmin] = useState(null);
  const [tab, setTab] = useState("accounts");
  const [message, setMessage] = useState("");
  const [dealerships, setDealerships] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [createForm, setCreateForm] = useState(EMPTY_CREATE_FORM);
  const [users, setUsers] = useState([]);
  const [userPage, setUserPage] = useState({ page: 0, size: 10, totalPages: 0, totalItems: 0 });
  const [passwordDrafts, setPasswordDrafts] = useState({});
  const [documents, setDocuments] = useState([]);
  const [documentStatus, setDocumentStatus] = useState("");
  const [job, setJob] = useState(null);
  const [busyAction, setBusyAction] = useState("");

  const adminId = getUserId(admin);

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

  useEffect(() => {
    if (!admin) return;
    getDealerships()
      .then((dealers) => setDealerships(unwrapList(dealers)))
      .catch((error) => setMessage(error.message));
  }, [admin]);

  const loadUsers = useCallback(
    async (nextPage = 0) => {
      const result = await getAdminUsers({
        ...filters,
        page: nextPage,
        size: userPage.size,
      });
      const items = result?.items || [];
      setUsers(items);
      setUserPage({
        page: result?.page ?? nextPage,
        size: result?.size ?? userPage.size,
        totalPages: result?.totalPages ?? 0,
        totalItems: result?.totalItems ?? items.length,
      });
    },
    [filters, userPage.size],
  );

  const loadDocuments = useCallback(async () => {
    const status = documentStatus ? [documentStatus] : undefined;
    const result = await getAiDocuments({ status });
    setDocuments(result?.documents || []);
  }, [documentStatus]);

  useEffect(() => {
    if (!admin || tab !== "accounts") return;
    loadUsers(0).catch((error) => setMessage(error.message));
  }, [admin, tab, loadUsers]);

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

  const createNeedsDealership = useMemo(() => needsDealership(createForm.role), [createForm.role]);

  function changeCreateField(event) {
    const { name, value } = event.target;
    setCreateForm((current) => ({
      ...current,
      [name]: value,
      ...(name === "role" && !needsDealership(value) ? { dealershipId: "" } : {}),
    }));
  }

  function changeFilter(event) {
    const { name, value } = event.target;
    setFilters((current) => ({ ...current, [name]: value }));
  }

  async function submitFilters(event) {
    event.preventDefault();
    setMessage("");
    await loadUsers(0).catch((error) => setMessage(error.message));
  }

  async function submitCreateUser(event) {
    event.preventDefault();
    setBusyAction("create-user");
    setMessage("");
    try {
      await createAdminUser({
        email: createForm.email.trim(),
        password: createForm.password,
        role: createForm.role,
        dealershipId: createForm.dealershipId || null,
      });
      setCreateForm(EMPTY_CREATE_FORM);
      setMessage("Tạo tài khoản thành công.");
      await loadUsers(0);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function toggleStatus(user) {
    const nextStatus = user.status === "BANNED" ? "ACTIVE" : "BANNED";
    setBusyAction(`status-${user.id}`);
    setMessage("");
    try {
      await updateAdminUserStatus(user.id, { status: nextStatus });
      setMessage(nextStatus === "BANNED" ? "Đã khóa tài khoản." : "Đã mở khóa tài khoản.");
      await loadUsers(userPage.page);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function submitPassword(user) {
    const password = passwordDrafts[user.id] || "";
    if (!password) return;
    setBusyAction(`password-${user.id}`);
    setMessage("");
    try {
      await resetAdminUserPassword(user.id, { password });
      setPasswordDrafts((current) => ({ ...current, [user.id]: "" }));
      setMessage("Đã đặt lại mật khẩu.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function uploadDocument(event) {
    event.preventDefault();
    const file = event.currentTarget.elements.file.files[0];
    if (!file) return;
    setBusyAction("upload-document");
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
      setBusyAction("");
    }
  }

  async function removeDocument(documentId) {
    if (!window.confirm("Xóa tài liệu này khỏi kho AI?")) return;
    setBusyAction(`document-${documentId}`);
    setMessage("");
    try {
      await deleteAiDocument(documentId);
      setMessage("Đã xóa tài liệu khỏi kho AI.");
      await loadDocuments();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  if (!admin) return <div className="status-box">Đang kiểm tra quyền quản trị...</div>;

  function getDealershipName(dealershipId) {
    return dealerships.find((dealer) => dealer.id === dealershipId)?.name || dealershipId || "Không áp dụng";
  }

  return (
    <div className="admin-dashboard">
      <nav className="role-tabs wide" aria-label="Các mục quản trị">
        <button className={tab === "accounts" ? "active" : ""} type="button" onClick={() => setTab("accounts")}>
          Tài khoản
        </button>
        <button className={tab === "documents" ? "active" : ""} type="button" onClick={() => setTab("documents")}>
          Dữ liệu AI
        </button>
      </nav>
      <div className="dashboard-feedback" aria-live="polite">
        {message ? <div className="status-box">{message}</div> : null}
      </div>

      {tab === "accounts" ? (
        <div className="admin-account-layout">
          <section className="ops-panel">
            <div className="ops-panel-head">
              <div>
                <p className="eyebrow">Tài khoản</p>
                <h2>Tạo tài khoản</h2>
              </div>
            </div>
            <form className="ops-form" onSubmit={submitCreateUser}>
              <input className="field" required name="email" type="email" placeholder="Email" value={createForm.email} onChange={changeCreateField} />
              <input className="field" required name="password" type="password" placeholder="Mật khẩu" value={createForm.password} onChange={changeCreateField} />
              <select className="field" name="role" value={createForm.role} onChange={changeCreateField}>
                {ROLES.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}
              </select>
              {createNeedsDealership ? (
                <select className="field" required name="dealershipId" value={createForm.dealershipId} onChange={changeCreateField}>
                  <option value="">Chọn đại lý</option>
                  {dealerships.map((dealer) => <option key={dealer.id} value={dealer.id}>{dealer.name}</option>)}
                </select>
              ) : null}
              <button className="btn btn-primary" disabled={busyAction === "create-user"} type="submit">Tạo</button>
            </form>
          </section>

          <section className="ops-panel admin-users-panel">
            <div className="ops-panel-head">
              <div>
                <p className="eyebrow">Quản trị</p>
                <h2>Danh sách tài khoản</h2>
              </div>
              <form className="admin-filter-form" onSubmit={submitFilters}>
                <input className="field compact-field" name="keyword" placeholder="Tìm kiếm" value={filters.keyword} onChange={changeFilter} />
                <select className="field compact-field" name="role" value={filters.role} onChange={changeFilter}>
                  <option value="">Tất cả role</option>
                  {ROLES.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}
                </select>
                <select className="field compact-field" name="status" value={filters.status} onChange={changeFilter}>
                  <option value="">Tất cả trạng thái</option>
                  {USER_STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
                </select>
                <button className="btn btn-secondary" type="submit">Lọc</button>
              </form>
            </div>

            <div className="admin-user-table-wrap">
              <table className="admin-user-table">
                <thead>
                  <tr>
                    <th>Tài khoản</th>
                    <th>Role</th>
                    <th>Trạng thái</th>
                    <th>Đại lý</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => {
                    const isSelf = user.id === adminId;
                    return (
                      <tr key={user.id}>
                        <td>
                          <strong>{user.email}</strong>
                          <span>{user.fullname || "Chưa cập nhật"}</span>
                        </td>
                        <td>
                          <span className="role-text">{roleLabel(user.role)}</span>
                        </td>
                        <td><span className={`status-pill ${user.status === "ACTIVE" ? "connected" : "error"}`}>{statusLabel(user.status)}</span></td>
                        <td>
                          <span>{needsDealership(user.role) ? getDealershipName(user.dealershipId) : "Không áp dụng"}</span>
                        </td>
                        <td>
                          <div className="admin-row-actions">
                            <button className="btn btn-ghost" disabled={isSelf || busyAction === `status-${user.id}`} type="button" onClick={() => toggleStatus(user)}>
                              {user.status === "BANNED" ? "Mở khóa" : "Khóa"}
                            </button>
                            <input
                              className="field table-field"
                              type="password"
                              placeholder="Mật khẩu mới"
                              value={passwordDrafts[user.id] || ""}
                              onChange={(event) => setPasswordDrafts((current) => ({ ...current, [user.id]: event.target.value }))}
                            />
                            <button className="btn btn-ghost" disabled={busyAction === `password-${user.id}`} type="button" onClick={() => submitPassword(user)}>
                              Reset
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {!users.length ? (
                    <tr>
                      <td colSpan="5">Không có tài khoản.</td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>

            <div className="admin-pagination">
              <span>{new Intl.NumberFormat("vi-VN").format(userPage.totalItems)} tài khoản</span>
              <div>
                <button className="btn btn-ghost" disabled={userPage.page <= 0} type="button" onClick={() => loadUsers(userPage.page - 1)}>Trước</button>
                <span>{userPage.page + 1} / {Math.max(userPage.totalPages, 1)}</span>
                <button className="btn btn-ghost" disabled={userPage.page + 1 >= userPage.totalPages} type="button" onClick={() => loadUsers(userPage.page + 1)}>Sau</button>
              </div>
            </div>
          </section>
        </div>
      ) : (
        <section className="ops-panel admin-documents">
          <div className="ops-panel-head">
            <div>
              <p className="eyebrow">AI Chatbot</p>
              <h2>Dữ liệu huấn luyện</h2>
            </div>
            <div className="document-toolbar">
              <select className="field compact-field" value={documentStatus} onChange={(event) => setDocumentStatus(event.target.value)}>
                <option value="">Tất cả trạng thái</option>
                {DOCUMENT_STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
              </select>
              <form className="document-upload" onSubmit={uploadDocument}>
                <input className="field" type="file" name="file" accept="application/pdf,.pdf" required />
                <button className="btn btn-primary" disabled={busyAction === "upload-document"} type="submit">Tải PDF</button>
              </form>
            </div>
          </div>
          {job ? <div className="status-box">Indexing job: {job.status}{job.message ? ` - ${job.message}` : ""}</div> : null}
          <div className="ops-list document-list">
            {documents.map((document) => (
              <article key={document.document_id}>
                <strong>{document.filename}</strong>
                <span>{statusLabel(document.status)}</span>
                <span>{new Intl.NumberFormat("vi-VN").format(document.size_bytes || 0)} bytes</span>
                <button className="btn btn-ghost" disabled={busyAction === `document-${document.document_id}`} type="button" onClick={() => removeDocument(document.document_id)}>Xóa</button>
              </article>
            ))}
            {!documents.length ? <div className="status-box">Chưa có tài liệu AI.</div> : null}
          </div>
        </section>
      )}
    </div>
  );
}
