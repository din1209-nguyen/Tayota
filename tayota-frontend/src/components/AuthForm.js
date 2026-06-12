"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  getMe,
  login,
  register,
  resetForgotPassword,
  sendForgotPasswordOtp,
  verifyForgotPasswordOtp,
} from "@/lib/services/auth";
import { mergeCurrentChatSession } from "@/lib/services/chat";
import { setAccessToken, setCurrentUser } from "@/lib/session";
import PasswordInput from "./PasswordInput";

const PASSWORD_PATTERN = /^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,20}$/;

export default function AuthForm({ mode }) {
  const router = useRouter();
  const isRegister = mode === "register";
  const [form, setForm] = useState({ email: "", password: "", confirmPassword: "" });
  const [forgotStep, setForgotStep] = useState("");
  const [forgotForm, setForgotForm] = useState({ email: "", otp: "", token: "", newPassword: "", confirmPassword: "" });
  const [message, setMessage] = useState("");
  const [registeredEmail, setRegisteredEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const forgotInfoMessages = new Set([
    "Mã OTP đã được gửi đến email của bạn.",
    "OTP hợp lệ. Bạn có thể đặt mật khẩu mới.",
  ]);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  function updateForgotField(event) {
    setForgotForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  function openForgotPassword() {
    setMessage("");
    setForgotStep("email");
    setForgotForm((current) => ({ ...current, email: form.email.trim() }));
  }

  function backToLogin() {
    setMessage("");
    setForgotStep("");
    setForgotForm({ email: "", otp: "", token: "", newPassword: "", confirmPassword: "" });
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
      if (["USER", "CUSTOMER"].includes(user?.role)) {
        await mergeCurrentChatSession().catch(() => {});
      }
      router.push("/");
      router.refresh();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function submitForgotPassword(event) {
    event.preventDefault();
    if (loading) return;

    setLoading(true);
    setMessage("");

    try {
      if (forgotStep === "email") {
        const email = forgotForm.email.trim();
        await sendForgotPasswordOtp(email);
        setForgotForm((current) => ({ ...current, email }));
        setForgotStep("otp");
        setMessage("Mã OTP đã được gửi đến email của bạn.");
        return;
      }

      if (forgotStep === "otp") {
        const result = await verifyForgotPasswordOtp({
          email: forgotForm.email.trim(),
          otp: forgotForm.otp.trim(),
        });
        setForgotForm((current) => ({ ...current, token: result?.token || "" }));
        setForgotStep("password");
        setMessage("OTP hợp lệ. Bạn có thể đặt mật khẩu mới.");
        return;
      }

      if (forgotStep === "password") {
        if (forgotForm.newPassword !== forgotForm.confirmPassword) {
          setMessage("Mật khẩu xác nhận không khớp.");
          return;
        }
        if (!PASSWORD_PATTERN.test(forgotForm.newPassword)) {
          setMessage("Mật khẩu cần 8-20 ký tự, có chữ hoa, số và ký tự đặc biệt.");
          return;
        }
        await resetForgotPassword({
          email: forgotForm.email.trim(),
          token: forgotForm.token,
          newPassword: forgotForm.newPassword,
        });
        setForgotStep("success");
        setMessage("");
      }
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

  if (!isRegister && forgotStep) {
    return (
      <form className="form-panel auth-form" onSubmit={submitForgotPassword}>
        <span className="eyebrow">Khôi phục tài khoản</span>
        <h2>Quên mật khẩu</h2>

        {forgotStep === "email" ? (
          <label className="label">
            Email
            <input className="field" type="email" name="email" value={forgotForm.email} onChange={updateForgotField} required />
          </label>
        ) : null}

        {forgotStep === "otp" ? (
          <>
            <p className="form-hint">Nhập mã OTP đã gửi đến {forgotForm.email}.</p>
            <label className="label">
              OTP
              <input className="field" name="otp" value={forgotForm.otp} onChange={updateForgotField} required />
            </label>
          </>
        ) : null}

        {forgotStep === "password" ? (
          <>
            <p className="form-hint">Mật khẩu cần 8-20 ký tự, gồm chữ hoa, số và ký tự đặc biệt.</p>
            <label className="label">
              Mật khẩu mới
              <PasswordInput name="newPassword" value={forgotForm.newPassword} onChange={updateForgotField} minLength={8} required />
            </label>
            <label className="label">
              Xác nhận mật khẩu
              <PasswordInput name="confirmPassword" value={forgotForm.confirmPassword} onChange={updateForgotField} minLength={8} required />
            </label>
          </>
        ) : null}

        {forgotStep === "success" ? (
          <div className="status-box">
            Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.
          </div>
        ) : null}

        {message ? (
          <div className={forgotInfoMessages.has(message) ? "status-box" : "form-alert error"}>{message}</div>
        ) : null}

        {forgotStep !== "success" ? (
          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? "Đang xử lý..." : forgotStep === "email" ? "Gửi OTP" : forgotStep === "otp" ? "Xác thực OTP" : "Đặt lại mật khẩu"}
          </button>
        ) : null}

        <button className="auth-switch button-link" type="button" onClick={backToLogin}>
          Về trang đăng nhập
        </button>
      </form>
    );
  }

  return (
    <form className="form-panel auth-form" onSubmit={submit}>
      <span className="eyebrow">{isRegister ? "Thành viên mới" : "Tài khoản"}</span>
      <h2>{isRegister ? "Tạo tài khoản" : "Truy cập tài khoản"}</h2>
      <p className="form-hint">
        {isRegister
          ? "Dùng email và mật khẩu mạnh từ 8-20 ký tự để bảo vệ tài khoản."
          : "Dùng email và mật khẩu đã đăng ký để tiếp tục."}
      </p>

      <label className="label">
        Email
        <input className="field" type="email" name="email" value={form.email} onChange={updateField} required />
      </label>

      <label className="label">
        Mật khẩu
        <PasswordInput
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
          <PasswordInput
            name="confirmPassword"
            value={form.confirmPassword}
            onChange={updateField}
            minLength={8}
            required
          />
        </label>
      ) : null}

      {isRegister ? <p className="form-hint">Mật khẩu cần 8-20 ký tự, gồm chữ hoa, số và ký tự đặc biệt.</p> : null}

      {message ? <div className="form-alert error">{message}</div> : null}

      <button className="btn btn-primary" type="submit" disabled={loading}>
        {loading ? "Đang xử lý..." : isRegister ? "Tạo tài khoản" : "Đăng nhập"}
      </button>

      {!isRegister ? (
        <button className="auth-switch button-link" type="button" onClick={openForgotPassword}>
          Quên mật khẩu?
        </button>
      ) : null}

      <Link className="auth-switch" href={isRegister ? "/auth/login" : "/auth/register"}>
        {isRegister ? "Đã có tài khoản? Đăng nhập" : "Chưa có tài khoản? Đăng ký"}
      </Link>
    </form>
  );
}
