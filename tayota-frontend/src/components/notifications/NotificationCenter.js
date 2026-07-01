"use client";

import { useEffect, useState } from "react";
import LinkifiedText from "@/components/notifications/LinkifiedText";
import {
  getNotifications,
  getUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from "@/lib/services/notifications";
import { statusLabel } from "@/lib/format";

function normalizeNotifications(result) {
  if (Array.isArray(result)) return result;
  return result?.items || result?.content || [];
}

function formatNotificationTime(value) {
  if (!value) return "Đang cập nhật";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function notificationTypeLabel(type) {
  return statusLabel(type) || type || "Thông báo";
}

function notificationTypeClass(type) {
  const normalized = String(type || "").toUpperCase();
  if (normalized === "APPOINTMENT") return "type-appointment";
  if (normalized === "SERVICE") return "type-service";
  return "type-default";
}

function emitNotificationRefresh() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("tayota:notifications-updated"));
  }
}

export default function NotificationCenter() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setMessage("");
    try {
      const [notificationResult, countResult] = await Promise.all([
        getNotifications(),
        getUnreadNotificationCount(),
      ]);
      setNotifications(normalizeNotifications(notificationResult));
      setUnreadCount(Number(countResult?.unreadCount || 0));
    } catch (error) {
      setMessage(error.message || "Không thể tải thông báo.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function openNotification(notification) {
    if (!notification?.id) return;
    let nextNotification = notification;
    setMessage("");
    try {
      if (!notification.read) {
        const updated = await markNotificationAsRead(notification.id);
        nextNotification = { ...notification, ...updated, read: true };
        setNotifications((items) => items.map((item) => (item.id === notification.id ? nextNotification : item)));
        setUnreadCount((current) => Math.max(0, current - 1));
        emitNotificationRefresh();
      }
      setSelectedNotification(nextNotification);
    } catch (error) {
      setMessage(error.message || "Không thể đánh dấu thông báo.");
    }
  }

  async function readAll() {
    if (saving || !notifications.length) return;
    setSaving(true);
    setMessage("");
    try {
      const result = await markAllNotificationsAsRead();
      setNotifications((items) => items.map((item) => ({ ...item, read: true })));
      setUnreadCount(Number(result?.unreadCount || 0));
      emitNotificationRefresh();
    } catch (error) {
      setMessage(error.message || "Không thể đánh dấu tất cả thông báo.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <section className="notification-page-panel">
        <div className="notification-page-head">
          <div>
            <h2>Tất cả thông báo</h2>
          </div>
          <div className="notification-page-actions">
            <span className={`notification-count-pill ${unreadCount > 0 ? "unread" : "read"}`}>{unreadCount > 0 ? `${unreadCount} chưa đọc` : "Đã đọc hết"}</span>
            <button className="btn btn-secondary" type="button" onClick={readAll} disabled={saving || !notifications.length}>
              {saving ? "Đang cập nhật..." : "Đánh dấu tất cả đã đọc"}
            </button>
          </div>
        </div>

        {message ? <div className="status-box">{message}</div> : null}
        {loading ? <div className="status-box">Đang tải thông báo...</div> : null}

        {!loading && notifications.length ? (
          <div className="notification-page-list">
            {notifications.map((item) => (
              <button className={`notification-page-item ${item.read ? "" : "unread"}`} key={item.id} type="button" onClick={() => openNotification(item)}>
                <span className="notification-page-copy">
                  <span>
                    <strong>{item.title || "Thông báo"}</strong>
                    <small>{formatNotificationTime(item.createdAt)}</small>
                  </span>
                  <em className={`notification-type-pill ${notificationTypeClass(item.type)}`}>{notificationTypeLabel(item.type)}</em>
                  <p>{item.content || "Không có nội dung chi tiết."}</p>
                </span>
                <span className={`notification-read-pill ${item.read ? "read" : "unread"}`}>{item.read ? "Đã đọc" : "Chưa đọc"}</span>
              </button>
            ))}
          </div>
        ) : null}

        {!loading && !notifications.length ? (
          <div className="notification-page-empty">
            <strong>Chưa có thông báo</strong>
            <span>Các cập nhật về lịch hẹn, dịch vụ và tài khoản sẽ xuất hiện tại đây.</span>
          </div>
        ) : null}
      </section>

      {selectedNotification ? (
        <div className="manager-modal-backdrop detail-modal-backdrop" role="presentation" onClick={() => setSelectedNotification(null)}>
          <section className="detail-modal" role="dialog" aria-modal="true" aria-labelledby="notification-page-detail-title" onClick={(event) => event.stopPropagation()}>
            <header className="manager-modal-head detail-modal-head">
              <div>
                <p className="eyebrow">{notificationTypeLabel(selectedNotification.type)}</p>
                <h2 id="notification-page-detail-title">{selectedNotification.title || "Thông báo"}</h2>
                <span>{formatNotificationTime(selectedNotification.createdAt)}</span>
              </div>
              <button className="icon-button" type="button" onClick={() => setSelectedNotification(null)} aria-label="Đóng chi tiết thông báo">
                ×
              </button>
            </header>
            <div className="detail-modal-body">
              <p><LinkifiedText text={selectedNotification.content || "Không có nội dung chi tiết."} /></p>
            </div>
          </section>
        </div>
      ) : null}
    </>
  );
}
