import Image from "next/image";
import { apiFetch } from "@/lib/api";
import { formatVnd, getVehicleName, getVehiclePrice, unwrapList } from "@/lib/format";

async function getCompare(ids) {
  if (!ids.length) return { vehicles: [], error: null };
  try {
    const query = ids.map((id) => `ids=${encodeURIComponent(id)}`).join("&");
    const data = await apiFetch(`/car/catalog/car-versions/compare?${query}`, {
      cache: "no-store",
    });
    return { vehicles: unwrapList(data), error: null };
  } catch (error) {
    return { vehicles: [], error: error.message };
  }
}

export default async function ComparePage({ searchParams }) {
  const params = await searchParams;
  const ids = Array.isArray(params?.ids) ? params.ids : params?.ids ? [params.ids] : [];
  const { vehicles, error } = await getCompare(ids.slice(0, 3));

  return (
    <section className="compare-page">
      <div className="compare-hero">
        <Image
          className="compare-hero-image"
          src="/images/compare-balance-land-cruiser.png"
          alt=""
          fill
          priority
          sizes="100vw"
          aria-hidden="true"
        />
        <div className="compare-hero-shade" aria-hidden="true" />
        <div className="shell-container compare-hero-content">
          <p className="eyebrow">So sánh</p>
          <h1>Đặt các phiên bản cạnh nhau</h1>
          {!error && !vehicles.length ? (
            <div className="status-box compare-empty">Chọn xe từ catalogue để bắt đầu so sánh.</div>
          ) : null}
        </div>
      </div>

      <div className="shell-container compare-table compare-results">
        {error ? <div className="status-box">{error}</div> : null}
        {vehicles.map((vehicle, index) => (
          <article className="card compare-card" key={vehicle?.id || vehicle?.carVersionId || index}>
            <p className="eyebrow">Lựa chọn {index + 1}</p>
            <h2>{getVehicleName(vehicle)}</h2>
            <strong>{formatVnd(getVehiclePrice(vehicle))}</strong>
            <p>{vehicle?.modelYear || "Năm mới nhất"}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
