import MechanicDashboard from "@/components/dashboard/MechanicDashboard";

export const metadata = {
  title: "Mechanic Dashboard | TAYOTA",
};

export default function MechanicDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Kỹ thuật viên</h1>
      </div>
      <div className="shell-container">
        <MechanicDashboard />
      </div>
    </section>
  );
}
