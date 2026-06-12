import { formatNumber } from "@/lib/format";

const SPEC_LABELS = [
  ["origin", "Xuất xứ", "Tổng quan"],
  ["fuel", "Nhiên liệu", "Tổng quan"],
  ["numberOfSeats", "Số chỗ", "Tổng quan", (value) => `${value} chỗ`],
  ["trademarks", "Thương hiệu", "Tổng quan"],
  ["length", "Dài", "Kích thước", (value) => formatNumber(value, "mm")],
  ["width", "Rộng", "Kích thước", (value) => formatNumber(value, "mm")],
  ["height", "Cao", "Kích thước", (value) => formatNumber(value, "mm")],
  ["grossWeightAllowance", "Khối lượng toàn tải", "Kích thước", (value) => formatNumber(value, "kg")],
  ["capacity", "Dung tích bình nhiên liệu", "Vận hành", (value) => formatNumber(value, "L")],
  ["cylinderCapacity", "Dung tích xy-lanh", "Vận hành"],
  ["cylinder", "Số xy-lanh", "Vận hành"],
  ["gearbox", "Hộp số", "Vận hành"],
  ["maximumSpeed", "Tốc độ tối đa", "Vận hành", (value) => formatNumber(value, "km/h")],
  ["acceleration", "Tăng tốc", "Vận hành"],
  ["torque", "Mô-men xoắn", "Vận hành"],
];

export function getSpecificationRows(specification = {}) {
  return SPEC_LABELS.map(([key, label, group, formatter]) => {
    const value = specification?.[key];
    if (value === null || value === undefined || value === "") return null;
    return {
      group,
      key,
      label,
      value: formatter ? formatter(value) : String(value),
    };
  }).filter(Boolean);
}

export function getCompareGroups(vehicle) {
  const spec = vehicle?.specification || {};
  return [
    {
      title: "Tổng quan",
      rows: [
        ["Dòng xe", vehicle?.carSeries?.name || vehicle?.carSeriesName],
        ["Kiểu dáng", vehicle?.carSeries?.carStyleName || vehicle?.carStyleName],
        ["Năm model", vehicle?.modelYear],
        ["Số chỗ", spec.numberOfSeats ? `${spec.numberOfSeats} chỗ` : ""],
        ["Nhiên liệu", spec.fuel],
        ["Xuất xứ", spec.origin],
      ],
    },
    {
      title: "Vận hành",
      rows: [
        ["Động cơ", spec.cylinderCapacity],
        ["Hộp số", spec.gearbox],
        ["Mô-men xoắn", spec.torque],
        ["Tốc độ tối đa", spec.maximumSpeed ? `${spec.maximumSpeed} km/h` : ""],
        ["Tăng tốc", spec.acceleration],
      ],
    },
    {
      title: "Kích thước",
      rows: [
        ["Dài", spec.length ? `${formatNumber(spec.length)} mm` : ""],
        ["Rộng", spec.width ? `${formatNumber(spec.width)} mm` : ""],
        ["Cao", spec.height ? `${formatNumber(spec.height)} mm` : ""],
        ["Bình nhiên liệu", spec.capacity ? `${formatNumber(spec.capacity)} L` : ""],
        ["Khối lượng toàn tải", spec.grossWeightAllowance ? `${formatNumber(spec.grossWeightAllowance)} kg` : ""],
      ],
    },
  ];
}
