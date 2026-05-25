import { getGoogleMapsUrl } from "@/lib/format";

export default function DealerGrid({ dealerships = [] }) {
  if (!dealerships.length) {
    return <div className="status-box">Chưa có đại lý đang hoạt động.</div>;
  }

  return (
    <div className="dealer-grid">
      {dealerships.map((dealer) => {
        const mapsUrl = getGoogleMapsUrl(dealer);
        return (
          <article className="dealer-card card" key={dealer.id}>
            <p className="eyebrow">Đại lý</p>
            <h2>{dealer.name}</h2>
            <dl className="dealer-info">
              <div>
                <dt>Địa chỉ</dt>
                <dd>{dealer.address || "Đang cập nhật"}</dd>
              </div>
              <div>
                <dt>Điện thoại</dt>
                <dd>{dealer.phone || "Đang cập nhật"}</dd>
              </div>
              <div>
                <dt>Giờ hoạt động</dt>
                <dd>{dealer.operatingHours || "Đang cập nhật"}</dd>
              </div>
              <div>
                <dt>Vị trí</dt>
                <dd>{dealer.latitude && dealer.longitude ? `${dealer.latitude}, ${dealer.longitude}` : "Đang cập nhật"}</dd>
              </div>
            </dl>
            {mapsUrl ? (
              <a className="btn btn-primary" href={mapsUrl} target="_blank" rel="noreferrer">
                Chỉ đường
              </a>
            ) : null}
          </article>
        );
      })}
    </div>
  );
}
