"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import VehicleCard from "@/components/vehicles/VehicleCard";
import VehicleFilterControls from "@/components/vehicles/VehicleFilterControls";
import { EMPTY_VEHICLE_FILTERS, filterVehicleItems } from "@/lib/vehicle-filters";

export default function FeaturedVehicleBrowser({ vehicles, styles }) {
  const [filters, setFilters] = useState(EMPTY_VEHICLE_FILTERS);
  const shownVehicles = useMemo(() => filterVehicleItems(vehicles, filters).slice(0, 3), [filters, vehicles]);
  const catalogHref = filters.styleId ? `/vehicles?styleId=${encodeURIComponent(filters.styleId)}` : "/vehicles";

  return (
    <>
      <div className="shell-container featured-filter">
        <VehicleFilterControls filters={filters} onChange={setFilters} styles={styles} variant="minimal" advanced={false} />
        <Link className="featured-filter-link" href={catalogHref}>Xem đầy đủ lựa chọn</Link>
      </div>
      <div className="shell-container vehicle-grid">
        {shownVehicles.map((vehicle, index) => (
          <VehicleCard key={vehicle?.id || vehicle?.carVersionId || index} vehicle={vehicle} />
        ))}
        {!shownVehicles.length ? <div className="status-box wide">Chưa có xe nổi bật cho kiểu dáng này.</div> : null}
      </div>
    </>
  );
}
