import { getVehiclePrice, getVehicleSeriesName, getVehicleStyleName } from "@/lib/format";

export const EMPTY_VEHICLE_FILTERS = {
  styleId: "",
  seriesId: "",
  keyword: "",
  modelYear: "",
  minPrice: "",
  maxPrice: "",
};

function normalized(value) {
  return String(value || "").trim().toLowerCase();
}

function vehicleStyleId(vehicle) {
  return vehicle?.carStyleId || vehicle?.carSeries?.carStyleId || "";
}

function vehicleSeriesId(vehicle) {
  return vehicle?.carSeriesId || vehicle?.carSeries?.id || "";
}

export function getSeriesOptions(styles = [], styleId = "") {
  const selectedStyle = styles.find((style) => String(style.id) === String(styleId));
  return selectedStyle ? selectedStyle.series || [] : styles.flatMap((style) => style.series || []);
}

export function filterVehicleItems(vehicles = [], filters = {}) {
  const keyword = normalized(filters.keyword);
  const minPrice = Number(filters.minPrice);
  const maxPrice = Number(filters.maxPrice);

  return vehicles.filter((vehicle) => {
    const price = Number(getVehiclePrice(vehicle));
    const searchable = normalized(
      `${vehicle?.name || ""} ${vehicle?.versionName || ""} ${getVehicleSeriesName(vehicle)} ${getVehicleStyleName(vehicle)}`
    );

    if (filters.styleId && String(vehicleStyleId(vehicle)) !== String(filters.styleId)) return false;
    if (filters.seriesId && String(vehicleSeriesId(vehicle)) !== String(filters.seriesId)) return false;
    if (keyword && !searchable.includes(keyword)) return false;
    if (filters.modelYear && String(vehicle?.modelYear || "") !== String(filters.modelYear)) return false;
    if (filters.minPrice && (!Number.isFinite(price) || price < minPrice)) return false;
    if (filters.maxPrice && (!Number.isFinite(price) || price > maxPrice)) return false;
    return true;
  });
}

export function groupVehiclesBySeries(vehicles = []) {
  const groups = new Map();

  vehicles.forEach((vehicle) => {
    const id = vehicleSeriesId(vehicle) || getVehicleSeriesName(vehicle);
    if (!groups.has(id)) {
      groups.set(id, {
        id,
        name: getVehicleSeriesName(vehicle),
        styleName: getVehicleStyleName(vehicle),
        vehicles: [],
      });
    }
    groups.get(id).vehicles.push(vehicle);
  });

  return Array.from(groups.values()).map((group) => ({
    ...group,
    vehicles: group.vehicles.sort((left, right) => Number(getVehiclePrice(left) || Infinity) - Number(getVehiclePrice(right) || Infinity)),
  }));
}
