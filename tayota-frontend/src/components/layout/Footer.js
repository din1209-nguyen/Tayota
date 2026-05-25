import Link from "next/link";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="shell-container footer-grid">
        <div>
          <Link className="brand footer-brand" href="/">
            TAYOTA
          </Link>
          <p>Không gian số dành cho khám phá xe, đặt lịch lái thử và chăm sóc sở hữu theo cách riêng tư.</p>
        </div>
        <div>
          <h2>Trải nghiệm</h2>
          <Link href="/vehicles">Sản phẩm</Link>
          <Link href="/compare">So sánh</Link>
          <Link href="/appointments/test-drive">Lái thử</Link>
        </div>
        <div>
          <h2>Hỗ trợ</h2>
          <Link href="/appointments/service">Dịch vụ</Link>
          <Link href="/support/live-chat">Live chat</Link>
          <Link href="#ai-chat">AI tư vấn</Link>
        </div>
      </div>
    </footer>
  );
}
