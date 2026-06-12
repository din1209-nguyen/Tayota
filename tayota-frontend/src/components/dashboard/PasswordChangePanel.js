"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import PasswordInput from "@/components/PasswordInput";
import { changePasswordDirect, getMe } from "@/lib/services/auth";
import { getDashboardPath, setCurrentUser } from "@/lib/session";

const EMPTY_FORM = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
};

export default function PasswordChangePanel() {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const profileHref = useMemo(() => {
    const dashboardPath = getDashboardPath(user?.role);
    return `${dashboardPath}?tab=profile`;
  }, [user?.role]);

  useEffect(() => {
    let active = true;
    getMe()
      .then((currentUser) => {
        if (!active) return;
        setUser(currentUser);
        setCurrentUser(currentUser);
      })
      .catch(() => {
        if (!active) return;
        router.replace("/auth/login");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [router]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submitPassword(event) {
    event.preventDefault();
    if (saving) return;

    setError("");
    setMessage("");
    if (form.newPassword !== form.confirmPassword) {
      setError("Mật khẩu mới và xác nhận mật khẩu mới không khớp.");
      return;
    }

    setSaving(true);
    try {
      await changePasswordDirect({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });
      setForm(EMPTY_FORM);
      setMessage("Đã đổi mật khẩu thành công.");
    } catch (caughtError) {
      setError(caughtError.message || "Không thể đổi mật khẩu.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="password-change-auth">
      <div className="password-change-copy">
        <p className="eyebrow">Tài khoản</p>
        <h1>Đổi mật khẩu</h1>
        <p>Cập nhật mật khẩu định kỳ để bảo vệ lịch hẹn, hồ sơ xe và hội thoại tư vấn của bạn.</p>
        <Link className="auth-switch" href={profileHref}>Quay lại hồ sơ</Link>
      </div>

      <form className="form-panel auth-form password-change-form" onSubmit={submitPassword}>
        <span className="eyebrow">Bảo mật</span>
        <h2>Cập nhật mật khẩu</h2>
        <p className="form-hint">Dùng mật khẩu mạnh từ 8-20 ký tự và không chia sẻ cho người khác.</p>

        {loading ? <div className="status-box">Đang kiểm tra phiên đăng nhập...</div> : null}
        {error ? <div className="form-alert error" aria-live="polite">{error}</div> : null}
        {message ? <div className="status-box success" aria-live="polite">{message}</div> : null}

        {!loading && user ? (
          <>
            <label className="label">
              Mật khẩu hiện tại
              <PasswordInput name="currentPassword" value={form.currentPassword} onChange={updateField} required autoComplete="current-password" />
            </label>
            <label className="label">
              Mật khẩu mới
              <PasswordInput name="newPassword" value={form.newPassword} onChange={updateField} required minLength={8} maxLength={20} autoComplete="new-password" />
            </label>
            <label className="label">
              Xác nhận mật khẩu mới
              <PasswordInput name="confirmPassword" value={form.confirmPassword} onChange={updateField} required minLength={8} maxLength={20} autoComplete="new-password" />
            </label>
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu mật khẩu"}
            </button>
            <Link className="auth-switch" href={profileHref}>Hủy và quay lại hồ sơ</Link>
          </>
        ) : null}
      </form>
    </section>
  );
}
