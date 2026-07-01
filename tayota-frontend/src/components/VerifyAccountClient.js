"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { verifyAccount } from "@/lib/services/auth";

export default function VerifyAccountClient() {
  const searchParams = useSearchParams();
  const email = searchParams.get("email") || "";
  const token = searchParams.get("token") || "";
  const [state, setState] = useState({ status: "loading", message: "Đang xác thực tài khoản..." });

  useEffect(() => {
    let alive = true;

    async function run() {
      if (!email || !token) {
        setState({
          status: "error",
          message: "Liên kết xác thực thiếu email hoặc token. Vui lòng kiểm tra lại email xác thực.",
        });
        return;
      }

      try {
        await verifyAccount({ email, token });
        if (!alive) return;
        setState({
          status: "success",
          message: "Tài khoản đã được xác thực. Bạn có thể đăng nhập và sử dụng đầy đủ dịch vụ.",
        });
      } catch (error) {
        if (!alive) return;
        setState({
          status: "error",
          message: error.message || "Không thể xác thực tài khoản. Liên kết có thể đã hết hạn.",
        });
      }
    }

    run();
    return () => {
      alive = false;
    };
  }, [email, token]);

  return (
    <section className="auth-shell">
      <div className="auth-copy">
        <span className="eyebrow">Xác thực tài khoản</span>
        <h1>{state.status === "success" ? "Tài khoản đã sẵn sàng" : "Xác nhận email của bạn"}</h1>
        <p>
          Tayota bảo vệ tài khoản bằng xác thực email trước khi mở các tính năng đặt lịch, chăm sóc xe và hỗ trợ cá
          nhân.
        </p>
      </div>

      <div className={`form-panel verify-card verify-card-${state.status}`}>
        <span className="verify-indicator" aria-hidden="true" />
        <h2>{state.status === "loading" ? "Đang kiểm tra" : state.status === "success" ? "Thành công" : "Chưa xác thực"}</h2>
        <p>{state.message}</p>
        <div className="auth-actions">
          <Link className="btn btn-primary" href="/auth/login">
            Đăng nhập
          </Link>
          {state.status === "error" ? (
            <Link className="btn btn-secondary" href="/auth/register">
              Đăng ký lại
            </Link>
          ) : null}
        </div>
      </div>
    </section>
  );
}
