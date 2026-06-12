import { Suspense } from "react";
import AdminDashboard from "@/components/dashboard/AdminDashboard";

export const metadata = {
  title: "Quản trị hệ thống | Tayota",
};

export default function AdminDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container admin-shell-container">
        <Suspense fallback={<div className="status-box">Đang tải dashboard...</div>}>
          <AdminDashboard />
        </Suspense>
      </div>
    </section>
  );
}
