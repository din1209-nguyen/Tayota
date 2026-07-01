import { ManagerVehicleEditorPage } from "@/components/dashboard/ManagerContentPanels";

export const metadata = {
  title: "Sửa xe | Trang quản lý",
};

export default async function EditManagerVehiclePage({ params }) {
  const { id } = await params;

  return (
    <section className="section ops-page manager-dashboard-page">
      <div className="manager-dashboard-shell">
        <ManagerVehicleEditorPage vehicleId={id} />
      </div>
    </section>
  );
}
