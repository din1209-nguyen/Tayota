"use client";

import { useMemo, useState } from "react";
import { filterVehicleItems, groupVehiclesBySeries } from "@/lib/vehicle-filters";
import VehicleFilters from "@/components/vehicles/VehicleFilters";
import VehicleSeriesCard from "@/components/vehicles/VehicleSeriesCard";

export default function VehicleCatalogClient({ error, searchParams = {}, styles = [], vehicles = [] }) {
  const [keyword, setKeyword] = useState(searchParams.keyword || "");
  const visibleVehicles = useMemo(
    () => filterVehicleItems(vehicles, { keyword }),
    [keyword, vehicles],
  );
  const seriesGroups = useMemo(() => groupVehiclesBySeries(visibleVehicles), [visibleVehicles]);

  return (
    <>
      <VehicleFilters keywordValue={keyword} onKeywordChange={setKeyword} searchParams={searchParams} styles={styles} />
      <div className="series-grid catalog-grid">
        {error ? <div className="status-box wide">{error}</div> : null}
        {!error && visibleVehicles.length === 0 ? <div className="status-box wide">Không tìm thấy xe phù hợp với bộ lọc hiện tại.</div> : null}
        {seriesGroups.map((group) => (
          <VehicleSeriesCard group={group} key={group.id} />
        ))}
      </div>
    </>
  );
}
