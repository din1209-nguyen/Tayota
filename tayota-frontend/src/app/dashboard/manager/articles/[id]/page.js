import { ManagerArticleEditorPage } from "@/components/dashboard/ManagerContentPanels";

export const metadata = {
  title: "Sửa bài viết | Manager Dashboard",
};

export default async function EditManagerArticlePage({ params }) {
  const { id } = await params;

  return (
    <section className="section ops-page manager-dashboard-page">
      <div className="manager-dashboard-shell">
        <ManagerArticleEditorPage articleId={id} />
      </div>
    </section>
  );
}
