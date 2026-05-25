import AssistantDashboard from "@/components/dashboard/AssistantDashboard";

export const metadata = {
  title: "Assistant Dashboard | Tayota",
};

export default function AssistantDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Dashboard</p>
        <h1>Trung tâm live chat</h1>
      </div>
      <div className="shell-container">
        <AssistantDashboard />
      </div>
    </section>
  );
}
