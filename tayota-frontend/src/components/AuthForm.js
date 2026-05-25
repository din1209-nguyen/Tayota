"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { getMe, login, register } from "@/lib/services/auth";
import { setAccessToken, setCurrentUser } from "@/lib/session";

const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/;

export default function AuthForm({ mode }) {
  const router = useRouter();
  const isRegister = mode === "register";
  const [form, setForm] = useState({ email: "", password: "", confirmPassword: "" });
  const [message, setMessage] = useState("");
  const [registeredEmail, setRegisteredEmail] = useState("");
  const [loading, setLoading] = useState(false);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    if (loading) return;

    const payload = {
      email: form.email.trim(),
      password: form.password,
    };

    if (isRegister && form.password !== form.confirmPassword) {
      setMessage("Mật khẩu xác nhận không khớp.");
      return;
    }

    if (isRegister && !PASSWORD_PATTERN.test(form.password)) {
      setMessage("Mật khẩu cần 8-20 ký tự, có chữ hoa, số và ký tự đặc biệt.");
      return;
    }

    setLoading(true);
    setMessage("");

    try {
      const result = isRegister ? await register(payload) : await login(payload);
      if (isRegister) {
        setRegisteredEmail(payload.email);
        return;
      }

      if (result?.accessToken) setAccessToken(result.accessToken);
      const user = await getMe();
      setCurrentUser(user);
      router.push("/");
      router.refresh();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  if (registeredEmail) {
    return (
      <section className="form-panel auth-success">
        <span className="eyebrow">Tài khoản đã được tạo</span>
        <h2>Kiểm tra email của bạn</h2>
        <p>
          Chúng tôi đã gửi liên kết xác thực đến <strong>{registeredEmail}</strong>. Mở liên kết đó để kích hoạt tài
          khoản trước khi đăng nhập.
        </p>
        <div className="auth-actions">
          <Link className="btn btn-primary" href="/auth/login">
            Về trang đăng nhập
          </Link>
          <button className="btn btn-secondary" type="button" onClick={() => setRegisteredEmail("")}>
            Đăng ký email khác
          </button>
        </div>
      </section>
    );
  }

  return (
    <form className="form-panel auth-form" onSubmit={submit}>
      <label className="label">
        Email
        <input className="field" type="email" name="email" value={form.email} onChange={updateField} required />
      </label>

      <label className="label">
        Mật khẩu
        <input
          className="field"
          type="password"
          name="password"
          value={form.password}
          onChange={updateField}
          minLength={8}
          required
        />
      </label>

      {isRegister ? (
        <label className="label">
          Xác nhận mật khẩu
          <input
            className="field"
            type="password"
            name="confirmPassword"
            value={form.confirmPassword}
            onChange={updateField}
            minLength={8}
            required
          />
        </label>
      ) : null}

      {isRegister ? <p className="form-hint">Mật khẩu cần 8-20 ký tự, gồm chữ hoa, số và ký tự đặc biệt.</p> : null}

      {message ? <div className="status-box">{message}</div> : null}

      <button className="btn btn-primary" type="submit" disabled={loading}>
        {loading ? "Đang xử lý..." : isRegister ? "Tạo tài khoản" : "Đăng nhập"}
      </button>

      <Link className="auth-switch" href={isRegister ? "/auth/login" : "/auth/register"}>
        {isRegister ? "Đã có tài khoản? Đăng nhập" : "Chưa có tài khoản? Đăng ký"}
      </Link>
    </form>
  );
}
