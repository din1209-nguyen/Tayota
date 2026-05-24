"use client";

import Link from "next/link";
import { useState } from "react";
import { apiFetch } from "@/lib/api";
import { setAccessToken } from "@/lib/session";

export default function AuthForm({ mode }) {
  const isRegister = mode === "register";
  const [form, setForm] = useState({ fullName: "", email: "", password: "", phone: "" });
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    if (loading) return;
    setLoading(true);
    setMessage("");
    try {
      const result = await apiFetch(isRegister ? "/user/register" : "/user/login", {
        method: "POST",
        body: JSON.stringify(form),
      });
      if (result?.accessToken) setAccessToken(result.accessToken);
      setMessage(isRegister ? "Đăng ký thành công. Vui lòng kiểm tra email nếu cần xác thực." : "Đăng nhập thành công.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="form-panel auth-form" onSubmit={submit}>
      {isRegister ? (
        <>
          <label className="label">
            Họ và tên
            <input className="field" name="fullName" value={form.fullName} onChange={updateField} />
          </label>
          <label className="label">
            Số điện thoại
            <input className="field" name="phone" value={form.phone} onChange={updateField} />
          </label>
        </>
      ) : null}
      <label className="label">
        Email
        <input className="field" type="email" name="email" value={form.email} onChange={updateField} required />
      </label>
      <label className="label">
        Mật khẩu
        <input className="field" type="password" name="password" value={form.password} onChange={updateField} required />
      </label>
      {message ? <div className="status-box">{message}</div> : null}
      <button className="btn btn-primary" type="submit" disabled={loading}>
        {loading ? "Đang xử lý..." : isRegister ? "Tạo tài khoản" : "Đăng nhập"}
      </button>
      <Link className="auth-switch" href={isRegister ? "/auth/login" : "/auth/register"}>
        {isRegister ? "Đã có tài khoản" : "Tạo tài khoản mới"}
      </Link>
    </form>
  );
}
