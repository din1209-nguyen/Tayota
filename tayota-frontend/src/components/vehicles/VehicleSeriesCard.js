import Link from "next/link";
import { formatVnd, getVehicleId, getVehicleImage, getVehicleName, getVehiclePrice } from "@/lib/format";

export default function VehicleSeriesCard({ group }) {
  const leadVehicle = group.vehicles[0];
  const imageUrl = getVehicleImage(leadVehicle);

  return (
    <article className="series-card">
      <div className="series-media" aria-hidden="true">
        {imageUrl ? <div style={{ backgroundImage: `url(${imageUrl})` }} /> : <span className="vehicle-silhouette" />}
      </div>
      <p className="eyebrow">{group.styleName}</p>
      <h2>{group.name}</h2>
      <p className="series-price">Chỉ từ <strong>{formatVnd(getVehiclePrice(leadVehicle))}</strong></p>
      <details className="series-versions">
        <summary>Xem {group.vehicles.length} phiên bản</summary>
        <div>
          {group.vehicles.map((vehicle) => {
            const id = getVehicleId(vehicle);
            return (
              <section className="series-version" key={id}>
                <div>
                  <strong>{getVehicleName(vehicle)}</strong>
                  <span>{formatVnd(getVehiclePrice(vehicle))}</span>
                </div>
                <nav aria-label={`Thao tác ${getVehicleName(vehicle)}`}>
                  <Link href={`/vehicles/${id}`}>Chi tiết</Link>
                  <Link href={`/compare?ids=${id}`}>So sánh</Link>
                  <Link href={`/appointments/test-drive?carVersionId=${id}`}>Lái thử</Link>
                </nav>
              </section>
            );
          })}
        </div>
      </details>
    </article>
  );
}
