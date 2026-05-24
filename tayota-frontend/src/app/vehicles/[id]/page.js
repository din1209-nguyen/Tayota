import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { formatVnd, getVehicleName, getVehiclePrice } from "@/lib/format";
import SpecificationTable from "@/components/vehicles/SpecificationTable";

async function getDetail(id) {
  try {
    const vehicle = await apiFetch(`/car/catalog/car-versions/${id}`, { cache: "no-store" });
    let specification = null;
    try {
      specification = await apiFetch(`/car/catalog/car-versions/${id}/specification`, {
        cache: "no-store",
      });
    } catch {
      specification = null;
    }
    return { vehicle, specification, error: null };
  } catch (error) {
    return { vehicle: null, specification: null, error: error.message };
  }
}

export default async function VehicleDetailPage({ params }) {
  const { id } = await params;
  const { vehicle, specification, error } = await getDetail(id);

  if (error) {
    return (
      <section className="section">
        <div className="shell-container status-box">{error}</div>
      </section>
    );
  }

  return (
    <section className="detail-page">
      <div className="detail-hero">
        <div className="shell-container detail-hero-inner">
          <div>
            <p className="eyebrow">Chi tiết phiên bản</p>
            <h1>{getVehicleName(vehicle)}</h1>
            <p>{vehicle?.description || "Thiết kế tinh gọn, vận hành êm và sẵn sàng cho mọi hành trình riêng."}</p>
          </div>
          <div className="detail-price">
            <span>Giá tham khảo</span>
            <strong>{formatVnd(getVehiclePrice(vehicle))}</strong>
            <Link className="btn btn-secondary" href={`/appointments/test-drive?carVersionId=${id}`}>
              Đặt lái thử
            </Link>
          </div>
        </div>
      </div>
      <div className="shell-container detail-content">
        <div>
          <p className="eyebrow">Tổng quan</p>
          <h2>Thông số và trang bị</h2>
        </div>
        <SpecificationTable specification={specification} />
      </div>
      <div className="mobile-sticky-cta">
        <Link className="btn btn-primary" href={`/appointments/test-drive?carVersionId=${id}`}>
          Đặt lái thử
        </Link>
        <Link className="btn btn-ghost" href="/#ai-chat">
          Tư vấn AI
        </Link>
      </div>
    </section>
  );
}
