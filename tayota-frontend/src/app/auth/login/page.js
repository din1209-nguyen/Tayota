import AuthForm from "@/components/AuthForm";

export default function LoginPage() {
  return (
    <section className="section auth-page">
      <div className="shell-container auth-layout">
        <div className="auth-copy">
          <p className="eyebrow">Tài khoản</p>
          <h1>Đăng nhập Tayota</h1>
          <p>Quản lý lịch hẹn, lưu xe yêu thích và tiếp tục tư vấn với thông tin cá nhân của bạn.</p>
        </div>
        <AuthForm mode="login" />
      </div>
    </section>
  );
}
