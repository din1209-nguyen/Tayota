import AuthForm from "@/components/AuthForm";

export default function LoginPage() {
  return (
    <section className="section auth-page auth-video-page">
      <video className="auth-bg-video" autoPlay muted loop playsInline aria-hidden="true">
        <source src="/images/giúp_tôi_chỉnh_video_xe_này_ch.mp4" type="video/mp4" />
      </video>
      <div className="shell-container auth-layout password-change-auth">
        <div className="auth-copy">
          <p className="eyebrow">Tài khoản</p>
          <h1>Đăng nhập</h1>
          <p>Quản lý lịch hẹn, hồ sơ xe và tiếp tục tư vấn với tài khoản của bạn.</p>
        </div>
        <AuthForm mode="login" />
      </div>
    </section>
  );
}
