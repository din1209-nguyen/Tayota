"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  createAdminUser,
  deleteAiDocument,
  getAdminUsers,
  getAiDocumentJob,
  getAiDocuments,
  uploadAiDocument,
} from "@/lib/services/admin";
import { getDealerships } from "@/lib/services/car";
import { getMe } from "@/lib/services/auth";
import { getValidDashboardTab } from "@/lib/dashboard-nav";
import { providerLabel, roleLabel, statusLabel, unwrapList } from "@/lib/format";
import { getDashboardPath, setCurrentUser } from "@/lib/session";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import PasswordInput from "@/components/PasswordInput";

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

export default function AdminDashboard() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const documentFileInputRef = useRef(null);
  const [admin, setAdmin] = useState(null);
  const tab = getValidDashboardTab("ADMIN", searchParams.get("tab"));
  const [message, setMessage] = useState("");
  const [dealerships, setDealerships] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [createForm, setCreateForm] = useState(EMPTY_CREATE_FORM);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [users, setUsers] = useState([]);
  const [userPage, setUserPage] = useState({ page: 0, size: 10, totalPages: 0, totalItems: 0 });
  const [documents, setDocuments] = useState([]);
  const [documentStatus, setDocumentStatus] = useState("");
  const [documentsLoading, setDocumentsLoading] = useState(false);
  const [documentsError, setDocumentsError] = useState("");
  const [selectedDocumentFile, setSelectedDocumentFile] = useState(null);
  const [uploadNotice, setUploadNotice] = useState("");
  const [job, setJob] = useState(null);
  const [busyAction, setBusyAction] = useState("");

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
    setDocumentsLoading(true);
    setDocumentsError("");
    try {
      const status = documentStatus ? [documentStatus] : undefined;
      const result = await getAiDocuments({ status });
      setDocuments(result?.documents || []);
    } catch (error) {
      setDocumentsError(error.message);
    } finally {
      setDocumentsLoading(false);
    }
  }, [documentStatus]);

  useEffect(() => {
    if (!admin || tab !== "accounts") return;
    loadUsers(0).catch((error) => setMessage(error.message));
  }, [admin, tab, loadUsers]);

  useEffect(() => {
    if (!admin || tab !== "documents") return;
    loadDocuments();
  }, [admin, tab, loadDocuments]);

  useEffect(() => {
    if (!job || !["queued", "running"].includes(job.status)) return undefined;
    const timer = window.setInterval(async () => {
      try {
        const current = await getAiDocumentJob(job.job_id);
        setJob(current);
        if (current.status === "running") {
          setUploadNotice("Đang lập chỉ mục tài liệu. Vui lòng chờ trong giây lát.");
        }
        if (current.status === "success") {
          setUploadNotice("Tài liệu đã được lập chỉ mục thành công.");
          await loadDocuments();
        }
        if (current.status === "failed") {
          setUploadNotice(`Lập chỉ mục thất bại${current.message ? `: ${current.message}` : "."}`);
          await loadDocuments();
        }
      } catch (error) {
        setUploadNotice(error.message);
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
      setShowCreateForm(false);
      setMessage("Tạo tài khoản thành công.");
      await loadUsers(0);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function uploadDocument(event) {
    event.preventDefault();
    const file = selectedDocumentFile;
    if (!file) return;
    setBusyAction("upload-document");
    setUploadNotice(`Đang tải lên ${file.name}...`);
    try {
      const nextJob = await uploadAiDocument(file);
      setJob(nextJob);
      setUploadNotice(`Đã tiếp nhận ${file.name}. Tài liệu đang chờ xử lý.`);
      setSelectedDocumentFile(null);
      if (documentFileInputRef.current) documentFileInputRef.current.value = "";
      await loadDocuments();
    } catch (error) {
      setUploadNotice(`Tải tài liệu thất bại: ${error.message}`);
    } finally {
      setBusyAction("");
    }
  }

  async function removeDocument(documentId) {
    if (!window.confirm("Xóa tài liệu này khỏi kho AI?")) return;
    setBusyAction(`document-${documentId}`);
    setUploadNotice("");
    try {
      await deleteAiDocument(documentId);
      setUploadNotice("Đã xóa tài liệu khỏi kho AI.");
      await loadDocuments();
    } catch (error) {
      setUploadNotice(error.message);
    } finally {
      setBusyAction("");
    }
  }

  if (!admin) return <div className="status-box">Đang kiểm tra quyền quản trị...</div>;

  function getDealershipName(dealershipId) {
    return dealerships.find((dealer) => dealer.id === dealershipId)?.name || dealershipId || "Không áp dụng";
  }

  function openUserDetail(userId) {
    router.push(`/dashboard/admin/users/${userId}`);
  }

  function openUserDetailByKeyboard(event, userId) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      openUserDetail(userId);
    }
  }

  function formatCreatedAt(value) {
    if (!value) return "Đang cập nhật";
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium" }).format(new Date(value));
  }

  function formatDocumentSize(value) {
    const size = Number(value) || 0;
    return size >= 1024 * 1024
      ? `${(size / (1024 * 1024)).toFixed(1)} MB`
      : `${Math.max(1, Math.round(size / 1024))} KB`;
  }

  return (
    <div className="admin-dashboard">
      <header className="admin-workspace-header">
        <div>
          <p className="eyebrow">Dashboard / Quản trị</p>
          <h1>Quản trị hệ thống</h1>
          <p className="admin-workspace-copy">Theo dõi tài khoản nội bộ và dữ liệu tư vấn AI.</p>
        </div>
      </header>
      {message ? <div className="dashboard-feedback" aria-live="polite"><div className="status-box">{message}</div></div> : null}

      {tab === "profile" ? (
        <ProfilePanel heading="Hồ sơ cá nhân" />
      ) : tab === "accounts" ? (
        <div className={`admin-account-layout ${showCreateForm ? "" : "list-only"}`}>
          {showCreateForm ? <section className="ops-panel admin-create-panel">
            <div className="ops-panel-head">
              <div>
                <p className="eyebrow">Tài khoản</p>
                <h2>Tạo tài khoản</h2>
              </div>
              <button className="btn btn-ghost" type="button" onClick={() => setShowCreateForm(false)}>Đóng</button>
            </div>
            <form className="ops-form" onSubmit={submitCreateUser}>
              <input className="field" required name="email" type="email" placeholder="Email" value={createForm.email} onChange={changeCreateField} />
              <PasswordInput required name="password" placeholder="Mật khẩu" value={createForm.password} onChange={changeCreateField} />
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
          </section> : null}

          <section className="ops-panel admin-users-panel">
            <div className="ops-panel-head">
              <div>
                <p className="eyebrow">Quản trị</p>
                <h2>Danh sách tài khoản</h2>
              </div>
              <button className="btn btn-primary" type="button" onClick={() => setShowCreateForm(true)}>Tạo tài khoản</button>
            </div>
            <form className="admin-filter-form admin-users-toolbar" onSubmit={submitFilters}>
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

            <div className="admin-user-table-wrap">
              <table className="admin-user-table">
                <thead>
                  <tr>
                    <th>Tài khoản</th>
                    <th>Role</th>
                    <th>Đăng nhập</th>
                    <th>Trạng thái</th>
                    <th>Đại lý</th>
                    <th>Ngày tạo</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => {
                    return (
                      <tr
                        className="admin-user-row"
                        key={user.id}
                        tabIndex={0}
                        onClick={() => openUserDetail(user.id)}
                        onKeyDown={(event) => openUserDetailByKeyboard(event, user.id)}
                      >
                        <td>
                          <strong>{user.email}</strong>
                          <span>{user.fullname || "Chưa cập nhật"}</span>
                        </td>
                        <td>
                          <span className="role-text">{roleLabel(user.role)}</span>
                        </td>
                        <td><span>{providerLabel(user.loginProvider)}</span></td>
                        <td><span className={`status-pill account-status ${user.status === "ACTIVE" ? "connected" : "error"}`}><i />{user.status === "ACTIVE" ? "Đang hoạt động" : statusLabel(user.status)}</span></td>
                        <td>
                          <span>{needsDealership(user.role) ? getDealershipName(user.dealershipId) : "Không áp dụng"}</span>
                        </td>
                        <td><span>{formatCreatedAt(user.createdAt)}</span></td>
                        <td><button className="btn btn-ghost" type="button">Chi tiết</button></td>
                      </tr>
                    );
                  })}
                  {!users.length ? (
                    <tr>
                      <td colSpan="7">Không có tài khoản.</td>
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
      ) : tab === "documents" ? (
        <section className="ops-panel admin-documents">
          <div className="ops-panel-head">
            <div>
              <p className="eyebrow">AI Chatbot</p>
              <h2>Kho tài liệu tư vấn</h2>
              <p className="muted-text">Tải PDF lên để cập nhật nguồn kiến thức phục vụ tư vấn.</p>
            </div>
          </div>
          <form className="document-upload-panel" onSubmit={uploadDocument}>
            <label className="document-picker">
              <span>Chọn tài liệu PDF</span>
              <strong>{selectedDocumentFile?.name || "Chưa chọn file nào"}</strong>
              <small>{selectedDocumentFile ? formatDocumentSize(selectedDocumentFile.size) : "Định dạng hỗ trợ: PDF"}</small>
              <input
                ref={documentFileInputRef}
                type="file"
                name="file"
                accept="application/pdf,.pdf"
                required
                onChange={(event) => setSelectedDocumentFile(event.target.files?.[0] || null)}
              />
            </label>
            <button className="btn btn-primary" disabled={busyAction === "upload-document" || !selectedDocumentFile} type="submit">
              {busyAction === "upload-document" ? "Đang tải lên..." : "Tải lên và lập chỉ mục"}
            </button>
          </form>
          {uploadNotice || job?.status === "queued" || job?.status === "running" ? (
            <div className={`document-job-state ${job?.status || ""}`}>
              {["queued", "running"].includes(job?.status) ? <span className="pending-spinner" aria-hidden="true" /> : null}
              <div>
                <strong>
                  {job?.status === "queued"
                    ? "Đang chờ xử lý"
                    : job?.status === "running"
                      ? "Đang xử lý tài liệu"
                      : job?.status === "success"
                        ? "Đã xử lý thành công"
                        : job?.status === "failed"
                          ? "Xử lý thất bại"
                          : "Cập nhật tài liệu"}
                </strong>
                <p>{uploadNotice}</p>
              </div>
            </div>
          ) : null}
          <div className="document-list-head">
            <h3>Tài liệu đã tải lên</h3>
            <select className="field compact-field" value={documentStatus} onChange={(event) => setDocumentStatus(event.target.value)}>
              <option value="">Tất cả trạng thái</option>
              {DOCUMENT_STATUSES.map((status) => <option key={status} value={status}>{statusLabel(status)}</option>)}
            </select>
          </div>
          <div className="ops-list document-list">
            {documents.map((document) => (
              <article key={document.document_id}>
                <div className="document-name">
                  <strong>{document.filename}</strong>
                  <small>{formatCreatedAt(document.uploaded_at)}</small>
                </div>
                <span className={`status-pill document-status ${document.status}`}><i />{statusLabel(document.status)}</span>
                <span>{formatDocumentSize(document.size_bytes)}</span>
                <button
                  className="btn btn-ghost"
                  disabled={busyAction === `document-${document.document_id}` || ["uploaded", "indexing"].includes(document.status)}
                  type="button"
                  onClick={() => removeDocument(document.document_id)}
                >
                  Xóa
                </button>
              </article>
            ))}
            {documentsLoading ? <div className="status-box">Đang tải danh sách tài liệu...</div> : null}
            {documentsError ? <div className="status-box">{documentsError}</div> : null}
            {!documentsLoading && !documents.length && !documentsError ? <div className="status-box">Chưa có tài liệu AI.</div> : null}
          </div>
        </section>
      ) : null}
    </div>
  );
}
