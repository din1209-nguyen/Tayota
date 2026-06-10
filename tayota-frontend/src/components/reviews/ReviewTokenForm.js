"use client";

import { useEffect, useMemo, useState } from "react";
import { getReviewByToken, submitReviewByToken } from "@/lib/services/reviews";
import { statusLabel, statusPillClass } from "@/lib/format";

function RatingInput({ label, name, value, onChange, disabled }) {
  return (
    <label className="label rating-field">
      {label}
      <div className="rating-options" role="radiogroup" aria-label={label}>
        {[1, 2, 3, 4, 5].map((score) => (
          <button
            className={Number(value) === score ? "selected" : ""}
            key={score}
            type="button"
            disabled={disabled}
            onClick={() => onChange(name, score)}
          >
            {score}
          </button>
        ))}
      </div>
    </label>
  );
}

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
  const [submitting, setSubmitting] = useState(false);

  const isServiceReview = review?.reviewType === "SERVICE";
  const submitted = review?.status === "SUBMITTED";
  const expired = review?.status === "EXPIRED";
  const disabled = submitting || submitted || expired;
  const contextTitle = useMemo(() => {
    if (!review) return "Đánh giá dịch vụ chăm sóc xe";
    if (review.reviewType === "TEST_DRIVE") return "Đánh giá buổi hẹn lái thử";
    return "Đánh giá dịch vụ chăm sóc xe";
  }, [review]);
  const serviceRatingLabel = isServiceReview ? "Điểm dịch vụ chăm sóc xe" : "Điểm buổi hẹn lái thử";
  const serviceCommentLabel = isServiceReview ? "Nhận xét dịch vụ chăm sóc xe" : "Nhận xét buổi hẹn lái thử";

  useEffect(() => {
    let alive = true;

    getReviewByToken(token)
      .then((result) => {
        if (!alive) return;
        setReview(result);
        setForm({
          serviceRating: result?.serviceRating || 5,
          serviceComment: result?.serviceComment || "",
          mechanicRating: result?.mechanicRating || 5,
          mechanicComment: result?.mechanicComment || "",
        });
      })
      .catch((error) => {
        if (alive) setMessage(error.message || "Không thể tải form đánh giá.");
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [token]);

  function setField(name, value) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  function updateField(event) {
    const { name, value } = event.target;
    setField(name, value);
  }

  async function submit(event) {
    event.preventDefault();
    if (disabled) return;

    setSubmitting(true);
    setMessage("");
    try {
      const payload = {
        serviceRating: Number(form.serviceRating),
        serviceComment: form.serviceComment.trim(),
        mechanicRating: isServiceReview ? Number(form.mechanicRating) : null,
        mechanicComment: isServiceReview ? form.mechanicComment.trim() : null,
      };
      const result = await submitReviewByToken(token, payload);
      setReview(result);
      setMessage("Cảm ơn bạn đã gửi đánh giá.");
    } catch (error) {
      setMessage(error.message || "Không thể gửi đánh giá.");
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <section className="form-panel review-panel"><div className="status-box">Đang tải đánh giá...</div></section>;
  }

  if (!review) {
    return <section className="form-panel review-panel"><div className="status-box">{message || "Không tìm thấy form đánh giá."}</div></section>;
  }

  return (
    <form className="form-panel review-panel" onSubmit={submit}>
      <div className="ops-panel-head review-panel-head">
        <div>
          <h2>{contextTitle}</h2>
          <p>Chia sẻ cảm nhận của bạn để Tayota phục vụ tốt hơn trong những lần hẹn tiếp theo.</p>
        </div>
        <span className={statusPillClass(review.status)}>{statusLabel(review.status)}</span>
      </div>

      <dl className="summary-list">
        <div><dt>Mã lịch</dt><dd>{review.appointmentId || review.serviceId || "Đang cập nhật"}</dd></div>
        {review.vinId ? <div><dt>VIN</dt><dd>{review.vinId}</dd></div> : null}
        {review.customerFullName ? <div><dt>Khách hàng</dt><dd>{review.customerFullName}</dd></div> : null}
        <div><dt>Loại đánh giá</dt><dd>{review.reviewType === "SERVICE" ? "Dịch vụ" : "Lái thử"}</dd></div>
      </dl>

      <RatingInput label={serviceRatingLabel} name="serviceRating" value={form.serviceRating} onChange={setField} disabled={disabled} />
      <label className="label">
        {serviceCommentLabel}
        <textarea className="field" name="serviceComment" rows={4} value={form.serviceComment} onChange={updateField} disabled={disabled} />
      </label>

      {isServiceReview ? (
        <>
          <RatingInput label="Điểm kỹ thuật viên" name="mechanicRating" value={form.mechanicRating} onChange={setField} disabled={disabled} />
          <label className="label">
            Nhận xét kỹ thuật viên
            <textarea className="field" name="mechanicComment" rows={4} value={form.mechanicComment} onChange={updateField} disabled={disabled} />
          </label>
        </>
      ) : null}

      {message ? <div className="status-box">{message}</div> : null}
      <button className="btn btn-primary" type="submit" disabled={disabled}>
        {submitted ? "Đã gửi đánh giá" : submitting ? "Đang gửi..." : "Gửi đánh giá"}
      </button>
    </form>
  );
}
