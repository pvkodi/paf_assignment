import React, { useCallback, useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";
import { useNotificationContext } from "../../contexts/NotificationContext";

function formatTimestamp(value) {
  if (!value) {
    return "-";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "-";
  }

  return parsed.toLocaleString();
}

function severityClass(severity) {
  if (severity === "HIGH") {
    return "bg-[#fef2f2] text-[#991b1b] border-[#fca5a5]";
  }
  return "bg-[#f0fdf4] text-[#166534] border-[#86efac]";
}

export function NotificationCenter() {
  const { updateUnreadCount } = useNotificationContext();
  const [notifications, setNotifications] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState(null);

  const fetchUnreadCount = useCallback(async () => {
    const response = await apiClient.get("/v1/notifications/unread/count");
    const payload = response?.data || {};
    const value = payload.unreadCount ?? payload.unread_count ?? 0;
    const count = Number(value) || 0;
    setUnreadCount(count);
    updateUnreadCount(count);
  }, [updateUnreadCount]);

  const fetchNotifications = useCallback(async (targetPage = 0) => {
    try {
      setLoading(true);
      setError(null);

      const response = await apiClient.get("/v1/notifications", {
        params: {
          page: targetPage,
          size: 10,
        },
      });

      const payload = response?.data;
      const content = Array.isArray(payload?.content)
        ? payload.content
        : Array.isArray(payload)
          ? payload
          : [];

      setNotifications(content);
      setPage(Number(payload?.number ?? targetPage) || 0);
      setTotalPages(Math.max(1, Number(payload?.totalPages ?? 1) || 1));
      await fetchUnreadCount();
    } catch (requestError) {
      console.error("Failed to load notifications", requestError);
      setError("Failed to load notifications.");
    } finally {
      setLoading(false);
    }
  }, [fetchUnreadCount]);

  useEffect(() => {
    fetchNotifications(0);
  }, [fetchNotifications]);

  const markOneAsRead = async (id) => {
    try {
      setWorking(true);
      await apiClient.post(`/v1/notifications/${id}/read`);
      await fetchNotifications(page);
    } catch (requestError) {
      console.error("Failed to mark notification as read", requestError);
      setError("Could not mark notification as read.");
    } finally {
      setWorking(false);
    }
  };

  const markAllAsRead = async () => {
    try {
      setWorking(true);
      await apiClient.delete("/v1/notifications");
      await fetchNotifications(page);
    } catch (requestError) {
      console.error("Failed to mark all notifications as read", requestError);
      setError("Could not mark all notifications as read.");
    } finally {
      setWorking(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <div className="w-10 h-10 rounded-full border-4 border-indigo-200 border-t-indigo-600 animate-spin mb-4" />
        <p className="text-sm font-medium text-[#64748b]">Loading notifications...</p>
      </div>
    );
  }

  return (
    <section className="space-y-6">
      <header className="rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-sm flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-[#0f172a] tracking-tight">Notification Center</h2>
          <p className="mt-1 text-sm font-medium text-[#64748b]">
            Unread alerts: <span className="font-bold text-[#0f172a]">{unreadCount}</span>
          </p>
        </div>
        <button
          type="button"
          disabled={working || unreadCount === 0}
          onClick={markAllAsRead}
          className="rounded-xl border border-[#e2e8f0] px-4 py-2 text-sm font-bold text-[#475569] transition hover:bg-[#f8fafc] disabled:cursor-not-allowed disabled:opacity-50 shadow-sm flex items-center gap-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
          Mark all as read
        </button>
      </header>

      {error && (
        <div className="rounded-xl border border-[#fca5a5] bg-[#fef2f2] p-4 text-sm font-semibold text-[#991b1b] flex items-center gap-3 shadow-sm">
          <svg className="w-5 h-5 text-[#ef4444]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
          {error}
        </div>
      )}

      <div className="space-y-4">
        {notifications.length === 0 ? (
          <div className="rounded-2xl border border-[#e2e8f0] bg-[#f8fafc] p-10 text-center shadow-sm">
            <div className="w-16 h-16 bg-white border border-[#e2e8f0] rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"></path></svg>
            </div>
            <p className="text-lg font-bold text-[#0f172a]">All caught up!</p>
            <p className="text-sm text-[#64748b] mt-1">You have no new notifications.</p>
          </div>
        ) : (
          notifications.map((notification) => (
            <article
              key={notification.id}
              className={`rounded-xl border p-5 transition-all ${
                notification.read 
                  ? "border-[#e2e8f0] bg-[#f8fafc]" 
                  : "border-[#6366f1] bg-white shadow-sm ring-1 ring-inset ring-[#6366f1]/10"
              }`}
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex-1 pr-4">
                  <div className="flex items-center gap-2 mb-1">
                    {!notification.read && <span className="w-2 h-2 rounded-full bg-[#6366f1] shrink-0 animate-pulse"></span>}
                    <h3 className={`text-sm font-bold ${notification.read ? "text-[#475569]" : "text-[#0f172a]"}`}>
                      {notification.title}
                    </h3>
                  </div>
                  <p className="text-sm font-medium text-[#475569] leading-relaxed ml-[1.125rem]">
                    {notification.message}
                  </p>
                </div>
                <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider border ${severityClass(notification.severity)}`}>
                  {notification.severity || "STANDARD"}
                </span>
              </div>

              <div className="mt-4 ml-[1.125rem] flex flex-wrap items-center justify-between gap-3 border-t border-[#e2e8f0] pt-3">
                <p className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider flex items-center gap-1.5">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                  {formatTimestamp(notification.createdAt)}
                </p>
                {!notification.read && (
                  <button
                    type="button"
                    disabled={working}
                    onClick={() => markOneAsRead(notification.id)}
                    className="rounded-lg bg-[#0f172a] px-3 py-1.5 text-xs font-bold text-white transition hover:bg-[#1e293b] disabled:cursor-not-allowed disabled:opacity-50 shadow-sm"
                  >
                    Mark as read
                  </button>
                )}
              </div>
            </article>
          ))
        )}
      </div>

      <div className="flex items-center justify-between rounded-2xl border border-[#e2e8f0] bg-white p-5 shadow-sm">
        <button
          type="button"
          disabled={page <= 0 || loading}
          onClick={() => fetchNotifications(page - 1)}
          className="rounded-xl border border-[#e2e8f0] px-4 py-2 text-sm font-bold text-[#475569] transition hover:bg-[#f8fafc] disabled:cursor-not-allowed disabled:opacity-50"
        >
          Previous
        </button>
        <span className="text-[10px] font-bold text-[#94a3b8] uppercase tracking-wider">
          Page {page + 1} of {totalPages}
        </span>
        <button
          type="button"
          disabled={page >= totalPages - 1 || loading}
          onClick={() => fetchNotifications(page + 1)}
          className="rounded-xl border border-[#e2e8f0] px-4 py-2 text-sm font-bold text-[#475569] transition hover:bg-[#f8fafc] disabled:cursor-not-allowed disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </section>
  );
}

export default NotificationCenter;
