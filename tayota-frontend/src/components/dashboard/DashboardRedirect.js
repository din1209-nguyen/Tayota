"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getMe } from "@/lib/services/auth";
import { getAccessToken, getDashboardPath, setCurrentUser } from "@/lib/session";

export default function DashboardRedirect() {
  const router = useRouter();
  const [message, setMessage] = useState("Đang kiểm tra vai trò...");

  useEffect(() => {
    let alive = true;

    async function run() {
      if (!getAccessToken()) {
        router.replace("/auth/login");
        return;
      }

      try {
        const user = await getMe();
        if (!alive) return;
        setCurrentUser(user);
        router.replace(getDashboardPath(user?.role));
      } catch (error) {
        if (alive) setMessage(error.message || "Không thể xác định vai trò tài khoản.");
      }
    }

    run();
    return () => {
      alive = false;
    };
  }, [router]);

  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Điều hướng theo vai trò</h1>
      </div>
      <div className="shell-container dashboard-role-grid">
        <div className="status-box wide">{message}</div>
        <Link className="role-card" href="/dashboard/admin">Admin</Link>
        <Link className="role-card" href="/dashboard/advisor">Cố vấn</Link>
        <Link className="role-card" href="/dashboard/assistant">Assistant</Link>
        <Link className="role-card" href="/dashboard/mechanic">Kỹ thuật viên</Link>
        <Link className="role-card" href="/dashboard/user">Khách hàng</Link>
      </div>
    </section>
  );
}
