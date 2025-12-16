import React, { useEffect, useRef, useState } from "react";

const API_BASE = "http://localhost:8080";
const ALERTS_BASE = `${API_BASE}/api/alerts`; // ⬅ change to `${API_BASE}/alerts` if needed

type AlertType = "LOW_STOCK" | "EXPIRY" | "VENDOR_RESPONSE" | string;
type AlertSeverity = "INFO" | "WARNING" | "HIGH" | "CRITICAL" | string;

interface Alert {
  id: number;
  message: string;
  type?: AlertType;
  severity?: AlertSeverity;
  read?: boolean;
  createdAt?: string;
  productId?: number;
  productName?: string;
}

interface NotificationBellProps {
  onOpenAlertsPage?: () => void; // optional: navigate to /alerts
}

const NotificationBell: React.FC<NotificationBellProps> = ({ onOpenAlertsPage }) => {
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [recentAlerts, setRecentAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dropdownRef = useRef<HTMLDivElement | null>(null);

  const token = localStorage.getItem("token");

  const fetchUnreadCount = async () => {
    if (!token) return;
    try {
      const res = await fetch(`${ALERTS_BASE}/unread/count`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!res.ok) {
        console.error("Failed to fetch unread count", res.status);
        return;
      }
      const data = await res.json();
      // backend might return: { count: 3 } or just a number; handle both:
      const count = typeof data === "number" ? data : data.count ?? 0;
      setUnreadCount(count);
    } catch (err) {
      console.error("Unread count error", err);
    }
  };

  const fetchRecentAlerts = async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`${ALERTS_BASE}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!res.ok) {
        throw new Error(`Failed to fetch alerts: ${res.status}`);
      }
      const data = await res.json();
      const alerts: Alert[] = Array.isArray(data) ? data : data.data ?? data.content ?? [];
      // sort by createdAt desc (if available) and take top 5
      const sorted = [...alerts].sort((a, b) => {
        const da = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const db = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return db - da;
      });
      setRecentAlerts(sorted.slice(0, 5));
    } catch (err: any) {
      console.error(err);
      setError(err.message || "Failed to load alerts");
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = async (alertId: number) => {
    if (!token) return;
    try {
      const res = await fetch(`${ALERTS_BASE}/${alertId}/read`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!res.ok) {
        console.error("Failed to mark alert read", await res.text());
        return;
      }
      // update UI
      setRecentAlerts((prev) =>
        prev.map((a) => (a.id === alertId ? { ...a, read: true } : a))
      );
      fetchUnreadCount();
    } catch (err) {
      console.error("markAsRead error", err);
    }
  };

  // click outside to close dropdown
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  // initial + polling
  useEffect(() => {
    fetchUnreadCount();
    fetchRecentAlerts();

    const interval = setInterval(() => {
      fetchUnreadCount();
      fetchRecentAlerts();
    }, 30000); // 30s

    return () => clearInterval(interval);
  }, []);

  const badgeVisible = unreadCount > 0;

  const iconForType = (alert: Alert) => {
    const type = alert.type || "";
    const severity = alert.severity || "";
    if (type === "LOW_STOCK") return "📉";
    if (type === "EXPIRY") return "⏰";
    if (type === "VENDOR_RESPONSE") return "📬";
    if (severity === "CRITICAL") return "🚨";
    if (severity === "HIGH") return "⚠️";
    return "🔔";
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        className="relative inline-flex items-center justify-center rounded-full p-2 bg-slate-800/80 hover:bg-slate-700 border border-slate-600"
        onClick={() => setOpen((prev) => !prev)}
        title="Notifications"
      >
        {/* Bell Icon */}
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5 text-gray-100"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 0 0-5-5.9V4a1 1 0 1 0-2 0v1.1A6 6 0 0 0 6 11v3.2c0 .5-.2 1-.6 1.4L4 17h5m6 0a3 3 0 1 1-6 0m6 0H9"
          />
        </svg>

        {/* Badge */}
        {badgeVisible && (
          <span className="absolute -top-1 -right-1 inline-flex items-center justify-center text-[10px] min-w-[16px] h-[16px] rounded-full bg-red-600 text-white font-semibold">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown */}
      {open && (
        <div className="absolute right-0 mt-2 w-80 bg-slate-900 border border-slate-700 rounded-xl shadow-xl z-50">
          <div className="px-3 py-2 border-b border-slate-800 flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-200">
              Alerts & Notifications
            </span>
            {badgeVisible && (
              <span className="text-[10px] text-red-300">
                {unreadCount} unread
              </span>
            )}
          </div>

          <div className="max-h-72 overflow-y-auto">
            {loading && (
              <div className="px-3 py-3 text-xs text-gray-400">
                Loading alerts…
              </div>
            )}
            {error && !loading && (
              <div className="px-3 py-3 text-xs text-red-400">
                {error}
              </div>
            )}
            {!loading && !error && recentAlerts.length === 0 && (
              <div className="px-3 py-3 text-xs text-gray-500">
                No alerts yet. System is all good ✨
              </div>
            )}

            {!loading &&
              !error &&
              recentAlerts.map((alert) => (
                <div
                  key={alert.id}
                  className={`px-3 py-2 text-xs flex gap-2 border-b border-slate-800/70 ${
                    alert.read ? "bg-slate-900" : "bg-slate-800/70"
                  }`}
                >
                  <div className="pt-[2px] text-lg">
                    {iconForType(alert)}
                  </div>
                  <div className="flex-1">
                    <div className="text-gray-100">
                      {alert.message}
                    </div>
                    <div className="text-[10px] text-gray-500 mt-0.5">
                      {alert.productName && (
                        <span className="mr-2">
                          Product: {alert.productName}
                        </span>
                      )}
                      {alert.createdAt && (
                        <span>
                          {new Date(alert.createdAt).toLocaleString()}
                        </span>
                      )}
                    </div>
                  </div>
                  {!alert.read && (
                    <button
                      onClick={() => markAsRead(alert.id)}
                      className="text-[10px] text-sky-300 hover:text-sky-200"
                    >
                      Mark<br />read
                    </button>
                  )}
                </div>
              ))}
          </div>

          <div className="px-3 py-2 border-t border-slate-800 flex justify-between items-center">
            <button
              className="text-[11px] text-gray-400 hover:text-gray-200"
              onClick={() => {
                setOpen(false);
                if (onOpenAlertsPage) onOpenAlertsPage();
              }}
            >
              View all alerts
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;
