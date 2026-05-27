import { Suspense } from "react";
import AdvisorDashboard from "@/components/dashboard/AdvisorDashboard";

export const metadata = {
  title: "Advisor Dashboard | Tayota",
};

export default function AdvisorDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container">
        <Suspense fallback={<div className="status-box">Đang tải dashboard...</div>}>
          <AdvisorDashboard />
        </Suspense>
      </div>
    </section>
  );
}
