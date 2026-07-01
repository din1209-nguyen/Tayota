import Link from "next/link";
import { getPublishedNews } from "@/lib/services/manager";
import { unwrapList } from "@/lib/format";

export const metadata = {
  title: "Tin tức | Tayota",
};
export const dynamic = "force-dynamic";

export default async function NewsPage() {
  let articles = [];
  let error = "";
  try {
    articles = unwrapList(await getPublishedNews());
  } catch (caughtError) {
    error = caughtError.message;
  }

  return (
    <section className="section news-page">
      <div className="shell-container page-title">
        <p className="eyebrow">Tin tức Tayota</p>
        <h1>Câu chuyện và cập nhật mới</h1>
        <p>Khám phá công nghệ, phong cách sống, phụ kiện và thông tin sản phẩm từ Tayota.</p>
      </div>
      <div className="shell-container news-grid">
        {error ? <div className="status-box wide">{error}</div> : null}
        {!error && !articles.length ? <div className="status-box wide">Chưa có bài viết được xuất bản.</div> : null}
        {articles.map((article) => (
          <article className="news-card card" key={article.id}>
            {article.imageUrl ? <div className="news-image" style={{ backgroundImage: `url(${article.imageUrl})` }} /> : null}
            <div>
              <p className="eyebrow">{article.type}</p>
              <h2>{article.title}</h2>
              <p>{article.content.length > 160 ? `${article.content.slice(0, 160)}...` : article.content}</p>
              <Link className="btn btn-ghost" href={`/news/${article.id}`}>Đọc bài viết</Link>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
