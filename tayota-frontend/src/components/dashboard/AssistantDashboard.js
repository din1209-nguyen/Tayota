"use client";

import { useSearchParams } from "next/navigation";
import ProfilePanel from "@/components/dashboard/ProfilePanel";
import StaffChatWorkspace from "@/components/dashboard/StaffChatWorkspace";
import { getValidDashboardTab } from "@/lib/dashboard-nav";

export default function AssistantDashboard() {
  const searchParams = useSearchParams();
  const tab = getValidDashboardTab("ASSISTANT", searchParams.get("tab"));

  if (tab === "profile") {
    return (
      <div className="ops-grid workspace-tabs-layout">
        <ProfilePanel eyebrow="Assistant" heading="Hồ sơ cá nhân" />
      </div>
    );
  }

  return (
    <StaffChatWorkspace emptyPanelMessage="Nhân viên tư vấn chỉ xử lý live chat realtime, không quản lý lịch hẹn." />
  );
}
