import Link from "next/link";

function normalizeValue(value) {
  return Array.isArray(value) ? value[0] : value || "";
}

export default function VehicleFilters({ searchParams = {}, styles = [] }) {
  const selectedStyleId = normalizeValue(searchParams.styleId);
  const selectedSeriesId = normalizeValue(searchParams.seriesId);
  const selectedStyle = styles.find((style) => String(style.id) === String(selectedStyleId));
  const seriesOptions = selectedStyle ? selectedStyle.series || [] : styles.flatMap((style) => style.series || []);
  const activeFilters = [
    normalizeValue(searchParams.keyword) && `Từ khóa: ${normalizeValue(searchParams.keyword)}`,
    selectedStyle?.name,
    seriesOptions.find((series) => String(series.id) === String(selectedSeriesId))?.name,
    normalizeValue(searchParams.modelYear) && `Năm ${normalizeValue(searchParams.modelYear)}`,
    normalizeValue(searchParams.minPrice) && "Có giá tối thiểu",
    normalizeValue(searchParams.maxPrice) && "Có giá tối đa",
  ].filter(Boolean);

  function styleHref(styleId) {
    const query = new URLSearchParams();
    Object.entries(searchParams).forEach(([key, value]) => {
      const normalized = normalizeValue(value);
      if (normalized && key !== "styleId" && key !== "seriesId" && key !== "page") query.set(key, normalized);
    });
    if (styleId) query.set("styleId", styleId);
    return query.size ? `/vehicles?${query}` : "/vehicles";
  }

  return (
    <form className="filter-panel catalog-filter">
      <div className="catalog-filter-head">
        <div>
        <p className="eyebrow">Bộ lọc</p>
          <h2>Khám phá theo kiểu dáng</h2>
        </div>
        <Link className="filter-reset" href="/vehicles">
          Xóa lọc
        </Link>
      </div>

      <nav className="catalog-tabs" aria-label="Kiểu dáng xe">
        <Link className={!selectedStyleId ? "active" : ""} href={styleHref("")}>Tất cả</Link>
        {styles.map((style) => (
          <Link className={String(style.id) === String(selectedStyleId) ? "active" : ""} href={styleHref(style.id)} key={style.id}>
            {style.name}
          </Link>
        ))}
      </nav>

      <details className="catalog-filter-more" open={activeFilters.length > 0}>
        <summary>Lọc thêm theo phiên bản và khoảng giá</summary>
        <div className="catalog-filter-fields">
          <label className="label">
            Từ khóa
            <input className="field" name="keyword" defaultValue={normalizeValue(searchParams.keyword)} placeholder="Camry, Cross, Vios..." />
          </label>
          <label className="label">
            Dòng xe
            <select className="field" name="seriesId" defaultValue={selectedSeriesId}>
              <option value="">Tất cả dòng xe</option>
              {seriesOptions.map((series) => (
                <option key={series.id} value={series.id}>{series.name}</option>
              ))}
            </select>
          </label>
          <label className="label">
            Năm model
            <input className="field" name="modelYear" defaultValue={normalizeValue(searchParams.modelYear)} inputMode="numeric" placeholder="2026" />
          </label>
          <label className="label">
            Giá từ
            <input className="field" name="minPrice" defaultValue={normalizeValue(searchParams.minPrice)} inputMode="numeric" placeholder="500000000" />
          </label>
          <label className="label">
            Giá đến
            <input className="field" name="maxPrice" defaultValue={normalizeValue(searchParams.maxPrice)} inputMode="numeric" placeholder="2500000000" />
          </label>
          <button className="btn btn-primary" type="submit">Áp dụng</button>
        </div>
        <input name="styleId" type="hidden" value={selectedStyleId} />
      </details>

      {activeFilters.length ? (
        <div className="filter-chips" aria-label="Bộ lọc đang áp dụng">
          {activeFilters.map((filter) => <span key={filter}>{filter}</span>)}
        </div>
      ) : null}
    </form>
  );
}
