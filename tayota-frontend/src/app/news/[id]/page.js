import Link from "next/link";
import { getPublishedArticle } from "@/lib/services/manager";

export default async function NewsDetailPage({ params }) {
  const { id } = await params;
  let article = null;
  let error = "";
  try {
    article = await getPublishedArticle(id);
  } catch (caughtError) {
    error = caughtError.message;
  }

  if (error) {
    return <section className="section"><div className="shell-container status-box">{error}</div></section>;
  }

  return (
    <article className="section news-detail">
      <div className="shell-container news-article">
        <p className="eyebrow">{article.type}</p>
        <h1>{article.title}</h1>
        {article.imageUrl ? <div className="news-detail-image" style={{ backgroundImage: `url(${article.imageUrl})` }} /> : null}
        <div className="news-copy">{article.content}</div>
        <Link className="btn btn-ghost" href="/news">Quay lại tin tức</Link>
      </div>
    </article>
  );
}
