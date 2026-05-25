"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { getMe, logout } from "@/lib/services/auth";
import { clearSession, getAccessToken, getCurrentUser, getDashboardPath, onSessionChange, setCurrentUser } from "@/lib/session";

const navItems = [
  ["Dòng xe", "/vehicles"],
  ["So sánh", "/compare"],
  ["Đại lý", "/dealerships"],
  ["Lái thử", "/appointments/test-drive"],
  ["Dịch vụ", "/appointments/service"],
];

export default function Header() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [user, setUser] = useState(null);

  useEffect(() => {
    let alive = true;

    async function syncUser() {
      const cachedUser = getCurrentUser();
      if (cachedUser && alive) setUser(cachedUser);

      if (!getAccessToken()) {
        if (alive) setUser(null);
        return;
      }

      try {
        const nextUser = await getMe();
        if (!alive) return;
        setUser(nextUser);
        setCurrentUser(nextUser);
      } catch {
        if (!alive) return;
        clearSession();
        setUser(null);
      }
    }

    syncUser();
    const unsubscribe = onSessionChange(syncUser);
    return () => {
      alive = false;
      unsubscribe();
    };
  }, []);

  async function signOut() {
    try {
      await logout();
    } catch {
      // Local session cleanup still keeps the UI consistent if the server session is already gone.
    } finally {
      clearSession();
      setUser(null);
      setAccountOpen(false);
      router.push("/");
      router.refresh();
    }
  }

  const dashboardPath = getDashboardPath(user?.role);
  const initials = (user?.fullname || user?.email || "T").trim().slice(0, 1).toUpperCase();

  return (
    <header className="site-header">
      <div className="shell-container header-inner">
        <Link className="brand" href="/" aria-label="Tayota trang chủ">
          TAYOTA
        </Link>

        <nav className="desktop-nav" aria-label="Điều hướng chính">
          {navItems.map(([label, href]) => (
            <Link key={href} href={href}>
              {label}
            </Link>
          ))}
        </nav>

        {user ? (
          <div className="account-menu">
            <button className="account-button" type="button" onClick={() => setAccountOpen((value) => !value)}>
              <span className="avatar" style={user.avatarUrl ? { backgroundImage: `url(${user.avatarUrl})` } : undefined}>
                {user.avatarUrl ? "" : initials}
              </span>
              <span className="account-name">{user.fullname || user.email}</span>
            </button>
            {accountOpen ? (
              <div className="account-dropdown">
                <span className="account-role">{user.role || "USER"}</span>
                <Link href={dashboardPath} onClick={() => setAccountOpen(false)}>
                  Dashboard
                </Link>
                <Link href="/dashboard/user" onClick={() => setAccountOpen(false)}>
                  Hồ sơ
                </Link>
                <button type="button" onClick={signOut}>
                  Đăng xuất
                </button>
              </div>
            ) : null}
          </div>
        ) : (
          <Link className="btn btn-primary header-cta" href="/auth/login">
            Đăng nhập
          </Link>
        )}

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
          <Link className="btn btn-primary" href={user ? dashboardPath : "/auth/login"} onClick={() => setOpen(false)}>
            {user ? "Dashboard" : "Đăng nhập"}
          </Link>
        </div>
      ) : null}
    </header>
  );
}
