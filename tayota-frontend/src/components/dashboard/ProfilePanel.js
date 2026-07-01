"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import UserAvatar from "@/components/UserAvatar";
import { getMe, logout } from "@/lib/services/auth";
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

function PencilIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
    </svg>
  );
}

export default function ProfilePanel({ heading = "Hồ sơ cá nhân" }) {
  const router = useRouter();
  const avatarInputRef = useRef(null);
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(EMPTY_PROFILE);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [avatarMessage, setAvatarMessage] = useState("");
  const [logoutSaving, setLogoutSaving] = useState(false);
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

  async function signOut() {
    if (logoutSaving) return;
    setLogoutSaving(true);
    setMessage("");
    try {
      await logout();
    } catch {
      // Local cleanup keeps the UI consistent if the server session is already gone.
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
                <button
                  className="profile-avatar-button"
                  type="button"
                  onClick={() => avatarInputRef.current?.click()}
                  disabled={avatarUploading}
                  aria-label={avatarUploading ? "Đang tải ảnh đại diện" : "Đổi ảnh đại diện"}
                  title={avatarUploading ? "Đang tải..." : "Đổi ảnh đại diện"}
                >
                  {avatarUploading ? "..." : <PencilIcon />}
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

            <Link className="btn profile-password-button" href="/dashboard/change-password">
              Đổi mật khẩu
            </Link>

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
        </div>
      ) : null}
    </section>
  );
}
