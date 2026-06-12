import PasswordChangePanel from "@/components/dashboard/PasswordChangePanel";

export const metadata = {
  title: "Đổi mật khẩu | Tayota",
};

export default function ChangePasswordPage() {
  return (
    <section className="section auth-page auth-video-page password-change-page">
      <video className="auth-bg-video" autoPlay muted loop playsInline aria-hidden="true">
        <source src="/images/giúp_tôi_chỉnh_video_xe_này_ch.mp4" type="video/mp4" />
      </video>
      <div className="shell-container">
        <PasswordChangePanel />
      </div>
    </section>
  );
}
