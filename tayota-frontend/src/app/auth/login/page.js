import AuthForm from "@/components/AuthForm";

export default function LoginPage() {
  return (
    <section className="section auth-page">
      <div className="shell-container auth-layout">
        <div>
          <p className="eyebrow">Tài khoản</p>
          <h1>Đăng nhập TAYOTA</h1>
          <p>Quản lý lịch hẹn và tiếp tục hành trình tư vấn với thông tin cá nhân của bạn.</p>
        </div>
        <AuthForm mode="login" />
      </div>
    </section>
  );
}
