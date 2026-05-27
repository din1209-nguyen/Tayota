"use client";

import { useEffect, useState } from "react";
import { getMe } from "@/lib/services/auth";
import { getUserProfile, updateUserProfile } from "@/lib/services/user";
import { roleLabel } from "@/lib/format";
import { setCurrentUser } from "@/lib/session";

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

export default function ProfilePanel({ eyebrow = "Hồ sơ", heading = "Thông tin cá nhân" }) {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(EMPTY_PROFILE);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
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

  async function submitProfile(event) {
    event.preventDefault();
    if (saving) return;

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

  return (
    <section className="ops-panel wide profile-panel">
      <div className="ops-panel-head">
        <div>
          <p className="eyebrow">{eyebrow}</p>
          <h2>{heading}</h2>
        </div>
        <button className="btn btn-ghost" type="button" onClick={loadProfile} disabled={loading || saving}>
          Tải lại
        </button>
      </div>

      {loading ? <div className="status-box">Đang tải hồ sơ...</div> : null}
      {message ? <div className="status-box" aria-live="polite">{message}</div> : null}

      {!loading && user ? (
        <div className="profile-layout">
          <aside className="profile-summary">
            <span className="profile-avatar" style={form.avatarUrl ? { backgroundImage: `url(${form.avatarUrl})` } : undefined}>
              {form.avatarUrl ? "" : (form.fullname || user.email || "T").trim().slice(0, 1).toUpperCase()}
            </span>
            <div>
              <strong>{form.fullname || "Chưa cập nhật họ tên"}</strong>
              <span>{user.email}</span>
              <small>{roleLabel(user.role)}</small>
            </div>
          </aside>

          <form className="ops-form profile-form" onSubmit={submitProfile}>
            <div className="form-grid">
              <label className="label">Họ và tên<input className="field" name="fullname" value={form.fullname} onChange={updateField} maxLength={40} /></label>
              <label className="label">Email<input className="field" type="email" value={profile?.email || user.email || ""} readOnly /></label>
              <label className="label">Số điện thoại<input className="field" name="phone" value={form.phone} onChange={updateField} placeholder="10 chữ số" pattern="\d{10}" /></label>
              <label className="label">Giới tính<select className="field" name="gender" value={form.gender} onChange={updateField}>
                <option value="">Chưa cập nhật</option>
                <option value="true">Nam</option>
                <option value="false">Nữ</option>
              </select></label>
              <label className="label">Ngày sinh<input className="field" name="birthDate" type="date" value={form.birthDate} onChange={updateField} /></label>
              <label className="label">Ảnh đại diện URL<input className="field" name="avatarUrl" value={form.avatarUrl} onChange={updateField} placeholder="https://..." maxLength={1024} /></label>
              <label className="label wide">Địa chỉ<textarea className="field" name="address" rows={3} value={form.address} onChange={updateField} maxLength={255} /></label>
            </div>
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu hồ sơ"}
            </button>
          </form>
        </div>
      ) : null}
    </section>
  );
}
