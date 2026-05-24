import AuthForm from "@/components/AuthForm";

export default function RegisterPage() {
  return (
    <section className="section auth-page">
      <div className="shell-container auth-layout">
        <div>
          <p className="eyebrow">Thành viên mới</p>
          <h1>Tạo tài khoản TAYOTA</h1>
          <p>Đăng ký để lưu thông tin liên hệ, theo dõi lịch hẹn và nhận tư vấn liền mạch hơn.</p>
        </div>
        <AuthForm mode="register" />
      </div>
    </section>
  );
}
