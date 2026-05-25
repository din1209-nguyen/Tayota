"use client";

import { getSeriesOptions } from "@/lib/vehicle-filters";

export default function VehicleFilterControls({
  filters,
  onChange,
  onReset,
  styles = [],
  variant = "chooser",
  advanced = true,
  showKeyword = true,
}) {
  const seriesOptions = getSeriesOptions(styles, filters.styleId);

  function change(name, value) {
    onChange({
      ...filters,
      [name]: value,
      ...(name === "styleId" ? { seriesId: "" } : {}),
      ...(!showKeyword ? { keyword: "" } : {}),
    });
  }

  return (
    <div className={`vehicle-filter-controls vehicle-filter-${variant}`}>
      <div className="vehicle-tabs" role="group" aria-label="Lọc kiểu dáng xe">
        <button className={!filters.styleId ? "active" : ""} type="button" onClick={() => change("styleId", "")}>
          Tất cả
        </button>
        {styles.map((style) => (
          <button
            className={String(filters.styleId) === String(style.id) ? "active" : ""}
            key={style.id}
            type="button"
            onClick={() => change("styleId", style.id)}
          >
            {style.name}
          </button>
        ))}
      </div>

      {variant !== "minimal" ? (
        <div className={`vehicle-filter-primary ${showKeyword ? "" : "vehicle-filter-primary-series-only"}`}>
          {showKeyword ? (
            <input
              aria-label="Tìm xe"
              className="field"
              placeholder="Tìm mẫu xe hoặc phiên bản"
              value={filters.keyword}
              onChange={(event) => change("keyword", event.target.value)}
            />
          ) : null}
          <select
            aria-label="Dòng xe"
            className="field"
            value={filters.seriesId}
            onChange={(event) => change("seriesId", event.target.value)}
          >
            <option value="">Tất cả dòng xe</option>
            {seriesOptions.map((series) => (
              <option key={series.id} value={series.id}>
                {series.name}
              </option>
            ))}
          </select>
          {onReset ? (
            <button className="filter-clear" type="button" onClick={onReset}>
              Xóa lọc
            </button>
          ) : null}
        </div>
      ) : null}

      {advanced && variant !== "compact" && variant !== "minimal" ? (
        <details className="vehicle-filter-more">
          <summary>Lọc thêm</summary>
          <div className="vehicle-filter-fields">
            <input
              aria-label="Năm model"
              className="field"
              inputMode="numeric"
              placeholder="Năm model"
              value={filters.modelYear}
              onChange={(event) => change("modelYear", event.target.value)}
            />
            <input
              aria-label="Giá từ"
              className="field"
              inputMode="numeric"
              placeholder="Giá từ"
              value={filters.minPrice}
              onChange={(event) => change("minPrice", event.target.value)}
            />
            <input
              aria-label="Giá đến"
              className="field"
              inputMode="numeric"
              placeholder="Giá đến"
              value={filters.maxPrice}
              onChange={(event) => change("maxPrice", event.target.value)}
            />
          </div>
        </details>
      ) : null}
    </div>
  );
}
