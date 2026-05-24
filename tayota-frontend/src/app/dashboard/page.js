import Link from "next/link";
import CustomerDashboard from "@/components/dashboard/CustomerDashboard";

export const metadata = {
  title: "Dashboard | TAYOTA",
};

export default function DashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Không gian khách hàng</h1>
        <div className="ops-nav">
          <Link className="btn btn-ghost" href="/dashboard/advisor">Advisor</Link>
          <Link className="btn btn-ghost" href="/dashboard/mechanic">Mechanic</Link>
          <Link className="btn btn-ghost" href="/dashboard/admin">Admin</Link>
        </div>
      </div>
      <div className="shell-container">
        <CustomerDashboard />
      </div>
    </section>
  );
}
