import Link from "next/link";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="shell-container footer-grid">
        <div className="footer-brand-block">
          <Link className="brand footer-brand" href="/">
            TAYOTA
          </Link>
          <p>Không gian số dành cho khám phá xe, so sánh phiên bản, đặt lịch lái thử và chăm sóc sở hữu theo cách riêng tư.</p>
          <div className="footer-contact">
            <span>Hotline: 1900 5588</span>
            <span>Email: care@tayota.vn</span>
          </div>
        </div>
        <nav aria-label="Khám phá">
          <h2>Khám phá</h2>
          <Link href="/vehicles">Dòng xe</Link>
          <Link href="/compare">So sánh</Link>
          <Link href="/dealerships">Đại lý</Link>
        </nav>
        <nav aria-label="Dịch vụ">
          <h2>Dịch vụ</h2>
          <Link href="/appointments/test-drive">Đặt lái thử</Link>
          <Link href="/appointments/service">Chăm sóc xe</Link>
          <Link href="/support/live-chat">Trò chuyện trực tuyến</Link>
        </nav>
        <nav aria-label="Tin tức và hỗ trợ">
          <h2>Tin tức & hỗ trợ</h2>
          <Link href="/news">Bài viết</Link>
          <Link href="/notifications">Thông báo</Link>
          <Link href="#ai-chat">AI tư vấn</Link>
        </nav>
      </div>

      <div className="shell-container footer-bottom">
        <span>© Tayota digital showroom</span>
        <span>Danh mục xe, đại lý và dịch vụ trong một trải nghiệm thống nhất.</span>
      </div>
    </footer>
  );
}
