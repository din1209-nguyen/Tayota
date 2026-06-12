import Link from "next/link";

export default function ExperienceBand() {
  return (
    <section className="section home-experience">
      <div className="shell-container home-experience-inner">
        <div className="home-experience-media" />
        <div className="home-experience-copy">
          <p className="eyebrow">Tư vấn thông minh</p>
          <h2>Một hành trình mua xe rõ ràng hơn từ showroom đến dịch vụ</h2>
          <p>
            Tayota gom catalog, so sánh phiên bản, lịch lái thử, đại lý và chăm sóc sau bán hàng vào một trải nghiệm liền mạch để khách
            hàng ra quyết định tự tin hơn.
          </p>
          <div className="home-experience-points">
            <span>Catalog theo nhu cầu</span>
            <span>Đại lý gần bạn</span>
            <span>Lịch hẹn minh bạch</span>
          </div>
          <div className="home-experience-actions">
            <Link className="btn btn-primary" href="/vehicles">
              Xem dòng xe
            </Link>
            <Link className="btn btn-ghost" href="/dealerships">
              Tìm đại lý
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
