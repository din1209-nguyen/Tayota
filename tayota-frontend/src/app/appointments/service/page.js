import AppointmentForm from "@/components/appointments/AppointmentForm";

export default function ServiceAppointmentPage() {
  return (
    <section className="section">
      <div className="shell-container appointment-layout">
        <div>
          <p className="eyebrow">Dịch vụ sở hữu</p>
          <h1>Đặt lịch chăm sóc xe</h1>
          <p>Nhập thông tin xe và thời gian mong muốn để trung tâm dịch vụ sắp xếp tiếp nhận.</p>
        </div>
        <AppointmentForm type="service" />
      </div>
    </section>
  );
}
