import AdminDashboard from "@/components/dashboard/AdminDashboard";

export const metadata = {
  title: "Admin Dashboard | Tayota",
};

export default function AdminDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container admin-shell-container">
        <AdminDashboard />
      </div>
    </section>
  );
}
