export const DASHBOARD_NAV = {
  ADMIN: {
    basePath: "/dashboard/admin",
    defaultTab: "profile",
    items: [
      ["profile", "Tài khoản"],
      ["accounts", "Quản lý tài khoản"],
      ["documents", "Dữ liệu AI"],
    ],
  },
  MANAGER: {
    basePath: "/dashboard/manager",
    defaultTab: "profile",
    items: [
      ["profile", "Tài khoản"],
      ["chat", "Trò chuyện trực tuyến"],
      ["vehicles", "Xe"],
      ["articles", "Bài viết"],
      ["dealerships", "Đại lý"],
      ["accessories", "Phụ kiện"],
      ["users", "Người dùng"],
    ],
  },
  SERVICE_ADVISOR: {
    basePath: "/dashboard/advisor",
    defaultTab: "profile",
    items: [
      ["profile", "Tài khoản"],
      ["overview", "Tổng quan"],
      ["appointments", "Lịch hẹn"],
      ["tickets", "Phiếu dịch vụ"],
      ["vehicles", "Gán xe"],
      ["slots", "Khung giờ"],
      ["holidays", "Ngày nghỉ"],
      ["reports", "Báo cáo"],
    ],
  },
  ASSISTANT: {
    basePath: "/dashboard/assistant",
    defaultTab: "profile",
    items: [["profile", "Tài khoản"], ["chat", "Trò chuyện trực tuyến"]],
  },
  MECHANIC: {
    basePath: "/dashboard/mechanic",
    defaultTab: "profile",
    items: [
      ["profile", "Tài khoản"],
      ["queue", "Cần tiếp nhận"],
      ["active", "Đang sửa"],
      ["history", "Lịch sử"],
      ["reviews", "Đánh giá"],
    ],
  },
  USER: {
    basePath: "/dashboard/user",
    defaultTab: "profile",
    items: [
      ["profile", "Tài khoản"],
      ["vehicles", "Xe cá nhân"],
      ["appointments", "Lịch của tôi"],
      ["services", "Dịch vụ xe của tôi"],
      ["reviews", "Đánh giá"],
      ["chat", "Trò chuyện trực tuyến"],
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
