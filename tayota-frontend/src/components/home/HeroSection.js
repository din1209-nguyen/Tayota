import Link from "next/link";

export default function HeroSection() {
  return (
    <section className="hero">
      <video className="hero-video" autoPlay muted loop playsInline poster="/images/camry-luxury-hero.png">
        <source src="/images/sửa_lại_nội_dung_từ_lúc_chạy_r.mp4" type="video/mp4" />
      </video>
      <div className="shell-container hero-content">
        <p className="eyebrow">Tayota showroom số</p>
        <h1>Chọn xe đúng nhu cầu</h1>
        <p>So sánh phiên bản, đặt lịch lái thử và kết nối đại lý trong vài bước.</p>
        <div className="hero-actions">
          <Link className="btn btn-primary" href="/vehicles">
            Khám phá xe
          </Link>
          <Link className="btn btn-ghost hero-dark" href="/appointments/test-drive">
            Đặt lịch lái thử
          </Link>
        </div>
        <div className="hero-telemetry" aria-label="Điểm nổi bật của Tayota">
          <span>So sánh phiên bản</span>
          <span>Đại lý toàn quốc</span>
          <span>Tư vấn thông minh</span>
        </div>
      </div>
    </section>
  );
}
