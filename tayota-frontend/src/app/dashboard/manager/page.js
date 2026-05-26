import ManagerDashboard from "@/components/dashboard/ManagerDashboard";

export const metadata = {
  title: "Manager Dashboard | Tayota",
};

export default function ManagerDashboardPage() {
  return (
    <section className="section ops-page manager-dashboard-page">
      <div className="manager-dashboard-shell">
        <ManagerDashboard />
      </div>
    </section>
  );
}
