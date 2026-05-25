import { formatNumber } from "@/lib/format";

const SPEC_LABELS = [
  ["origin", "Xuất xứ"],
  ["fuel", "Nhiên liệu"],
  ["numberOfSeats", "Số chỗ", (value) => `${value} chỗ`],
  ["length", "Dài", (value) => formatNumber(value, "mm")],
  ["width", "Rộng", (value) => formatNumber(value, "mm")],
  ["height", "Cao", (value) => formatNumber(value, "mm")],
  ["capacity", "Dung tích bình nhiên liệu", (value) => formatNumber(value, "L")],
  ["cylinderCapacity", "Dung tích xy-lanh"],
  ["cylinder", "Số xy-lanh"],
  ["gearbox", "Hộp số"],
  ["maximumSpeed", "Tốc độ tối đa", (value) => formatNumber(value, "km/h")],
  ["acceleration", "Tăng tốc"],
  ["torque", "Mô-men xoắn"],
  ["grossWeightAllowance", "Khối lượng toàn tải", (value) => formatNumber(value, "kg")],
  ["trademarks", "Thương hiệu"],
];

export function getSpecificationRows(specification = {}) {
  return SPEC_LABELS.map(([key, label, formatter]) => {
    const value = specification?.[key];
    if (value === null || value === undefined || value === "") return null;
    return {
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
