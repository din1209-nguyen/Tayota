"use client";

import Link from "next/link";
import { useState } from "react";

const navItems = [
  ["Dòng xe", "/vehicles"],
  ["So sánh", "/compare"],
  ["Lái thử", "/appointments/test-drive"],
  ["Dịch vụ", "/appointments/service"],
  ["AI tư vấn", "#ai-chat"],
  ["Tài khoản", "/auth/login"],
];

export default function Header() {
  const [open, setOpen] = useState(false);

  return (
    <header className="site-header">
      <div className="shell-container header-inner">
        <Link className="brand" href="/" aria-label="TAYOTA trang chủ">
          TAYOTA
        </Link>
        <nav className="desktop-nav" aria-label="Điều hướng chính">
          {navItems.map(([label, href]) => (
            <Link key={href} href={href}>
              {label}
            </Link>
          ))}
        </nav>
        <Link className="btn btn-primary header-cta" href="/appointments/test-drive">
          Đặt lịch riêng
        </Link>
        <button
          className="menu-button"
          type="button"
          aria-label="Mở menu"
          aria-expanded={open}
          onClick={() => setOpen((value) => !value)}
        >
          <span />
          <span />
          <span />
        </button>
      </div>
      {open ? (
        <div className="mobile-nav">
          {navItems.map(([label, href]) => (
            <Link key={href} href={href} onClick={() => setOpen(false)}>
              {label}
            </Link>
          ))}
          <Link
            className="btn btn-primary"
            href="/appointments/test-drive"
            onClick={() => setOpen(false)}
          >
            Tư vấn 1:1
          </Link>
        </div>
      ) : null}
    </header>
  );
}
