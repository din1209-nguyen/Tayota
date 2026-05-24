import AdminDashboard from "@/components/dashboard/AdminDashboard";

export const metadata = {
  title: "Admin Dashboard | TAYOTA",
};

export default function AdminDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Quản trị hệ thống</h1>
      </div>
      <div className="shell-container">
        <AdminDashboard />
      </div>
    </section>
  );
}
