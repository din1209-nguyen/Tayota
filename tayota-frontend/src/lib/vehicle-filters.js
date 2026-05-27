import { getVehicleName, getVehiclePrice, getVehicleSeriesName, getVehicleSpec, getVehicleStyleName } from "@/lib/format";

export const EMPTY_VEHICLE_FILTERS = {
  styleId: "",
  seriesId: "",
  keyword: "",
  versionKeyword: "",
  modelYear: "",
  priceRange: "",
  minPrice: "",
  maxPrice: "",
  numberOfSeats: "",
  fuel: "",
  origin: "",
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

function vehicleSpecValue(vehicle, keys = []) {
  const spec = getVehicleSpec(vehicle);
  for (const key of keys) {
    const value = vehicle?.[key] || spec?.[key];
    if (value !== undefined && value !== null && value !== "") return value;
  }
  return "";
}

export function getSeriesOptions(styles = [], styleId = "") {
  const selectedStyle = styles.find((style) => String(style.id) === String(styleId));
  const getStyleSeries = (style) => style?.series || style?.carSeries || [];
  return selectedStyle ? getStyleSeries(selectedStyle) : styles.flatMap(getStyleSeries);
}

export function filterVehicleItems(vehicles = [], filters = {}) {
  const keyword = normalized(filters.keyword);
  const versionKeyword = normalized(filters.versionKeyword);
  const minPrice = Number(filters.minPrice);
  const maxPrice = Number(filters.maxPrice);

  return vehicles.filter((vehicle) => {
    const price = Number(getVehiclePrice(vehicle));
    const vehicleName = normalized(getVehicleName(vehicle));
    const searchable = normalized(
      `${getVehicleName(vehicle)} ${vehicle?.versionName || ""} ${getVehicleSeriesName(vehicle)} ${getVehicleStyleName(vehicle)}`
    );
    const numberOfSeats = vehicleSpecValue(vehicle, ["numberOfSeats", "seats", "seatCount"]);
    const fuel = vehicleSpecValue(vehicle, ["fuel", "fuelType", "engineType"]);
    const origin = vehicleSpecValue(vehicle, ["origin", "madeIn", "assembly"]);

    if (filters.styleId && String(vehicleStyleId(vehicle)) !== String(filters.styleId)) return false;
    if (filters.seriesId && String(vehicleSeriesId(vehicle)) !== String(filters.seriesId)) return false;
    if (keyword && !searchable.includes(keyword)) return false;
    if (versionKeyword && !vehicleName.includes(versionKeyword)) return false;
    if (filters.modelYear && String(vehicle?.modelYear || "") !== String(filters.modelYear)) return false;
    if (filters.minPrice && (!Number.isFinite(price) || price < minPrice)) return false;
    if (filters.maxPrice && (!Number.isFinite(price) || price > maxPrice)) return false;
    if (filters.numberOfSeats && numberOfSeats && String(numberOfSeats) !== String(filters.numberOfSeats)) return false;
    if (filters.fuel && fuel && normalized(fuel) !== normalized(filters.fuel)) return false;
    if (filters.origin && origin && normalized(origin) !== normalized(filters.origin)) return false;
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
