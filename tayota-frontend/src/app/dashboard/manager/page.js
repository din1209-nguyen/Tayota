import ManagerDashboard from "@/components/dashboard/ManagerDashboard";

export const metadata = {
  title: "Manager Dashboard | Tayota",
};

export default function ManagerDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Quản lý nghiệp vụ</h1>
      </div>
      <div className="shell-container">
        <ManagerDashboard />
      </div>
    </section>
  );
}
