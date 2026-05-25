"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import StaffChatWorkspace from "@/components/dashboard/StaffChatWorkspace";
import ManagerContentPanels from "@/components/dashboard/ManagerContentPanels";
import { getMe } from "@/lib/services/auth";
import { getDashboardPath, setCurrentUser } from "@/lib/session";

const TABS = [
  ["chat", "Live Chat"],
  ["vehicles", "Xe"],
  ["articles", "Bài viết"],
  ["dealerships", "Đại lý"],
  ["accessories", "Phụ kiện"],
  ["users", "Người dùng"],
];

export default function ManagerDashboard() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [tab, setTab] = useState("chat");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;
    getMe()
      .then((user) => {
        if (!active) return;
        setCurrentUser(user);
        if (user?.role !== "MANAGER") {
          router.replace(getDashboardPath(user?.role));
          return;
        }
        setReady(true);
      })
      .catch((error) => {
        if (!active) return;
        setMessage(error.message || "Phiên đăng nhập đã hết hạn.");
        router.replace("/auth/login");
      });
    return () => {
      active = false;
    };
  }, [router]);

  if (!ready) {
    return <div className="status-box" aria-live="polite">{message || "Đang kiểm tra quyền truy cập..."}</div>;
  }

  return (
    <div className="admin-dashboard manager-dashboard">
      <header className="admin-workspace-header">
        <div>
          <p className="eyebrow">Dashboard / Manager</p>
          <h1>Quản lý nội dung website</h1>
          <p className="admin-workspace-copy">Cập nhật catalog, tin tức, đại lý, phụ kiện và tư vấn khách hàng.</p>
        </div>
        <nav className="role-tabs admin-role-tabs manager-tabs" aria-label="Các mục dành cho manager">
          {TABS.map(([value, label]) => (
            <button className={tab === value ? "active" : ""} key={value} type="button" onClick={() => setTab(value)}>
              {label}
            </button>
          ))}
        </nav>
      </header>
      {tab === "chat" ? (
        <StaffChatWorkspace
          eyebrow="Manager"
          heading="Tư vấn khách hàng trực tuyến"
          emptyPanelMessage="Chọn phiên đang chờ để hỗ trợ khách hàng."
        />
      ) : <ManagerContentPanels tab={tab} />}
    </div>
  );
}
