import AuthForm from "@/components/AuthForm";

export default function RegisterPage() {
  return (
    <section className="section auth-page auth-video-page">
      <video className="auth-bg-video" autoPlay muted loop playsInline aria-hidden="true">
        <source src="/images/giúp_tôi_chỉnh_video_xe_này_ch.mp4" type="video/mp4" />
      </video>
      <div className="shell-container auth-layout password-change-auth">
        <div className="auth-copy">
          <p className="eyebrow">Thành viên mới</p>
          <h1>Đăng ký</h1>
          <p>Tạo tài khoản để lưu thông tin liên hệ, theo dõi lịch hẹn và nhận tư vấn liền mạch hơn.</p>
        </div>
        <AuthForm mode="register" />
      </div>
    </section>
  );
}
