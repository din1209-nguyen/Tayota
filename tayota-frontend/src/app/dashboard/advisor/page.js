import AdvisorDashboard from "@/components/dashboard/AdvisorDashboard";

export const metadata = {
  title: "Advisor Dashboard | Tayota",
};

export default function AdvisorDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Cố vấn dịch vụ</h1>
      </div>
      <div className="shell-container">
        <AdvisorDashboard />
      </div>
    </section>
  );
}
