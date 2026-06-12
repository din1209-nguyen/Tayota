"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getMe } from "@/lib/services/auth";
import { getDashboardPath, setCurrentUser } from "@/lib/session";

export default function DashboardRedirect() {
  const router = useRouter();
  const [message, setMessage] = useState("Đang kiểm tra vai trò...");

  useEffect(() => {
    let alive = true;

    async function run() {
      try {
        const user = await getMe();
        if (!alive) return;
        setCurrentUser(user);
        router.replace(getDashboardPath(user?.role));
      } catch (error) {
        if (!alive) return;
        setMessage(error.message || "Phiên đăng nhập đã hết hạn.");
        router.replace("/auth/login");
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
        <p className="eyebrow">Bảng điều khiển</p>
        <h1>Điều hướng theo vai trò</h1>
      </div>
      <div className="shell-container dashboard-role-grid">
        <div className="status-box wide">{message}</div>
        <Link className="role-card" href="/dashboard/admin">Quản trị viên</Link>
        <Link className="role-card" href="/dashboard/manager">Quản lý</Link>
        <Link className="role-card" href="/dashboard/advisor">Cố vấn</Link>
        <Link className="role-card" href="/dashboard/assistant">Tư vấn viên</Link>
        <Link className="role-card" href="/dashboard/mechanic">Kỹ thuật viên</Link>
        <Link className="role-card" href="/dashboard/user">Khách hàng</Link>
      </div>
    </section>
  );
}
