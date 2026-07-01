import NotificationCenter from "@/components/notifications/NotificationCenter";

export const metadata = {
  title: "Thông báo | Tayota",
};

export default function NotificationsPage() {
  return (
    <section className="section ops-page notification-page">
      <div className="shell-container">
        <NotificationCenter />
      </div>
    </section>
  );
}
