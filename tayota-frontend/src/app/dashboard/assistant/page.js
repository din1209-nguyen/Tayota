import { Suspense } from "react";
import AssistantDashboard from "@/components/dashboard/AssistantDashboard";

export const metadata = {
  title: "Assistant Dashboard | Tayota",
};

export default function AssistantDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="assistant-dashboard-shell">
        <Suspense fallback={<div className="status-box">Đang tải dashboard...</div>}>
          <AssistantDashboard />
        </Suspense>
      </div>
    </section>
  );
}
