"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import UserAvatar from "@/components/UserAvatar";
import { changePasswordDirect, getMe, logout } from "@/lib/services/auth";
import { uploadMedia } from "@/lib/services/media";
import { getUserProfile, updateUserProfile } from "@/lib/services/user";
import { roleLabel } from "@/lib/format";
import { clearSession, setCurrentUser } from "@/lib/session";

const EMPTY_PROFILE = {
  userId: "",
  fullname: "",
  phone: "",
  gender: "",
  birthDate: "",
  address: "",
  avatarUrl: "",
};

const EMPTY_PASSWORD_FORM = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
};

function toProfileForm(user, profile) {
  return {
    userId: String(user?.id || profile?.id || ""),
    fullname: profile?.fullname || user?.fullname || "",
    phone: profile?.phone || "",
    gender: profile?.gender === true ? "true" : profile?.gender === false ? "false" : "",
    birthDate: profile?.birthDate || "",
    address: profile?.address || "",
    avatarUrl: profile?.avatarUrl || user?.avatarUrl || "",
  };
}

export default function ProfilePanel({ heading = "Thông tin cá nhân" }) {
  const router = useRouter();
  const avatarInputRef = useRef(null);
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(EMPTY_PROFILE);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [avatarMessage, setAvatarMessage] = useState("");
  const [securityOpen, setSecurityOpen] = useState(false);
  const [passwordForm, setPasswordForm] = useState(EMPTY_PASSWORD_FORM);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [logoutSaving, setLogoutSaving] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState("");
  const [message, setMessage] = useState("");

  async function loadProfile() {
    setLoading(true);
    setMessage("");
    try {
      const currentUser = await getMe();
      const nextProfile = await getUserProfile(currentUser.id);
      setUser(currentUser);
      setProfile(nextProfile);
      setForm(toProfileForm(currentUser, nextProfile));
      setCurrentUser({
        ...currentUser,
        fullname: nextProfile?.fullname || currentUser.fullname,
        avatarUrl: nextProfile?.avatarUrl || currentUser.avatarUrl,
      });
    } catch (error) {
      setMessage(error.message || "Không thể tải hồ sơ.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadProfile();
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function updatePasswordField(event) {
    const { name, value } = event.target;
    setPasswordForm((current) => ({ ...current, [name]: value }));
  }

  async function uploadAvatar(event) {
    const file = event.target.files?.[0];
    if (!file || avatarUploading) return;

    setAvatarUploading(true);
    setAvatarMessage("");
    try {
      const result = await uploadMedia(file, "USER_AVATAR");
      const avatarUrl = result?.secureUrl || "";
      setForm((current) => ({ ...current, avatarUrl }));
      setAvatarMessage("Đã tải ảnh lên. Bấm Lưu hồ sơ để cập nhật.");
    } catch (error) {
      setAvatarMessage(error.message || "Không thể tải ảnh đại diện.");
    } finally {
      setAvatarUploading(false);
      if (avatarInputRef.current) avatarInputRef.current.value = "";
    }
  }

  async function submitProfile(event) {
    event.preventDefault();
    if (saving || avatarUploading) return;

    setSaving(true);
    setMessage("");
    try {
      await updateUserProfile({
        userId: form.userId,
        fullname: form.fullname.trim(),
        phone: form.phone.trim() || null,
        gender: form.gender === "" ? null : form.gender === "true",
        birthDate: form.birthDate || null,
        address: form.address.trim(),
        avatarUrl: form.avatarUrl.trim(),
      });
      setMessage("Đã cập nhật hồ sơ.");
      await loadProfile();
    } catch (error) {
      setMessage(error.message || "Không thể cập nhật hồ sơ.");
    } finally {
      setSaving(false);
    }
  }

  async function submitPassword(event) {
    event.preventDefault();
    if (passwordSaving) return;

    setPasswordMessage("");
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordMessage("Mật khẩu mới và xác nhận mật khẩu mới không khớp.");
      return;
    }

    setPasswordSaving(true);
    try {
      await changePasswordDirect({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordForm(EMPTY_PASSWORD_FORM);
      setPasswordMessage("Đã đổi mật khẩu. Vui lòng đăng nhập lại ở các thiết bị khác nếu cần.");
    } catch (error) {
      setPasswordMessage(error.message || "Không thể đổi mật khẩu.");
    } finally {
      setPasswordSaving(false);
    }
  }

  async function signOut() {
    if (logoutSaving) return;
    setLogoutSaving(true);
    setMessage("");
    try {
      await logout();
    } catch {
      // Local session cleanup still keeps the UI consistent if the server session is already gone.
    } finally {
      clearSession();
      router.push("/");
      router.refresh();
    }
  }

  return (
    <section className="ops-panel wide profile-panel">
      <div className="ops-panel-head">
        <div>
          <h2>{heading}</h2>
        </div>
      </div>

      {loading ? <div className="status-box">Đang tải hồ sơ...</div> : null}
      {message ? <div className="status-box" aria-live="polite">{message}</div> : null}

      {!loading && user ? (
        <div className="profile-layout">
          <aside className="profile-summary">
            <div className="profile-avatar-wrap">
              <div className="profile-avatar-control">
                <UserAvatar className="profile-avatar" src={form.avatarUrl} label="Ảnh đại diện" />
                <button className="profile-avatar-button" type="button" onClick={() => avatarInputRef.current?.click()} disabled={avatarUploading} aria-label={avatarUploading ? "Đang tải ảnh đại diện" : "Đổi ảnh đại diện"} title={avatarUploading ? "Đang tải..." : "Đổi ảnh đại diện"}>
                  {avatarUploading ? "..." : "✎"}
                </button>
              </div>
              <input ref={avatarInputRef} className="visually-hidden" type="file" accept="image/*" onChange={uploadAvatar} />
              {avatarMessage ? <small className="profile-avatar-message" aria-live="polite">{avatarMessage}</small> : null}
            </div>
            <div className="profile-summary-info">
              <strong>{form.fullname || "Chưa cập nhật họ tên"}</strong>
              <span>{user.email}</span>
              <small>{roleLabel(user.role)}</small>
            </div>
            <button className="btn btn-ghost profile-logout-button" type="button" onClick={signOut} disabled={logoutSaving}>
              {logoutSaving ? "Đang đăng xuất..." : "Đăng xuất"}
            </button>
          </aside>

          <form className="ops-form profile-form" onSubmit={submitProfile}>
            <div className="profile-form-head">
              <div>
                <h3>Thông tin liên hệ</h3>
              </div>
            </div>
            <div className="form-grid">
              <label className="label">Họ và tên<input className="field" name="fullname" value={form.fullname} onChange={updateField} maxLength={40} /></label>
              <label className="label">Email<input className="field readonly-field" type="email" value={profile?.email || user.email || ""} readOnly disabled tabIndex={-1} aria-disabled="true" /></label>
              <label className="label">Số điện thoại<input className="field" name="phone" value={form.phone} onChange={updateField} placeholder="10 chữ số" pattern="\d{10}" /></label>
              <label className="label">Giới tính<select className="field" name="gender" value={form.gender} onChange={updateField}>
                <option value="">Chưa cập nhật</option>
                <option value="true">Nam</option>
                <option value="false">Nữ</option>
              </select></label>
              <label className="label">Ngày sinh<input className="field" name="birthDate" type="date" value={form.birthDate} onChange={updateField} /></label>
              <label className="label wide">Địa chỉ<textarea className="field" name="address" rows={3} value={form.address} onChange={updateField} maxLength={255} /></label>
            </div>
            <button className="btn btn-primary" type="submit" disabled={saving || avatarUploading}>
              {saving ? "Đang lưu..." : "Lưu hồ sơ"}
            </button>
          </form>

          <section className={`security-panel ${securityOpen ? "open" : ""}`}>
            <button className="security-toggle" type="button" onClick={() => setSecurityOpen((current) => !current)} aria-expanded={securityOpen} aria-label={securityOpen ? "Thu gọn đổi mật khẩu" : "Mở đổi mật khẩu"}>
              <span>
                <strong>Đổi mật khẩu</strong>
              </span>
              <span className="security-toggle-icon" aria-hidden="true">{securityOpen ? "⌃" : "⌄"}</span>
            </button>
            {passwordMessage ? <div className="status-box compact-status" aria-live="polite">{passwordMessage}</div> : null}
            {securityOpen ? (
              <form className="ops-form password-form" onSubmit={submitPassword}>
                <div className="form-grid">
                  <label className="label">Mật khẩu cũ<input className="field" name="currentPassword" type="password" value={passwordForm.currentPassword} onChange={updatePasswordField} required autoComplete="current-password" /></label>
                  <label className="label">Mật khẩu mới<input className="field" name="newPassword" type="password" value={passwordForm.newPassword} onChange={updatePasswordField} required minLength={8} maxLength={20} autoComplete="new-password" /></label>
                  <label className="label">Xác nhận mật khẩu mới<input className="field" name="confirmPassword" type="password" value={passwordForm.confirmPassword} onChange={updatePasswordField} required minLength={8} maxLength={20} autoComplete="new-password" /></label>
                </div>
                <button className="btn btn-secondary" type="submit" disabled={passwordSaving}>
                  {passwordSaving ? "Đang đổi..." : "Đổi mật khẩu"}
                </button>
              </form>
            ) : null}
          </section>
        </div>
      ) : null}
    </section>
  );
}
