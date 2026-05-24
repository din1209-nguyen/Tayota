import Link from "next/link";

export default function VehicleFilters({ searchParams = {} }) {
  return (
    <form className="filter-panel">
      <label className="label">
        Từ khóa
        <input
          className="field"
          name="keyword"
          defaultValue={searchParams.keyword || ""}
          placeholder="Camry, Cross..."
        />
      </label>
      <label className="label">
        Năm model
        <input
          className="field"
          name="modelYear"
          defaultValue={searchParams.modelYear || ""}
          inputMode="numeric"
          placeholder="2026"
        />
      </label>
      <label className="label">
        Giá từ
        <input
          className="field"
          name="minPrice"
          defaultValue={searchParams.minPrice || ""}
          inputMode="numeric"
          placeholder="800000000"
        />
      </label>
      <label className="label">
        Giá đến
        <input
          className="field"
          name="maxPrice"
          defaultValue={searchParams.maxPrice || ""}
          inputMode="numeric"
          placeholder="2500000000"
        />
      </label>
      <button className="btn btn-primary" type="submit">
        Lọc xe
      </button>
      <Link className="btn btn-ghost" href="/vehicles">
        Xóa lọc
      </Link>
    </form>
  );
}
