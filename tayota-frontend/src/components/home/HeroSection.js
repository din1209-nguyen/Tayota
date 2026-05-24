import Link from "next/link";

export default function HeroSection() {
  return (
    <section className="hero">
      <div className="hero-bg" />
      <div className="shell-container hero-content">
        <p className="eyebrow">Showroom số cao cấp</p>
        <h1>TAYOTA</h1>
        <p>
          Chuẩn mực mới của chuyển động sang trọng, từ việc khám phá dòng xe đến
          lịch lái thử riêng tư trong vài thao tác.
        </p>
        <div className="hero-actions">
          <Link className="btn btn-secondary" href="/vehicles">
            Khám phá dòng xe
          </Link>
          <Link className="btn btn-primary hero-dark" href="/appointments/test-drive">
            Đặt lái thử riêng
          </Link>
        </div>
        <div className="hero-telemetry" aria-label="Điểm nhấn công nghệ">
          <span>HYBRID DRIVE</span>
          <span>TSS READY</span>
          <span>CONNECTED SERVICE</span>
        </div>
      </div>
    </section>
  );
}
