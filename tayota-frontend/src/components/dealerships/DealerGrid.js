"use client";

import { useMemo, useState } from "react";
import { getGoogleMapsUrl } from "@/lib/format";

function getCoordinate(value) {
  const coordinate = Number(value);
  return Number.isFinite(coordinate) ? coordinate : null;
}

function hasCoordinates(dealer) {
  return getCoordinate(dealer?.latitude) !== null && getCoordinate(dealer?.longitude) !== null;
}

function getMapEmbedUrl(dealer) {
  if (!dealer) return "";
  const latitude = getCoordinate(dealer.latitude);
  const longitude = getCoordinate(dealer.longitude);
  const query = latitude !== null && longitude !== null ? `${latitude},${longitude}` : dealer.address || dealer.name || "";
  return query ? `https://www.google.com/maps?q=${encodeURIComponent(query)}&z=15&output=embed` : "";
}

export default function DealerGrid({ dealerships = [] }) {
  const firstMappableDealer = useMemo(() => dealerships.find((dealer) => hasCoordinates(dealer) || dealer.address), [dealerships]);
  const [selectedDealerId, setSelectedDealerId] = useState(firstMappableDealer?.id || "");
  const selectedDealer = dealerships.find((dealer) => dealer.id === selectedDealerId) || firstMappableDealer;
  const mapUrl = getMapEmbedUrl(selectedDealer);
  const selectedDirectionsUrl = getGoogleMapsUrl(selectedDealer);

  if (!dealerships.length) {
    return <div className="shell-container status-box">Chưa có đại lý đang hoạt động.</div>;
  }

  return (
    <div className="dealer-layout">
      <div className="dealer-list" aria-label="Danh sách đại lý Tayota">
        {dealerships.map((dealer) => {
          const canShowOnMap = hasCoordinates(dealer) || Boolean(dealer.address);
          const isSelected = selectedDealer?.id === dealer.id;
          const directionsUrl = getGoogleMapsUrl(dealer);

          return (
            <article className={`dealer-card card ${isSelected ? "selected" : ""}`} key={dealer.id}>
              <button
                className="dealer-card-map-hitarea"
                type="button"
                disabled={!canShowOnMap}
                aria-label={`Xem ${dealer.name} trên bản đồ`}
                onClick={() => setSelectedDealerId(dealer.id)}
              />
              <div className="dealer-card-head">
                <p className="eyebrow">Đại lý</p>
                <span className={`dealer-status ${dealer.active === false ? "inactive" : ""}`}>
                  <span aria-hidden="true" />
                  {dealer.active === false ? "Tạm ngừng" : "Đang hoạt động"}
                </span>
              </div>
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
                  <dd>{hasCoordinates(dealer) ? `${dealer.latitude}, ${dealer.longitude}` : "Theo địa chỉ đại lý"}</dd>
                </div>
              </dl>
              <div className="dealer-actions">
                <button
                  className="btn btn-primary"
                  type="button"
                  disabled={!canShowOnMap}
                  onClick={() => setSelectedDealerId(dealer.id)}
                >
                  Xem trên bản đồ
                </button>
                {dealer.phone ? (
                  <a className="btn btn-ghost" href={`tel:${dealer.phone}`}>
                    Gọi đại lý
                  </a>
                ) : null}
                {directionsUrl ? (
                  <a className="btn btn-ghost" href={directionsUrl} target="_blank" rel="noreferrer">
                    Chỉ đường
                  </a>
                ) : null}
              </div>
            </article>
          );
        })}
      </div>

      <aside className="dealer-map-panel" aria-label="Bản đồ đại lý">
        <div className="dealer-map-head">
          <div>
            <p className="eyebrow">Bản đồ đại lý</p>
            <h2>{selectedDealer?.name || "Chọn đại lý"}</h2>
          </div>
          {selectedDealer ? <span>{selectedDealer.address}</span> : null}
          {selectedDealer ? (
            <div className="dealer-map-meta">
              <span className={`dealer-status ${selectedDealer.active === false ? "inactive" : ""}`}>
                <span aria-hidden="true" />
                {selectedDealer.active === false ? "Tạm ngừng" : "Đang hoạt động"}
              </span>
              {selectedDirectionsUrl ? (
                <a className="btn btn-primary" href={selectedDirectionsUrl} target="_blank" rel="noreferrer">
                  Chỉ đường
                </a>
              ) : null}
            </div>
          ) : null}
        </div>
        {mapUrl ? (
          <iframe
            key={selectedDealer.id}
            title={`Bản đồ ${selectedDealer.name}`}
            src={mapUrl}
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
          />
        ) : (
          <div className="dealer-map-empty">Chưa có thông tin vị trí để hiển thị bản đồ.</div>
        )}
      </aside>
    </div>
  );
}
