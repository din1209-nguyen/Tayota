import AppointmentForm from "@/components/appointments/AppointmentForm";

export default function ServiceAppointmentPage() {
  return (
    <section className="section appointment-page">
      <div className="shell-container appointment-layout appointment-layout-large">
        <div className="appointment-copy">
          <p className="eyebrow">Dịch vụ sở hữu</p>
          <h1>Đặt lịch chăm sóc xe</h1>
          <p>Nhập VIN, chọn đại lý và khung giờ để trung tâm dịch vụ chuẩn bị tiếp nhận xe của bạn.</p>
        </div>
        <div className="appointment-form-shell">
          <AppointmentForm type="service" />
        </div>
      </div>
    </section>
  );
}
