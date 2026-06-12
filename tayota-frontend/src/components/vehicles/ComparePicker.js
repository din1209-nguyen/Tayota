"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { getAllCarVersions, getCarStylesWithVersions } from "@/lib/services/car";
import { formatVnd, getVehicleId, getVehicleImage, getVehicleName, getVehiclePrice, unwrapList } from "@/lib/format";
import { EMPTY_VEHICLE_FILTERS, filterVehicleItems } from "@/lib/vehicle-filters";
import VehicleFilterControls from "@/components/vehicles/VehicleFilterControls";

const MAX_COMPARE_VEHICLES = 3;

export default function ComparePicker({ selectedIds = [], selectedVehicles: comparedVehicles = [] }) {
  const router = useRouter();
  const [vehicles, setVehicles] = useState([]);
  const [styles, setStyles] = useState([]);
  const [selected, setSelected] = useState(selectedIds.slice(0, MAX_COMPARE_VEHICLES));
  const [filters, setFilters] = useState(EMPTY_VEHICLE_FILTERS);
  const [message, setMessage] = useState("");

  useEffect(() => {
    setSelected(selectedIds.slice(0, MAX_COMPARE_VEHICLES));
  }, [selectedIds]);

  useEffect(() => {
    let alive = true;
    Promise.all([getAllCarVersions(), getCarStylesWithVersions()])
      .then(([vehicleResult, styleResult]) => {
        if (!alive) return;
        setVehicles(vehicleResult);
        setStyles(unwrapList(styleResult));
      })
      .catch((error) => {
        if (alive) setMessage(error.message || "Không thể tải danh sách xe.");
      });
    return () => {
      alive = false;
    };
  }, []);

  const vehicleMap = useMemo(() => {
    const entries = [...vehicles, ...comparedVehicles].map((vehicle) => [String(getVehicleId(vehicle)), vehicle]);
    return new Map(entries);
  }, [comparedVehicles, vehicles]);

  const selectedVehicles = selected.map((id) => vehicleMap.get(String(id))).filter(Boolean);
  const filteredVehicles = useMemo(() => filterVehicleItems(vehicles, filters), [filters, vehicles]);

  function toggle(id) {
    setMessage("");
    setSelected((current) => {
      if (current.some((item) => String(item) === String(id))) {
        return current.filter((item) => String(item) !== String(id));
      }
      if (current.length >= MAX_COMPARE_VEHICLES) {
        setMessage("Bạn có thể so sánh tối đa 3 xe cùng lúc.");
        return current;
      }
      return [...current, id];
    });
  }

  function applySelection(nextSelected = selected) {
    const query = nextSelected.map((id) => `ids=${encodeURIComponent(id)}`).join("&");
    router.push(query ? `/compare?${query}` : "/compare");
  }

  function remove(id) {
    const nextSelected = selected.filter((item) => String(item) !== String(id));
    setSelected(nextSelected);
    applySelection(nextSelected);
  }

  return (
    <section className="compare-studio shell-container">
      <div className="compare-picker-head">
        <div>
          <p className="eyebrow">So sánh xe</p>
          <h2>Chọn ba phiên bản để đối chiếu</h2>
        </div>
        <button className="btn btn-primary" type="button" onClick={() => applySelection()}>
          Cập nhật so sánh
        </button>
      </div>

      <div className="compare-slots">
        {Array.from({ length: MAX_COMPARE_VEHICLES }, (_, index) => {
          const vehicle = selectedVehicles[index];
          if (!vehicle) {
            return (
              <article className="compare-slot empty" key={`empty-${index}`}>
                <span className="compare-plus">+</span>
                <strong>Thêm mẫu xe</strong>
                <small>Chọn từ danh sách bên dưới</small>
              </article>
            );
          }
          const id = getVehicleId(vehicle);
          const imageUrl = getVehicleImage(vehicle);
          return (
            <article className="compare-slot" key={id}>
              <button className="slot-remove" type="button" aria-label={`Bỏ ${getVehicleName(vehicle)}`} onClick={() => remove(id)}>x</button>
              <div className="compare-slot-media" style={imageUrl ? { backgroundImage: `url(${imageUrl})` } : undefined} />
              <h2>{getVehicleName(vehicle)}</h2>
              <p>Giá từ <strong>{formatVnd(getVehiclePrice(vehicle))}</strong></p>
              <Link href={`/appointments/test-drive?carVersionId=${id}`}>Đăng ký lái thử</Link>
            </article>
          );
        })}
      </div>

      <div className="compare-selector">
        <div className="compare-selector-head">
          <p className="eyebrow">Chọn hoặc thay xe</p>
          <span>{filteredVehicles.length} phiên bản phù hợp</span>
        </div>
        <VehicleFilterControls
          filters={filters}
          onChange={setFilters}
          onReset={() => setFilters(EMPTY_VEHICLE_FILTERS)}
          styles={styles}
          variant="chooser"
        />
        {message ? <div className="status-box">{message}</div> : null}
        <div className="compare-option-grid">
          {filteredVehicles.map((vehicle) => {
            const id = getVehicleId(vehicle);
            const active = selected.some((item) => String(item) === String(id));
            const imageUrl = getVehicleImage(vehicle);
            return (
              <button className={`choice-card compare-choice-card ${active ? "selected" : ""}`} key={id} type="button" onClick={() => toggle(id)}>
                <span className="compare-choice-image" style={imageUrl ? { backgroundImage: `url(${imageUrl})` } : undefined}>
                  {!imageUrl ? "TAYOTA" : null}
                </span>
                <span className="compare-choice-copy">
                  <strong>{getVehicleName(vehicle)}</strong>
                  <span>Giá từ {formatVnd(getVehiclePrice(vehicle))}</span>
                </span>
                <span className="compare-choice-state">{active ? "Đã chọn" : "Chọn xe"}</span>
              </button>
            );
          })}
          {!filteredVehicles.length ? <div className="status-box wide">Không có mẫu xe phù hợp bộ lọc.</div> : null}
        </div>
      </div>
    </section>
  );
}
