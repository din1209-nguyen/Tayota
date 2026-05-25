"use client";

import { useEffect, useState } from "react";
import { getReviewByToken, submitReviewByToken } from "@/lib/services/reviews";

export default function ReviewTokenForm({ token }) {
  const [review, setReview] = useState(null);
  const [form, setForm] = useState({
    serviceRating: 5,
    serviceComment: "",
    mechanicRating: 5,
    mechanicComment: "",
  });
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getReviewByToken(token)
      .then(setReview)
      .catch((error) => setMessage(error.message))
      .finally(() => setLoading(false));
  }, [token]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: name.includes("Rating") ? Number(value) : value,
    }));
  }

  async function submit(event) {
    event.preventDefault();
    setMessage("");
    try {
      const result = await submitReviewByToken(token, form);
      setReview(result);
      setMessage("Cảm ơn bạn đã gửi đánh giá.");
    } catch (error) {
      setMessage(error.message);
    }
  }

  return (
    <form className="form-panel" onSubmit={submit}>
      {loading ? <p>Đang tải đánh giá...</p> : null}
      {review ? <div className="status-box">{review.reviewType || "REVIEW"} · {review.status || "PENDING"}</div> : null}
      <label className="label">
        Điểm dịch vụ
        <input className="field" type="number" min="1" max="5" name="serviceRating" value={form.serviceRating} onChange={updateField} />
      </label>
      <label className="label">
        Nhận xét dịch vụ
        <textarea className="field" name="serviceComment" rows={4} value={form.serviceComment} onChange={updateField} />
      </label>
      <label className="label">
        Điểm kỹ thuật viên
        <input className="field" type="number" min="1" max="5" name="mechanicRating" value={form.mechanicRating} onChange={updateField} />
      </label>
      <label className="label">
        Nhận xét kỹ thuật viên
        <textarea className="field" name="mechanicComment" rows={4} value={form.mechanicComment} onChange={updateField} />
      </label>
      {message ? <div className="status-box">{message}</div> : null}
      <button className="btn btn-primary" type="submit">Gửi đánh giá</button>
    </form>
  );
}
