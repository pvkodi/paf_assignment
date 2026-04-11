import React, { useEffect, useState } from "react";
import { apiClient } from "../../services/apiClient";

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
    return "bg-red-100 text-red-700";
  }
  return "bg-blue-100 text-blue-700";
}

export function NotificationCenter() {
  const [notifications, setNotifications] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState(null);

  const fetchUnreadCount = async () => {
    const response = await apiClient.get("/v1/notifications/unread/count");
    const payload = response?.data || {};
    const value = payload.unreadCount ?? payload.unread_count ?? 0;
    setUnreadCount(Number(value) || 0);
  };

  const fetchNotifications = async (targetPage = 0) => {
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
  };

  useEffect(() => {
    fetchNotifications(0);
  }, []);

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
      <div className="flex items-center justify-center py-10">
        <div className="h-10 w-10 animate-spin rounded-full border-b-2 border-indigo-600" />
      </div>
    );
  }

  return (
    <section className="space-y-5">
      <header className="rounded-xl border border-slate-200 bg-white p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-slate-900">Notification Center</h2>
            <p className="mt-1 text-sm text-slate-600">
              Unread notifications: {unreadCount}
            </p>
          </div>
          <button
            type="button"
            disabled={working || unreadCount === 0}
            onClick={markAllAsRead}
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Mark all as read
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="space-y-3">
        {notifications.length === 0 ? (
          <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-600">
            No notifications available.
          </div>
        ) : (
          notifications.map((notification) => (
            <article
              key={notification.id}
              className={`rounded-xl border p-4 ${
                notification.read ? "border-slate-200 bg-white" : "border-indigo-200 bg-indigo-50"
              }`}
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 className="text-sm font-semibold text-slate-900">{notification.title}</h3>
                  <p className="mt-1 text-sm text-slate-700">{notification.message}</p>
                </div>
                <span className={`rounded-full px-2 py-1 text-xs font-medium ${severityClass(notification.severity)}`}>
                  {notification.severity || "STANDARD"}
                </span>
              </div>

              <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
                <p>{formatTimestamp(notification.createdAt)}</p>
                {!notification.read && (
                  <button
                    type="button"
                    disabled={working}
                    onClick={() => markOneAsRead(notification.id)}
                    className="rounded-md bg-indigo-600 px-2 py-1 font-medium text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Mark as read
                  </button>
                )}
              </div>
            </article>
          ))
        )}
      </div>

      <div className="flex items-center justify-between rounded-xl border border-slate-200 bg-white p-4 text-sm">
        <button
          type="button"
          disabled={page <= 0 || loading}
          onClick={() => fetchNotifications(page - 1)}
          className="rounded-md border border-slate-300 px-3 py-1 text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Previous
        </button>
        <span className="text-slate-600">
          Page {page + 1} of {totalPages}
        </span>
        <button
          type="button"
          disabled={page >= totalPages - 1 || loading}
          onClick={() => fetchNotifications(page + 1)}
          className="rounded-md border border-slate-300 px-3 py-1 text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
        >
          Next
        </button>
      </div>
    </section>
  );
}

export default NotificationCenter;
