"use client";

import Link from "next/link";

const PRICE_OPTIONS = [
  { value: "", label: "Chọn" },
  { value: "0-600000000", label: "Dưới 600 triệu" },
  { value: "600000000-900000000", label: "600 - 900 triệu" },
  { value: "900000000-1200000000", label: "900 triệu - 1.2 tỷ" },
  { value: "1200000000-", label: "Trên 1.2 tỷ" },
];

function normalizeValue(value) {
  return Array.isArray(value) ? value[0] : value || "";
}

function getSpecValue(vehicle, keys = []) {
  const spec = vehicle?.specification || vehicle?.spec || {};
  for (const key of keys) {
    const value = vehicle?.[key] || spec?.[key];
    if (value !== undefined && value !== null && value !== "") return value;
  }
  return "";
}

function uniqueOptions(items = [], getValue) {
  const seen = new Map();
  items.forEach((item) => {
    const value = getValue(item);
    if (value === undefined || value === null || value === "") return;
    const key = String(value);
    if (!seen.has(key)) seen.set(key, { value: key, label: key });
  });
  return Array.from(seen.values());
}

function getSeriesVersions(series = [], selectedSeriesId = "") {
  const scopedSeries = selectedSeriesId
    ? series.filter((item) => String(item.id) === String(selectedSeriesId))
    : series;

  return scopedSeries.flatMap((item) => item.versions || item.carVersions || []);
}

function AutoSubmitSelect({ label, name, value, placeholder = "Chọn", options = [] }) {
  return (
    <label className="label catalog-select-field">
      {label}
      <select className="field" name={name} defaultValue={value} onChange={(event) => event.currentTarget.form?.requestSubmit()}>
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

export default function VehicleFilters({ keywordValue = "", onKeywordChange, searchParams = {}, styles = [] }) {
  const selectedStyleId = normalizeValue(searchParams.styleId);
  const selectedSeriesId = normalizeValue(searchParams.seriesId);
  const selectedPriceRange = normalizeValue(searchParams.priceRange);
  const selectedVersionKeyword = normalizeValue(searchParams.versionKeyword);
  const selectedSeats = normalizeValue(searchParams.numberOfSeats);
  const selectedFuel = normalizeValue(searchParams.fuel);
  const selectedOrigin = normalizeValue(searchParams.origin);
  const selectedStyle = styles.find((style) => String(style.id) === String(selectedStyleId));
  const allSeries = styles.flatMap((style) => style.series || []);
  const seriesOptions = allSeries;
  const versionSeries = selectedSeriesId ? allSeries : selectedStyle?.series || allSeries;
  const versions = getSeriesVersions(versionSeries, selectedSeriesId);
  const priceLabel = PRICE_OPTIONS.find((option) => option.value === selectedPriceRange)?.label;

  const activeFilters = [
    selectedStyle?.name,
    seriesOptions.find((series) => String(series.id) === String(selectedSeriesId))?.name,
    keywordValue && `Tên xe: ${keywordValue}`,
    selectedPriceRange && priceLabel,
    selectedVersionKeyword && `Phiên bản: ${selectedVersionKeyword}`,
    selectedSeats && `${selectedSeats} chỗ`,
    selectedFuel,
    selectedOrigin,
  ].filter(Boolean);

  return (
    <form className="filter-panel catalog-filter">
      <div className="catalog-filter-head">
        <div>
          <p className="eyebrow">Bộ lọc</p>
          <h2>Khám phá theo nhu cầu</h2>
        </div>
        <Link className="filter-reset" href="/vehicles">
          Xóa lọc
        </Link>
      </div>

      <div className="catalog-filter-grid">
        <AutoSubmitSelect
          label="Kiểu dáng"
          name="styleId"
          value={selectedStyleId}
          options={styles.map((style) => ({ value: style.id, label: style.name }))}
        />
        <AutoSubmitSelect
          label="Giá"
          name="priceRange"
          value={selectedPriceRange}
          options={PRICE_OPTIONS.slice(1)}
        />
        <AutoSubmitSelect
          label="Số chỗ ngồi"
          name="numberOfSeats"
          value={selectedSeats}
          options={uniqueOptions(versions, (version) => getSpecValue(version, ["numberOfSeats", "seats", "seatCount"]))}
        />
        <AutoSubmitSelect
          label="Dòng xe"
          name="seriesId"
          value={selectedSeriesId}
          placeholder="Tất cả"
          options={seriesOptions.map((series) => ({ value: series.id, label: series.name }))}
        />
        <AutoSubmitSelect
          label="Phiên bản"
          name="versionKeyword"
          value={selectedVersionKeyword}
          options={uniqueOptions(versions, (version) => version.name || version.versionName || version.carVersionName)}
        />
        <AutoSubmitSelect
          label="Nhiên liệu"
          name="fuel"
          value={selectedFuel}
          options={uniqueOptions(versions, (version) => getSpecValue(version, ["fuel", "fuelType", "engineType"]))}
        />
        <AutoSubmitSelect
          label="Xuất xứ"
          name="origin"
          value={selectedOrigin}
          options={uniqueOptions(versions, (version) => getSpecValue(version, ["origin", "madeIn", "assembly"]))}
        />
      </div>

      <label className="label catalog-select-field catalog-search-field">
        Tìm theo tên
        <input
          className="field"
          value={keywordValue}
          placeholder="Nhập tên xe hoặc phiên bản"
          onChange={(event) => onKeywordChange?.(event.target.value)}
        />
      </label>

      {activeFilters.length ? (
        <div className="filter-chips" aria-label="Bộ lọc đang áp dụng">
          {activeFilters.map((filter) => <span key={filter}>{filter}</span>)}
        </div>
      ) : null}
    </form>
  );
}
