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

function NotificationIcon() {
  return (
    <svg className="notification-bell-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M18 9.6c0-3.1-2.4-5.6-6-5.6S6 6.5 6 9.6c0 6-2.2 6.6-2.2 7.7 0 .5.4.9.9.9h14.6c.5 0 .9-.4.9-.9 0-1.1-2.2-1.7-2.2-7.7Z" />
      <path d="M9.8 19.4a2.4 2.4 0 0 0 4.4 0" />
      <path d="M12 2.8v1.4" />
    </svg>
  );
}

export default function Header() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [open, setOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const [selectedNotification, setSelectedNotification] = useState(null);
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
    let nextNotification = notification;
    try {
      if (!notification.read) {
        const updated = await markNotificationAsRead(notification.id);
        nextNotification = { ...notification, ...updated, read: true };
        setNotifications((items) => items.map((item) => (item.id === notification.id ? nextNotification : item)));
        await loadUnreadCount();
      }
      setSelectedNotification(nextNotification);
      setNotificationOpen(false);
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
                <NotificationIcon />
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

      {selectedNotification ? (
        <div className="manager-modal-backdrop detail-modal-backdrop" role="presentation" onClick={() => setSelectedNotification(null)}>
          <section className="detail-modal" role="dialog" aria-modal="true" aria-labelledby="notification-detail-title" onClick={(event) => event.stopPropagation()}>
            <header className="manager-modal-head detail-modal-head">
              <div>
                <p className="eyebrow">{statusLabel(selectedNotification.type) || selectedNotification.type || "Thông báo"}</p>
                <h2 id="notification-detail-title">{selectedNotification.title || "Thông báo"}</h2>
                <span>{formatNotificationTime(selectedNotification.createdAt)}</span>
              </div>
              <button className="icon-button" type="button" onClick={() => setSelectedNotification(null)} aria-label="Đóng chi tiết thông báo">
                ×
              </button>
            </header>
            <div className="detail-modal-body">
              <p>{selectedNotification.content || "Không có nội dung chi tiết."}</p>
            </div>
          </section>
        </div>
      ) : null}
    </header>
  );
}
