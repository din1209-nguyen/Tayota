import Link from "next/link";
import {
  formatVnd,
  getVehicleHighlights,
  getVehicleId,
  getVehicleImage,
  getVehicleName,
  getVehiclePrice,
  getVehicleSeriesName,
  getVehicleStyleName,
} from "@/lib/format";

export default function VehicleCard({ vehicle }) {
  const id = getVehicleId(vehicle);
  const name = getVehicleName(vehicle);
  const imageUrl = getVehicleImage(vehicle);
  const highlights = getVehicleHighlights(vehicle);

  return (
    <article className="vehicle-card card">
      <div className="vehicle-media" aria-hidden="true">
        {imageUrl ? <div className="vehicle-image" style={{ backgroundImage: `url(${imageUrl})` }} /> : <div className="vehicle-silhouette" />}
      </div>
      <div className="vehicle-body">
        <p className="eyebrow">{getVehicleStyleName(vehicle)}</p>
        <h3>{name}</h3>
        <p className="vehicle-series">{getVehicleSeriesName(vehicle)}</p>
        <p className="vehicle-price">
          <span>Giá từ</span>
          <strong>{formatVnd(getVehiclePrice(vehicle))}</strong>
        </p>
        <dl className="vehicle-meta vehicle-meta-rich">
          {highlights.slice(0, 6).map((item) => (
            <div key={item.label}>
              <dt>{item.label}</dt>
              <dd>{item.value}</dd>
            </div>
          ))}
        </dl>
        <div className="vehicle-actions">
          <Link className="btn btn-primary" href={id ? `/vehicles/${id}` : "/vehicles"}>
            Chi tiết
          </Link>
          <Link className="btn btn-ghost" href={`/compare${id ? `?ids=${id}` : ""}`}>
            So sánh
          </Link>
          <Link className="btn btn-ghost" href="/dealerships">
            Xem đại lý
          </Link>
        </div>
      </div>
    </article>
  );
}
