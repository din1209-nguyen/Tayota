export const DASHBOARD_NAV = {
  ADMIN: {
    basePath: "/dashboard/admin",
    defaultTab: "accounts",
    items: [
      ["accounts", "Tài khoản"],
      ["documents", "Dữ liệu AI"],
    ],
  },
  MANAGER: {
    basePath: "/dashboard/manager",
    defaultTab: "chat",
    items: [
      ["chat", "Live Chat"],
      ["vehicles", "Xe"],
      ["articles", "Bài viết"],
      ["dealerships", "Đại lý"],
      ["accessories", "Phụ kiện"],
      ["users", "Người dùng"],
    ],
  },
  SERVICE_ADVISOR: {
    basePath: "/dashboard/advisor",
    defaultTab: "overview",
    items: [
      ["overview", "Tổng quan"],
      ["appointments", "Lịch hẹn"],
      ["walkin", "Walk-in"],
      ["vehicles", "Gán xe"],
      ["slots", "Khung giờ"],
      ["holidays", "Ngày nghỉ"],
      ["tickets", "Phiếu"],
      ["reports", "Báo cáo"],
    ],
  },
  ASSISTANT: {
    basePath: "/dashboard/assistant",
    defaultTab: "chat",
    items: [["chat", "Live Chat"]],
  },
  MECHANIC: {
    basePath: "/dashboard/mechanic",
    defaultTab: "queue",
    items: [
      ["queue", "Cần tiếp nhận"],
      ["active", "Đang sửa"],
      ["history", "Lịch sử"],
      ["reviews", "Đánh giá"],
    ],
  },
};

export function getDashboardNavByPath(pathname = "") {
  return Object.entries(DASHBOARD_NAV).find(([, config]) => pathname.startsWith(config.basePath)) || null;
}

export function getValidDashboardTab(role, value) {
  const config = DASHBOARD_NAV[role];
  if (!config) return "";
  return config.items.some(([id]) => id === value) ? value : config.defaultTab;
}

export function getDashboardTabHref(role, tab) {
  const config = DASHBOARD_NAV[role];
  if (!config) return "/dashboard";
  return `${config.basePath}?tab=${encodeURIComponent(tab)}`;
}
