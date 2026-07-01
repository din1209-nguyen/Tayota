"use client";

import { useState } from "react";
import { formatVnd } from "@/lib/format";
import VehicleImageButton from "@/components/vehicles/VehicleImageButton";

export default function AccessoryDetailGrid({ accessories = [] }) {
  const [selected, setSelected] = useState(null);

  return (
    <>
      <div className="accessory-grid">
        {accessories.map((accessory) => (
          <button className="accessory-card card accessory-card-button" key={accessory.id} type="button" onClick={() => setSelected(accessory)}>
            <span className={`accessory-image ${accessory.imageUrl ? "has-image" : "is-empty"}`} style={accessory.imageUrl ? { backgroundImage: `url(${accessory.imageUrl})` } : undefined}>
              {!accessory.imageUrl ? <span>Ảnh phụ kiện</span> : null}
            </span>
            <span className="accessory-card-copy">
              <span className="accessory-card-type">{accessory.type || "Phụ kiện"}</span>
              <strong className="accessory-card-title">{accessory.model}</strong>
              <span className="accessory-card-desc">{accessory.description}</span>
              <span className="accessory-card-price">{formatVnd(accessory.price)}</span>
            </span>
          </button>
        ))}
      </div>

      {selected ? (
        <div className="detail-modal-backdrop" role="presentation" onClick={() => setSelected(null)}>
          <section className="detail-modal accessory-detail-modal" role="dialog" aria-modal="true" aria-labelledby="accessory-detail-title" onClick={(event) => event.stopPropagation()}>
            <header className="detail-modal-head">
              <div>
                <p className="eyebrow">{selected.type || "Phụ kiện"}</p>
                <h2 id="accessory-detail-title">{selected.model}</h2>
                <span>{selected.brand || "Tayota"} · {formatVnd(selected.price)}</span>
              </div>
              <button className="icon-button" type="button" aria-label="Đóng" onClick={() => setSelected(null)}>×</button>
            </header>
            <div className="detail-modal-body">
              {selected.imageUrl ? (
                <VehicleImageButton className="accessory-detail-image" src={selected.imageUrl} title={selected.model} />
              ) : (
                <div className="accessory-detail-image accessory-detail-placeholder">Ảnh phụ kiện đang được cập nhật</div>
              )}
              <section>
                <h3>Mô tả</h3>
                <p>{selected.description || "Thông tin phụ kiện đang được cập nhật."}</p>
              </section>
              {selected.useContent ? (
                <section>
                  <h3>Công dụng</h3>
                  <p>{selected.useContent}</p>
                </section>
              ) : null}
              {selected.reminderContent ? (
                <section>
                  <h3>Lưu ý</h3>
                  <p>{selected.reminderContent}</p>
                </section>
              ) : null}
            </div>
          </section>
        </div>
      ) : null}
    </>
  );
}
