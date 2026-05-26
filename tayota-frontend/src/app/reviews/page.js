import { notFound, redirect } from "next/navigation";

export default function ReviewQueryPage({ searchParams }) {
  const token = searchParams?.token;

  if (typeof token !== "string" || !token.trim()) {
    notFound();
  }

  redirect(`/reviews/${encodeURIComponent(token)}`);
}
