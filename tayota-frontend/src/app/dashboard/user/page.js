import { Suspense } from "react";
import CustomerDashboard from "@/components/dashboard/CustomerDashboard";

export const metadata = {
  title: "User Dashboard | Tayota",
};

export default function UserDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container">
        <Suspense fallback={<div className="status-box">Đang tải dashboard...</div>}>
          <CustomerDashboard />
        </Suspense>
      </div>
    </section>
  );
}
