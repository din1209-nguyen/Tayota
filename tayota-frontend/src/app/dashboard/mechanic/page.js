import { Suspense } from "react";
import MechanicDashboard from "@/components/dashboard/MechanicDashboard";

export const metadata = {
  title: "Mechanic Dashboard | Tayota",
};

export default function MechanicDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container">
        <Suspense fallback={<div className="status-box">Đang tải dashboard...</div>}>
          <MechanicDashboard />
        </Suspense>
      </div>
    </section>
  );
}
