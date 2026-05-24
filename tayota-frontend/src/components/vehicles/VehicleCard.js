import Link from "next/link";
import {
  formatVnd,
  getVehicleId,
  getVehicleName,
  getVehiclePrice,
} from "@/lib/format";

export default function VehicleCard({ vehicle }) {
  const id = getVehicleId(vehicle);
  const name = getVehicleName(vehicle);
  const year = vehicle?.modelYear || vehicle?.year || "Mới nhất";
  const style = vehicle?.styleName || vehicle?.bodyStyle || vehicle?.style || "Cao cấp";
  const fuel = vehicle?.fuelType || vehicle?.engineType || "Toyota Hybrid";

  return (
    <article className="vehicle-card card">
      <div className="vehicle-media" aria-hidden="true">
        <div className="vehicle-silhouette" />
      </div>
      <div className="vehicle-body">
        <p className="eyebrow">{style}</p>
        <h3>{name}</h3>
        <p className="vehicle-price">{formatVnd(getVehiclePrice(vehicle))}</p>
        <dl className="vehicle-meta">
          <div>
            <dt>Năm</dt>
            <dd>{year}</dd>
          </div>
          <div>
            <dt>Động cơ</dt>
            <dd>{fuel}</dd>
          </div>
        </dl>
        <div className="vehicle-actions">
          <Link className="btn btn-primary" href={id ? `/vehicles/${id}` : "/vehicles"}>
            Chi tiết
          </Link>
          <Link className="btn btn-ghost" href={`/compare${id ? `?ids=${id}` : ""}`}>
            So sánh
          </Link>
        </div>
      </div>
    </article>
  );
}
