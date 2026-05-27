import AssistantDashboard from "@/components/dashboard/AssistantDashboard";

export const metadata = {
  title: "Assistant Dashboard | Tayota",
};

export default function AssistantDashboardPage() {
  return (
    <section className="section ops-page">
      <div className="shell-container">
        <AssistantDashboard />
      </div>
    </section>
  );
}
