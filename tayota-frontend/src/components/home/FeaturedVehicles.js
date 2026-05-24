import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { unwrapList } from "@/lib/format";
import VehicleCard from "@/components/vehicles/VehicleCard";

async function getVehicles() {
  try {
    const data = await apiFetch("/car/catalog/car-versions?page=0&size=3", {
      cache: "no-store",
    });
    return { vehicles: unwrapList(data), error: null };
  } catch (error) {
    return { vehicles: [], error: error.message };
  }
}

export default async function FeaturedVehicles() {
  const { vehicles, error } = await getVehicles();

  return (
    <section className="section">
      <div className="shell-container section-heading">
        <div>
          <p className="eyebrow">Bộ sưu tập TAYOTA</p>
          <h2>Những phiên bản đang được quan tâm</h2>
        </div>
        <Link className="btn btn-ghost" href="/vehicles">
          Xem tất cả
        </Link>
      </div>
      <div className="shell-container vehicle-grid">
        {error ? (
          <div className="status-box wide">
            Chưa kết nối được gateway catalog. Vui lòng kiểm tra
            NEXT_PUBLIC_API_BASE_URL hoặc khởi động backend.
          </div>
        ) : null}
        {!error && vehicles.length === 0 ? (
          <div className="status-box wide">
            Hiện chưa có xe phù hợp trong catalog.
          </div>
        ) : null}
        {vehicles.map((vehicle, index) => (
          <VehicleCard key={vehicle?.id || vehicle?.carVersionId || index} vehicle={vehicle} />
        ))}
      </div>
    </section>
  );
}
