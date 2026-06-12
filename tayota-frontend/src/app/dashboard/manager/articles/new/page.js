import { ManagerArticleEditorPage } from "@/components/dashboard/ManagerContentPanels";

export const metadata = {
  title: "Thêm bài viết | Manager Dashboard",
};

export default function NewManagerArticlePage() {
  return (
    <section className="section ops-page manager-dashboard-page">
      <div className="manager-dashboard-shell">
        <ManagerArticleEditorPage articleId="new" />
      </div>
    </section>
  );
}
