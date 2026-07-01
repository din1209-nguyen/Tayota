import AdminUserDetail from "@/components/dashboard/AdminUserDetail";

export const metadata = {
  title: "Chi tiết tài khoản | Tayota",
};

export default async function AdminUserDetailPage({ params }) {
  const { userId } = await params;

  return (
    <section className="section ops-page">
      <div className="shell-container admin-shell-container">
        <AdminUserDetail userId={userId} />
      </div>
    </section>
  );
}
