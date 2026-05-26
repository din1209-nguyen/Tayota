"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import MediaUploadField from "@/components/dashboard/MediaUploadField";
import {
  getAdminUser,
  getAdminUserDevices,
  getAdminUserProfile,
  resetAdminUserPassword,
  revokeAdminUserDevice,
  updateAdminUserDealership,
  updateAdminUserProfile,
  updateAdminUserStatus,
} from "@/lib/services/admin";
import { getDealerships } from "@/lib/services/car";
import { getMe } from "@/lib/services/auth";
import { providerLabel, roleLabel, statusLabel, unwrapList } from "@/lib/format";
import { getDashboardPath, setCurrentUser } from "@/lib/session";

const EMPTY_PROFILE = {
  fullname: "",
  phone: "",
  gender: "",
  birthDate: "",
  address: "",
  avatarUrl: "",
};

function getUserId(user) {
  return user?.id || user?.userId;
}

function toProfileDraft(profile, user) {
  return {
    fullname: profile?.fullname ?? user?.fullname ?? "",
    phone: profile?.phone ?? user?.phone ?? "",
    gender: profile?.gender === true ? "true" : profile?.gender === false ? "false" : "",
    birthDate: profile?.birthDate ?? "",
    address: profile?.address ?? "",
    avatarUrl: profile?.avatarUrl ?? user?.avatarUrl ?? "",
  };
}

