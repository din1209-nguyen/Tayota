import { ManagerVehicleEditorPage } from "@/components/dashboard/ManagerContentPanels";

export const metadata = {
  title: "Thêm xe | Manager Dashboard",
};

export default function NewManagerVehiclePage() {
  return (
    <section className="section ops-page manager-dashboard-page">
      <div className="manager-dashboard-shell">
        <ManagerVehicleEditorPage vehicleId="new" />
      </div>
    </section>
  );
}
