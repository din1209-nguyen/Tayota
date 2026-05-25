export function formatVnd(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(numeric);
}

export function formatNumber(value, suffix = "") {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return "Đang cập nhật";
  return `${new Intl.NumberFormat("vi-VN").format(numeric)}${suffix ? ` ${suffix}` : ""}`;
}

export function getVehicleName(vehicle) {
  return vehicle?.versionName || vehicle?.carVersionName || vehicle?.name || vehicle?.modelName || "Mẫu xe Tayota";
}

export function getVehicleId(vehicle) {
  return vehicle?.carVersionId || vehicle?.id || vehicle?.versionId;
}

export function getVehiclePrice(vehicle) {
  if (vehicle?.minPrice) return vehicle.minPrice;
  if (Array.isArray(vehicle?.prices) && vehicle.prices.length) {
    return vehicle.prices
      .map((item) => Number(item?.price))
      .filter((value) => Number.isFinite(value) && value > 0)
      .sort((a, b) => a - b)[0];
  }
  return vehicle?.price || vehicle?.listedPrice || vehicle?.startingPrice || vehicle?.basePrice;
}

export function getVehicleSeriesName(vehicle) {
  return vehicle?.carSeriesName || vehicle?.seriesName || vehicle?.carSeries?.name || "Toyota";
}

export function getVehicleStyleName(vehicle) {
  return vehicle?.carStyleName || vehicle?.styleName || vehicle?.bodyStyle || vehicle?.carSeries?.carStyleName || "Đang cập nhật";
}

export function getVehicleSpec(vehicle) {
  return vehicle?.specification || vehicle?.spec || {};
}

export function getVehicleImage(vehicle) {
  const firstPriceImage = Array.isArray(vehicle?.prices)
    ? vehicle.prices.find((item) => item?.exImageUrl)?.exImageUrl
    : "";
  const firstGalleryImage = Array.isArray(vehicle?.galleries)
    ? vehicle.galleries.find((item) => item?.imageUrl)?.imageUrl
    : "";
  return vehicle?.imageUrl || vehicle?.thumbnailUrl || vehicle?.mainImageUrl || firstPriceImage || firstGalleryImage || "";
}

export function getVehicleHighlights(vehicle) {
  const spec = getVehicleSpec(vehicle);
  return [
    { label: "Số chỗ", value: spec?.numberOfSeats ? `${spec.numberOfSeats} chỗ` : vehicle?.numberOfSeats },
    { label: "Kiểu dáng", value: getVehicleStyleName(vehicle) },
    { label: "Nhiên liệu", value: spec?.fuel || vehicle?.fuelType || vehicle?.engineType },
    { label: "Xuất xứ", value: spec?.origin },
    { label: "Hộp số", value: spec?.gearbox },
    { label: "Động cơ", value: spec?.cylinderCapacity },
  ].filter((item) => item.value);
}

export function unwrapList(data) {
  if (Array.isArray(data)) return data;
  return data?.content || data?.items || data?.data || data?.records || [];
}

export function getGoogleMapsUrl(dealer) {
  if (dealer?.latitude && dealer?.longitude) {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${dealer.latitude},${dealer.longitude}`)}`;
  }
  if (dealer?.address) {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(dealer.address)}`;
  }
  return "";
}

export function roleLabel(role) {
  const labels = {
    ADMIN: "Quản trị viên",
    MANAGER: "Quản lý",
    SERVICE_ADVISOR: "Cố vấn dịch vụ",
    ASSISTANT: "Nhân viên tư vấn",
    MECHANIC: "Kỹ thuật viên",
    USER: "Khách hàng",
  };
  return labels[role] || role || "Khách hàng";
}

export function statusLabel(status) {
  const labels = {
    ACTIVE: "Hoạt động",
    BANNED: "Đã khóa",
    uploaded: "Đã tải lên",
    indexing: "Đang lập chỉ mục",
    indexed: "Sẵn sàng",
    failed: "Thất bại",
    PENDING: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    COMPLETED: "Hoàn tất",
    CANCELLED: "Đã hủy",
    WAITING: "Đang chờ",
    CHATTING: "Đang chat",
    RESOLVED: "Đã xử lý",
    CLOSED: "Đã đóng",
  };
  return labels[status] || status || "Đang cập nhật";
}
