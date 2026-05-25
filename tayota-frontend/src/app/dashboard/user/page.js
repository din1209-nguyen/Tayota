import CustomerDashboard from "@/components/dashboard/CustomerDashboard";

export const metadata = {
  title: "User Dashboard | Tayota",
};

export default function UserDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Không gian người dùng</h1>
      </div>
      <div className="shell-container">
        <CustomerDashboard />
      </div>
    </section>
  );
}
