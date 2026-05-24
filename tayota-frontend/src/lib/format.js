export function formatVnd(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(numeric);
}

export function getVehicleName(vehicle) {
  return (
    vehicle?.versionName ||
    vehicle?.carVersionName ||
    vehicle?.name ||
    vehicle?.modelName ||
    "Mẫu xe TAYOTA"
  );
}

export function getVehicleId(vehicle) {
  return vehicle?.carVersionId || vehicle?.id || vehicle?.versionId;
}

export function getVehiclePrice(vehicle) {
  return (
    vehicle?.price ||
    vehicle?.listedPrice ||
    vehicle?.startingPrice ||
    vehicle?.basePrice
  );
}

export function unwrapList(data) {
  if (Array.isArray(data)) return data;
  return data?.content || data?.items || data?.data || data?.records || [];
}
