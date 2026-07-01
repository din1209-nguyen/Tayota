"use client";

import { useEffect, useState } from "react";

export default function VehicleImageButton({ src, title, className = "gallery-tile" }) {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return undefined;
    function onKeyDown(event) {
      if (event.key === "Escape") setOpen(false);
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open]);

  if (!src) {
    return <div className={className} aria-label={title || "Ảnh chưa cập nhật"} />;
  }

  return (
    <>
      <button
        className={`${className} image-zoom-button`}
        type="button"
        style={{ backgroundImage: `url(${src})` }}
        aria-label={`Xem ảnh lớn: ${title || "Ảnh xe"}`}
        onClick={() => setOpen(true)}
      />
      {open ? (
        <div className="image-lightbox" role="dialog" aria-modal="true" aria-label={title || "Ảnh xe"} onClick={() => setOpen(false)}>
          <div className="image-lightbox-panel" onClick={(event) => event.stopPropagation()}>
            <header>
              <strong>{title || "Ảnh xe"}</strong>
              <button className="icon-button" type="button" aria-label="Đóng ảnh" onClick={() => setOpen(false)}>
                ×
              </button>
            </header>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={src} alt={title || "Ảnh xe"} />
          </div>
        </div>
      ) : null}
    </>
  );
}
