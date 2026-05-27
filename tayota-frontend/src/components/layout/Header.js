"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { getMe, logout } from "@/lib/services/auth";
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from "@/lib/services/notifications";
import { getDashboardNavByPath, getDashboardTabHref, getValidDashboardTab } from "@/lib/dashboard-nav";
import { clearSession, getCurrentUser, getDashboardPath, onSessionChange, setCurrentUser } from "@/lib/session";
import { statusLabel } from "@/lib/format";

const navItems = [
  ["Sản phẩm", "/vehicles"],
  ["So sánh", "/compare"],
  ["Đại lý", "/dealerships"],
  ["Lái thử", "/appointments/test-drive"],
  ["Dịch vụ", "/appointments/service"],
];

export default function Header() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [open, setOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [notificationMessage, setNotificationMessage] = useState("");
  const [user, setUser] = useState(null);
  const [sessionReady, setSessionReady] = useState(false);

  useEffect(() => {
    let alive = true;

    async function syncUser() {
      const cachedUser = getCurrentUser();
      if (cachedUser && alive) setUser(cachedUser);

      try {
        const nextUser = await getMe();
        if (!alive) return;
        setUser(nextUser);
        setCurrentUser(nextUser);
      } catch {
        if (!alive) return;
        clearSession();
        setUser(null);
      } finally {
        if (alive) setSessionReady(true);
      }
    }

    syncUser();
    const unsubscribe = onSessionChange(syncUser);
    return () => {
      alive = false;
      unsubscribe();
    };
  }, []);

  useEffect(() => {
    setOpen(false);
    setAccountOpen(false);
    setNotificationOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!user) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }
    loadUnreadCount();
  }, [user]);

  async function loadUnreadCount() {
    try {
      const result = await getUnreadNotificationCount();
      setUnreadCount(Number(result?.unreadCount || 0));
    } catch {
      setUnreadCount(0);
    }
  }

  async function loadNotifications() {
    setNotificationsLoading(true);
    setNotificationMessage("");
    try {
      const result = await getNotifications();
      setNotifications(Array.isArray(result) ? result : result?.items || result?.content || []);
      await loadUnreadCount();
    } catch (error) {
      setNotificationMessage(error.message || "Không thể tải thông báo.");
    } finally {
      setNotificationsLoading(false);
    }
  }

  async function toggleNotifications() {
    const nextOpen = !notificationOpen;
    setNotificationOpen(nextOpen);
    setAccountOpen(false);
    if (nextOpen) await loadNotifications();
  }

  async function readNotification(notification) {
    if (!notification?.id) return;
    try {
      if (!notification.read) {
        const updated = await markNotificationAsRead(notification.id);
        setNotifications((items) => items.map((item) => (item.id === notification.id ? { ...item, ...updated, read: true } : item)));
        await loadUnreadCount();
      }
    } catch (error) {
      setNotificationMessage(error.message || "Không thể đánh dấu thông báo.");
    }
  }

  async function readAllNotifications() {
    setNotificationMessage("");
    try {
      const result = await markAllNotificationsAsRead();
      setNotifications((items) => items.map((item) => ({ ...item, read: true })));
      setUnreadCount(Number(result?.unreadCount || 0));
    } catch (error) {
      setNotificationMessage(error.message || "Không thể đánh dấu tất cả thông báo.");
    }
  }

  async function signOut() {
    try {
      await logout();
    } catch {
      // Local session cleanup still keeps the UI consistent if the server session is already gone.
    } finally {
      clearSession();
      setUser(null);
      setNotifications([]);
      setUnreadCount(0);
      setNotificationOpen(false);
      setAccountOpen(false);
      router.push("/");
      router.refresh();
    }
  }

  const dashboardPath = getDashboardPath(user?.role);
  const initials = (user?.fullname || user?.email || "T").trim().slice(0, 1).toUpperCase();
  const dashboardNavEntry = getDashboardNavByPath(pathname);
  const dashboardRole = dashboardNavEntry?.[0];
  const dashboardConfig = dashboardNavEntry?.[1];
  const showDashboardNav = Boolean(user?.role && dashboardRole === user.role && dashboardConfig);
  const activeDashboardTab = showDashboardNav ? getValidDashboardTab(dashboardRole, searchParams.get("tab")) : "";
  const activeNavItems = showDashboardNav
    ? dashboardConfig.items.map(([id, label]) => [label, getDashboardTabHref(dashboardRole, id), id])
    : navItems.map(([label, href]) => [label, href, ""]);

  function formatNotificationTime(value) {
    if (!value) return statusLabel("PENDING");
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(date);
  }

  return (
    <header className="site-header">
      <div className="shell-container header-inner">
        <Link className="brand" href="/" aria-label="Tayota trang chủ">
          TAYOTA
        </Link>

        <nav className="desktop-nav" aria-label="Điều hướng chính">
          {activeNavItems.map(([label, href, tabId]) => (
            <Link className={tabId && activeDashboardTab === tabId ? "active" : ""} key={href} href={href}>
              {label}
            </Link>
          ))}
        </nav>

        {user ? (
          <div className="header-actions">
            <div className="notification-menu">
              <button
                className="notification-button"
                type="button"
                aria-label="Mở thông báo"
                aria-expanded={notificationOpen}
                onClick={toggleNotifications}
              >
                <span className="notification-bell-icon" aria-hidden="true" />
                {unreadCount > 0 ? <span className="notification-badge">{unreadCount > 99 ? "99+" : unreadCount}</span> : null}
              </button>
              {notificationOpen ? (
                <div className="notification-dropdown">
                  <div className="notification-head">
                    <strong>Thông báo</strong>
                    <button type="button" disabled={!notifications.length} onClick={readAllNotifications}>Đã đọc tất cả</button>
                  </div>
                  {notificationMessage ? <div className="status-box compact-status">{notificationMessage}</div> : null}
                  {notificationsLoading ? <div className="status-box compact-status">Đang tải thông báo...</div> : null}
                  <div className="notification-list">
                    {notifications.map((item) => (
                      <button className={`notification-item ${item.read ? "" : "unread"}`} key={item.id} type="button" onClick={() => readNotification(item)}>
                        <span>
                          <strong>{item.title || "Thông báo"}</strong>
                          <small>{formatNotificationTime(item.createdAt)}</small>
                        </span>
                        <em>{item.content || statusLabel(item.type)}</em>
                      </button>
                    ))}
                    {!notificationsLoading && !notifications.length ? <div className="notification-empty">Không có thông báo.</div> : null}
                  </div>
                </div>
              ) : null}
            </div>

            <div className="account-menu">
              <button className="account-button" type="button" onClick={() => { setAccountOpen((value) => !value); setNotificationOpen(false); }}>
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
                  <Link href={`${dashboardPath}?tab=profile`} onClick={() => setAccountOpen(false)}>
                    Hồ sơ
                  </Link>
                  <button type="button" onClick={signOut}>
                    Đăng xuất
                  </button>
                </div>
              ) : null}
            </div>
          </div>
        ) : sessionReady ? (
          <Link className="btn btn-primary header-cta" href="/auth/login">
            Đăng nhập
          </Link>
        ) : null}

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
          {activeNavItems.map(([label, href, tabId]) => (
            <Link className={tabId && activeDashboardTab === tabId ? "active" : ""} key={href} href={href} onClick={() => setOpen(false)}>
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
