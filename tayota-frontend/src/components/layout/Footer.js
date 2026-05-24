import Link from "next/link";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="shell-container footer-grid">
        <div>
          <Link className="brand footer-brand" href="/">
            TAYOTA
          </Link>
          <p>
            Không gian số dành cho việc khám phá xe, đặt lịch lái thử và chăm
            sóc sở hữu một cách riêng tư.
          </p>
        </div>
        <div>
          <h2>Trải nghiệm</h2>
          <Link href="/vehicles">Dòng xe</Link>
          <Link href="/compare">So sánh</Link>
          <Link href="/appointments/test-drive">Lái thử</Link>
        </div>
        <div>
          <h2>Hỗ trợ</h2>
          <Link href="/appointments/service">Dịch vụ</Link>
          <Link href="/auth/login">Tài khoản</Link>
          <Link href="#ai-chat">AI tư vấn</Link>
        </div>
      </div>
    </footer>
  );
}
