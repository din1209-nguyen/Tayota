import AppointmentForm from "@/components/appointments/AppointmentForm";

export default async function TestDrivePage({ searchParams }) {
  const params = await searchParams;

  return (
    <section className="test-drive-page">
      <div className="test-drive-backdrop" aria-hidden="true" />
      <div className="shell-container test-drive-layout test-drive-layout-large">
        <div className="test-drive-copy">
          <p className="eyebrow">Lái thử riêng tư</p>
          <h1>Đặt lịch trải nghiệm xe Tayota</h1>
          <p>Chọn xe, đại lý, ngày và khung giờ phù hợp. Đội ngũ tư vấn sẽ xác nhận lại trước khi đón tiếp.</p>
        </div>
        <div className="test-drive-form">
          <AppointmentForm type="test-drive" defaultCarVersionId={params?.carVersionId || ""} />
        </div>
      </div>
    </section>
  );
}