function formatDateTime(value) {
  if (!value) return "Đang cập nhật";
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function AdminUserDetail({ userId }) {
  const router = useRouter();
  const [admin, setAdmin] = useState(null);
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [profileForm, setProfileForm] = useState(EMPTY_PROFILE);
  const [devices, setDevices] = useState([]);
  const [dealerships, setDealerships] = useState([]);
  const [password, setPassword] = useState("");
  const [dealershipId, setDealershipId] = useState("");
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [busyAction, setBusyAction] = useState("");

  const loadDetail = useCallback(async (currentAdmin) => {
    setLoading(true);
    try {
      const [targetUser, dealershipResult] = await Promise.all([
        getAdminUser(userId),
        getDealerships(),
      ]);
      const sameAccount = getUserId(currentAdmin) === targetUser.id;
      const canManageDetails = sameAccount || targetUser.role !== "ADMIN";

      setUser(targetUser);
      setDealerships(unwrapList(dealershipResult));
      setDealershipId(targetUser.dealershipId || "");

      if (canManageDetails) {
        const [targetProfile, targetDevices] = await Promise.all([
          getAdminUserProfile(userId),
          getAdminUserDevices(userId),
        ]);
        setProfile(targetProfile);
        setProfileForm(toProfileDraft(targetProfile, targetUser));
        setDevices(targetDevices || []);
      } else {
        setProfile(null);
        setProfileForm(EMPTY_PROFILE);
        setDevices([]);
      }
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    let active = true;
    getMe()
      .then(async (currentUser) => {
        if (!active) return;
        setCurrentUser(currentUser);
        if (currentUser?.role !== "ADMIN") {
          router.replace(getDashboardPath(currentUser?.role));
          return;
        }
        setAdmin(currentUser);
        await loadDetail(currentUser);
      })
      .catch(() => router.replace("/auth/login"));
    return () => {
      active = false;
    };
  }, [loadDetail, router]);

  const isSelf = user?.id && user.id === getUserId(admin);
  const isPeerAdmin = user?.role === "ADMIN" && !isSelf;
  const canEditProfile = Boolean(user && !isPeerAdmin);
  const canManageSecurity = Boolean(user && !isSelf && !isPeerAdmin);
  const canManageDealership = canManageSecurity && ["SERVICE_ADVISOR", "MECHANIC"].includes(user?.role);
  const dealershipName = dealerships.find((item) => item.id === user?.dealershipId)?.name
    || user?.dealershipId
    || "Không áp dụng";

  function updateProfileField(event) {
    const { name, value } = event.target;
    setProfileForm((current) => ({ ...current, [name]: value }));
  }

  async function submitProfile(event) {
    event.preventDefault();
    setBusyAction("profile");
    setMessage("");
    try {
      await updateAdminUserProfile({
        userId: user.id,
        fullname: profileForm.fullname,
        phone: profileForm.phone,
        gender: profileForm.gender === "" ? null : profileForm.gender === "true",
        birthDate: profileForm.birthDate || null,
        address: profileForm.address,
        avatarUrl: profileForm.avatarUrl,
      });
      setMessage("Đã cập nhật hồ sơ.");
      await loadDetail(admin);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function toggleStatus() {
    const nextStatus = user.status === "BANNED" ? "ACTIVE" : "BANNED";
    if (nextStatus === "BANNED" && !window.confirm("Khóa tài khoản này và thu hồi toàn bộ phiên đăng nhập?")) {
      return;
    }
    setBusyAction("status");
    setMessage("");
    try {
      await updateAdminUserStatus(user.id, { status: nextStatus });
      setMessage(nextStatus === "BANNED" ? "Đã khóa tài khoản." : "Đã mở khóa tài khoản.");
      await loadDetail(admin);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function submitDealership(event) {
    event.preventDefault();
    setBusyAction("dealership");
    setMessage("");
    try {
      await updateAdminUserDealership(user.id, { dealershipId });
      setMessage("Đã cập nhật đại lý phụ trách.");
      await loadDetail(admin);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function submitPassword(event) {
    event.preventDefault();
    setBusyAction("password");
    setMessage("");
    try {
      await resetAdminUserPassword(user.id, { password });
      setPassword("");
      setMessage("Đã đặt lại mật khẩu và thu hồi toàn bộ phiên đăng nhập.");
      await loadDetail(admin);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  async function revokeDevice(deviceId) {
    setBusyAction(`device-${deviceId}`);
    setMessage("");
    try {
      await revokeAdminUserDevice(user.id, deviceId);
      setMessage("Đã thu hồi phiên đăng nhập.");
      await loadDetail(admin);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusyAction("");
    }
  }

  if (!admin || loading && !user) {
    return <div className="status-box">Đang tải thông tin tài khoản...</div>;
  }

  return (
    <div className="admin-user-detail">
      <header className="admin-detail-page-header">
        <Link className="admin-back-link" href="/dashboard/admin">
          <svg aria-hidden="true" viewBox="0 0 24 24">
            <path d="M15 18l-6-6 6-6" />
            <path d="M9 12h10" />
          </svg>
          Quay lại danh sách
        </Link>
        <div className="admin-detail-heading">
          <p className="eyebrow">Dashboard / Tài khoản</p>
          <h1>Chi tiết tài khoản</h1>
        </div>
        {message ? <div className="dashboard-feedback" aria-live="polite">
          <div className="status-box">{message}</div>
        </div> : null}
      </header>

      {user ? <section className="ops-panel admin-summary-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Tổng quan</p>
            <h2>{user.fullname || user.email}</h2>
          </div>
        </div>
        <dl className="admin-detail-summary">
          <div><dt>Email</dt><dd>{user.email}</dd></div>
          <div><dt>Vai trò</dt><dd>{roleLabel(user.role)}</dd></div>
          <div><dt>Đăng nhập</dt><dd>{providerLabel(user.loginProvider)}</dd></div>
          <div><dt>Điện thoại</dt><dd>{profile?.phone || user.phone || "Chưa cập nhật"}</dd></div>
          <div><dt>Đại lý</dt><dd>{dealershipName}</dd></div>
          <div><dt>Ngày tạo</dt><dd>{formatDateTime(user.createdAt)}</dd></div>
          <div><dt>Địa chỉ</dt><dd>{profile?.address || "Chưa cập nhật"}</dd></div>
        </dl>
        <div className={`account-status-card ${user.status === "ACTIVE" ? "active" : "banned"}`}>
          <div>
            <span className={`status-pill account-status ${user.status === "ACTIVE" ? "connected" : "error"}`}><i />{user.status === "ACTIVE" ? "Đang hoạt động" : statusLabel(user.status)}</span>
            <p>
              {user.status === "ACTIVE"
                ? "Tài khoản có thể đăng nhập và sử dụng hệ thống."
                : "Tài khoản đã bị khóa và các phiên đăng nhập đã được thu hồi."}
            </p>
          </div>
          {canManageSecurity ? <button className="btn btn-secondary" disabled={busyAction === "status"} type="button" onClick={toggleStatus}>
            {user.status === "BANNED" ? "Mở khóa tài khoản" : "Khóa tài khoản"}
          </button> : null}
        </div>
        {isPeerAdmin ? <div className="status-box">Tài khoản quản trị ngang cấp chỉ có thể xem thông tin cơ bản.</div> : null}
      </section> : null}

      {canEditProfile ? <div className="admin-detail-grid">
        <section className="ops-panel">
          <div className="ops-panel-head">
            <div>
              <p className="eyebrow">Hồ sơ</p>
              <h2>Cập nhật thông tin</h2>
            </div>
          </div>
          <form className="ops-form admin-profile-form" onSubmit={submitProfile}>
            <input className="field" name="fullname" value={profileForm.fullname} onChange={updateProfileField} placeholder="Họ và tên" maxLength="40" />
            <input className="field" name="phone" value={profileForm.phone} onChange={updateProfileField} placeholder="Số điện thoại" pattern="\d{10}" />
            <select className="field" name="gender" value={profileForm.gender} onChange={updateProfileField}>
              <option value="">Chưa cập nhật giới tính</option>
              <option value="true">Nam</option>
              <option value="false">Nữ</option>
            </select>
            <input className="field" name="birthDate" type="date" value={profileForm.birthDate} onChange={updateProfileField} />
            <input className="field" name="address" value={profileForm.address} onChange={updateProfileField} placeholder="Địa chỉ" maxLength="255" />
            <MediaUploadField
              label="Ảnh đại diện"
              value={profileForm.avatarUrl}
              onChange={(value) => setProfileForm((current) => ({ ...current, avatarUrl: value }))}
              context="USER_AVATAR"
            />
            <button className="btn btn-primary" disabled={busyAction === "profile"} type="submit">Lưu hồ sơ</button>
          </form>
        </section>

        {canManageSecurity ? <section className="ops-panel admin-security-panel">
          <div className="ops-panel-head">
            <div>
              <p className="eyebrow">Bảo mật</p>
              <h2>Đặt lại mật khẩu</h2>
            </div>
          </div>
          <form className="ops-form admin-password-form" onSubmit={submitPassword}>
            <p className="muted-text">Thao tác này sẽ thu hồi toàn bộ phiên đăng nhập đang hoạt động.</p>
            <label htmlFor="admin-new-password">Mật khẩu mới</label>
            <input
              className="field"
              id="admin-new-password"
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Mật khẩu mới"
            />
            <button className="btn btn-primary" disabled={busyAction === "password"} type="submit">Đặt lại mật khẩu</button>
          </form>
        </section> : null}
      </div> : null}

      {canManageDealership ? <section className="ops-panel admin-assignment-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Phân công</p>
            <h2>Đại lý phụ trách</h2>
          </div>
        </div>
        <p className="muted-text">Đại lý hiện tại: <strong>{dealershipName}</strong></p>
        <form className="admin-assignment-form" onSubmit={submitDealership}>
          <select className="field" required value={dealershipId} onChange={(event) => setDealershipId(event.target.value)}>
            <option value="">Chọn đại lý</option>
            {dealerships.map((dealer) => <option key={dealer.id} value={dealer.id}>{dealer.name}</option>)}
          </select>
          <button className="btn btn-primary" disabled={busyAction === "dealership" || dealershipId === user.dealershipId} type="submit">
            Cập nhật đại lý
          </button>
        </form>
      </section> : null}

      {canEditProfile ? <section className="ops-panel admin-devices-panel">
        <div className="ops-panel-head">
          <div>
            <p className="eyebrow">Phiên đăng nhập</p>
            <h2>Thiết bị đang hoạt động</h2>
          </div>
        </div>
        <div className="admin-device-list">
          {devices.map((device) => (
            <article key={device.deviceId}>
              <div>
                <strong>{device.userAgent || "Thiết bị chưa xác định"}</strong>
                <span>{device.clientIp || "Không có IP"} · {formatDateTime(device.loginAt)}</span>
              </div>
              <button
                className="btn btn-ghost"
                disabled={busyAction === `device-${device.deviceId}`}
                type="button"
                onClick={() => revokeDevice(device.deviceId)}
              >
                Thu hồi
              </button>
            </article>
          ))}
          {!devices.length ? <div className="status-box">Không có phiên đăng nhập đang hoạt động.</div> : null}
        </div>
      </section> : null}
    </div>
  );
}
