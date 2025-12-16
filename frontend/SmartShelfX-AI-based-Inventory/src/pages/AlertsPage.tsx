import React, { useEffect, useMemo, useState } from "react";

const API_BASE = "http://localhost:8080";
const ALERTS_BASE = `${API_BASE}/api/alerts`; // adjust if needed

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

type SortField = "createdAt" | "severity" | "type" | "read";

const AlertsPage: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>("createdAt");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");

  const [toast, setToast] = useState<{ type: "success" | "error"; message: string } | null>(
    null
  );

  const token = localStorage.getItem("token");

  const showToast = (type: "success" | "error", message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 3000);
  };

  const fetchAlerts = async () => {
    if (!token) {
      setError("No auth token - please log in.");
      setLoading(false);
      return;
    }

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
      const alertsArr: Alert[] = Array.isArray(data) ? data : data.data ?? data.content ?? [];
      setAlerts(alertsArr);
    } catch (err: any) {
      console.error(err);
      setError(err.message || "Error loading alerts");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const toggleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDir("asc");
    }
  };

  const sortedAlerts = useMemo(() => {
    const sorted = [...alerts];
    sorted.sort((a, b) => {
      let av: any = a[sortField];
      let bv: any = b[sortField];

      if (sortField === "createdAt") {
        av = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        bv = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      }

      if (av == null) av = "";
      if (bv == null) bv = "";

      if (av < bv) return sortDir === "asc" ? -1 : 1;
      if (av > bv) return sortDir === "asc" ? 1 : -1;
      return 0;
    });
    return sorted;
  }, [alerts, sortField, sortDir]);

  const markRead = async (id: number) => {
    if (!token) return;
    try {
      const res = await fetch(`${ALERTS_BASE}/${id}/read`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!res.ok) {
        throw new Error(`Failed to mark alert read: ${res.status}`);
      }
      setAlerts((prev) => prev.map((a) => (a.id === id ? { ...a, read: true } : a)));
      showToast("success", "Alert marked as read");
    } catch (err: any) {
      console.error(err);
      showToast("error", err.message || "Failed to update alert");
    }
  };

  const markAllRead = async () => {
    if (!token) return;
    try {
      const res = await fetch(`${ALERTS_BASE}/read-all`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!res.ok) {
        throw new Error(`Failed to mark all read: ${res.status}`);
      }
      setAlerts((prev) => prev.map((a) => ({ ...a, read: true })));
      showToast("success", "All alerts marked as read");
    } catch (err: any) {
      console.error(err);
      showToast("error", err.message || "Failed to update alerts");
    }
  };

  const severityBadge = (sev?: AlertSeverity) => {
    if (!sev) return null;
    const base = "px-2 py-1 rounded-full text-[10px] font-medium";
    if (sev === "CRITICAL")
      return <span className={`${base} bg-red-900/80 text-red-200`}>CRITICAL</span>;
    if (sev === "HIGH")
      return <span className={`${base} bg-orange-900/80 text-orange-200`}>HIGH</span>;
    if (sev === "WARNING")
      return <span className={`${base} bg-amber-900/80 text-amber-100`}>WARNING</span>;
    return <span className={`${base} bg-slate-700 text-slate-100`}>{sev}</span>;
  };

  const typeLabel = (type?: AlertType) => {
    if (!type) return "-";
    if (type === "LOW_STOCK") return "Low stock";
    if (type === "EXPIRY") return "Expiry";
    if (type === "VENDOR_RESPONSE") return "Vendor response";
    return type;
  };

  function openVendorModal(_alert: Alert): void {
    throw new Error("Function not implemented.");
  }

  return (
    <div className="p-6 text-gray-100 space-y-4">
      <div className="flex items-center justify-between mb-2">
        <div>
          <h1 className="text-2xl font-semibold">Alerts & Notifications</h1>
          <p className="text-xs text-gray-400">
            Low stock alerts, expiry alerts, and vendor response tracking in one place.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={fetchAlerts}
            className="px-3 py-1 rounded-md bg-slate-800 hover:bg-slate-700 text-xs border border-slate-600"
          >
            Refresh
          </button>
          <button
            onClick={markAllRead}
            className="px-3 py-1 rounded-md bg-emerald-600 hover:bg-emerald-500 text-xs"
          >
            Mark all as read
          </button>
        </div>
      </div>

      {loading && (
        <div className="text-sm text-gray-400">Loading alerts…</div>
      )}

      {error && (
        <div className="text-sm text-red-400">Error: {error}</div>
      )}

      {!loading && !error && (
        <div className="bg-slate-900 border border-slate-700 rounded-xl overflow-hidden">
          <table className="min-w-full text-xs">
            <thead className="bg-slate-800/80 border-b border-slate-700">
              <tr>
                <th className="px-3 py-2 text-left">Message</th>
                <th
                  className="px-3 py-2 text-left cursor-pointer select-none"
                  onClick={() => toggleSort("type")}
                >
                  Type {sortField === "type" && (sortDir === "asc" ? "↑" : "↓")}
                </th>
                <th
                  className="px-3 py-2 text-center cursor-pointer select-none"
                  onClick={() => toggleSort("severity")}
                >
                  Severity {sortField === "severity" && (sortDir === "asc" ? "↑" : "↓")}
                </th>
                <th className="px-3 py-2 text-left">Product</th>
                <th
                  className="px-3 py-2 text-center cursor-pointer select-none"
                  onClick={() => toggleSort("createdAt")}
                >
                  Date {sortField === "createdAt" && (sortDir === "asc" ? "↑" : "↓")}
                </th>
                <th
                  className="px-3 py-2 text-center cursor-pointer select-none"
                  onClick={() => toggleSort("read")}
                >
                  Status {sortField === "read" && (sortDir === "asc" ? "↑" : "↓")}
                </th>
                <th className="px-3 py-2 text-center">Action</th>
              </tr>
            </thead>
            <tbody>
              {sortedAlerts.length === 0 && (
                <tr>
                  <td
                    colSpan={7}
                    className="px-3 py-4 text-center text-gray-500"
                  >
                    No alerts found.
                  </td>
                </tr>
              )}
              {sortedAlerts.map((alert) => (
                <tr
                  key={alert.id}
                  className={`border-b border-slate-800 ${alert.read ? "bg-slate-900" : "bg-slate-800/60"
                    }`}
                >
                  <td className="px-3 py-2 text-xs max-w-xs">
                    {alert.message}
                  </td>
                  <td className="px-3 py-2 text-xs">
                    {typeLabel(alert.type)}
                  </td>
                  <td className="px-3 py-2 text-center">
                    {severityBadge(alert.severity)}
                  </td>
                  <td className="px-3 py-2 text-xs">
                    {alert.productName
                      ? `${alert.productName} ${alert.productId ? `(#${alert.productId})` : ""}`
                      : alert.productId
                        ? `Product #${alert.productId}`
                        : "-"}
                  </td>
                  <td className="px-3 py-2 text-center text-[11px] text-gray-400">
                    {alert.createdAt
                      ? new Date(alert.createdAt).toLocaleString()
                      : "-"}
                  </td>
                  <td className="px-3 py-2 text-center text-[11px]">
                    {alert.read ? (
                      <span className="text-gray-400">Read</span>
                    ) : (
                      <span className="text-emerald-300">Unread</span>
                    )}
                  </td>
                  <td className="px-3 py-2 text-center">

                    {/* Vendor Response Button */}
                    {alert.type === "VENDOR_RESPONSE" && (
                      <button
                        onClick={() => openVendorModal(alert)}
                        className="btn btn-sm btn-primary me-2"
                        style={{ fontSize: "11px" }}
                      >
                        View Response
                      </button>
                    )}

                    {/* Mark Read */}
                    {!alert.read && (
                      <button
                        onClick={() => markRead(alert.id)}
                        className="btn btn-sm btn-secondary"
                        style={{ fontSize: "11px" }}
                      >
                        Mark Read
                      </button>
                    )}
                  </td>

                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-4 right-4 z-50">
          <div
            className={`px-4 py-2 rounded-md text-sm shadow-lg ${toast.type === "success"
                ? "bg-emerald-600 text-emerald-50"
                : "bg-red-600 text-red-50"
              }`}
          >
            {toast.message}
          </div>
        </div>
      )}
    </div>
  );
};

export default AlertsPage;
