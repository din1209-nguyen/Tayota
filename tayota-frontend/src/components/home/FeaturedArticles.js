import Link from "next/link";
import { getPublishedNews } from "@/lib/services/manager";
import { unwrapList } from "@/lib/format";

async function getArticles() {
  try {
    const articles = unwrapList(await getPublishedNews());
    return { articles: articles.slice(0, 3), error: "" };
  } catch (error) {
    return { articles: [], error: error.message || "Chưa thể tải bài viết." };
  }
}

export default async function FeaturedArticles() {
  const { articles, error } = await getArticles();

  return (
    <section className="section home-articles">
      <div className="shell-container section-heading">
        <div>
          <p className="eyebrow">Tin tức & tư vấn</p>
          <h2>Bài viết nổi bật từ Tayota</h2>
        </div>
        <Link className="btn btn-ghost" href="/news">
          Xem tất cả
        </Link>
      </div>
      <div className="shell-container home-article-grid">
        {error ? <div className="status-box wide">{error}</div> : null}
        {!error && !articles.length ? <div className="status-box wide">Chưa có bài viết được xuất bản.</div> : null}
        {articles.map((article, index) => (
          <article className={`home-article-card card ${index === 0 ? "featured" : ""}`} key={article.id}>
            {article.imageUrl ? <div className="home-article-image" style={{ backgroundImage: `url(${article.imageUrl})` }} /> : null}
            <div className="home-article-copy">
              <p className="eyebrow">{article.type}</p>
              <h3>{article.title}</h3>
              <p>{article.content?.length > 150 ? `${article.content.slice(0, 150)}...` : article.content}</p>
              <Link className="btn btn-ghost" href={`/news/${article.id}`}>
                Đọc bài viết
              </Link>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
