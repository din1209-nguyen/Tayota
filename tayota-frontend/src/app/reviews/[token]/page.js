import ReviewTokenForm from "@/components/reviews/ReviewTokenForm";

export const metadata = {
  title: "Review | TAYOTA",
};

export default async function ReviewTokenPage({ params }) {
  const { token } = await params;

  return (
    <section className="section ops-page">
      <div className="shell-container appointment-layout">
        <div>
          <p className="eyebrow">Review</p>
          <h1>Đánh giá trải nghiệm</h1>
          <p>Phản hồi của bạn giúp đại lý cải thiện dịch vụ sau mỗi lịch hẹn.</p>
        </div>
        <ReviewTokenForm token={token} />
      </div>
    </section>
  );
}
