// src/pages/AnalyticsReportsPage.tsx
import React, { useEffect, useMemo, useState } from "react";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
} from "chart.js";
import { Bar, Pie, Line } from "react-chartjs-2";

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  PointElement,
  LineElement,
  Tooltip,
  Legend
);

const API_BASE = "http://localhost:8080/api";
const token = localStorage.getItem("token");

interface InventorySummary {
  totalProducts: number;
  lowStockProducts: number;
  outOfStockProducts: number;
  totalInventoryValue: number;
}

interface StockAlertsSummary {
  totalAlerts: number;
  lowStockAlerts: number;
  predictedStockoutAlerts: number;
  criticalAlerts: Alert[];
}

interface ProductLite {
  id: number;
  name: string;
  sku: string;
  category: string;
}

interface Alert {
  id: number;
  product: ProductLite;
  type: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | string;
  message: string;
  title: string;
  createdAt: string;
  suggestedAction?: string;
  predictedShortfall?: number;
  source?: string;
}

interface UserLite {
  id: number;
  firstName: string;
  lastName: string;
  role: string;
  email: string;
}

type TransactionType = "IN" | "OUT" | string;

interface InventoryTransaction {
  id: number;
  product: ProductLite;
  quantity: number;
  type: TransactionType;
  timestamp: string;
  handledBy: UserLite;
  notes?: string;
  referenceNumber?: string | null;
}

interface PurchaseOrderLite {
  id: number;
  poNumber: string;
  product: ProductLite;
  vendor: UserLite | null;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  status: string;
  createdAt: string;
  expectedDelivery?: string | null;
}

interface RecentActivity {
  recentStockIns: number;
  recentStockOuts: number;
  recentTransactions: InventoryTransaction[];
  recentPurchaseOrders: PurchaseOrderLite[];
}

interface PurchaseOrderSummary {
  totalPOs: number;
  pendingPOs: number;
  approvedPOs: number;
  upcomingDeliveries: PurchaseOrderLite[];
}

interface DashboardData {
  inventorySummary: InventorySummary;
  stockAlerts: StockAlertsSummary;
  recentActivity: RecentActivity;
  purchaseOrderSummary: PurchaseOrderSummary;
}

interface DashboardResponse {
  status: string;
  message: string;
  data: DashboardData;
}

interface SalesMetrics {
  totalSalesValue: number;
  salesGrowth: number;
  transactionsCount: number;
}

interface SalesMetricsResponse {
  status: string;
  message: string;
  data: SalesMetrics;
}

interface ToastState {
  type: "success" | "error";
  message: string;
}

// Helper: JSON-safe parsing
const getJsonSafe = async <T,>(res: Response): Promise<T> => {
  const contentType = res.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    const text = await res.text();
    throw new Error(`Expected JSON but got: ${text.substring(0, 200)}`);
  }
  return (await res.json()) as T;
};

const AnalyticsReportsPage: React.FC = () => {
  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [salesMetrics, setSalesMetrics] = useState<SalesMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState<ToastState | null>(null);

  const showToast = (type: ToastState["type"], message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  };

  // --------- Fetch dashboard + sales metrics ---------
  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const headers: HeadersInit = {};
        if (token) {
          headers["Authorization"] = `Bearer ${token}`;
        }

        const [dashRes, salesRes] = await Promise.all([
          fetch(`${API_BASE}/analytics/dashboard`, { headers }),
          fetch(`${API_BASE}/analytics/sales-metrics`, { headers }),
        ]);

        if (!dashRes.ok) {
          throw new Error(`Failed to load dashboard: ${dashRes.status}`);
        }
        const dashJson = await getJsonSafe<DashboardResponse>(dashRes);
        setDashboard(dashJson.data);

        if (salesRes.ok) {
          const salesJson = await getJsonSafe<SalesMetricsResponse>(salesRes);
          setSalesMetrics(salesJson.data);
        } else {
          setSalesMetrics(null);
        }
      } catch (err: any) {
        console.error(err);
        setError(err.message || "Failed to load analytics");
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  // --------- Derived metrics ---------

  const inventorySummary = dashboard?.inventorySummary;
  const stockAlerts = dashboard?.stockAlerts;
  const recentActivity = dashboard?.recentActivity;
  const poSummary = dashboard?.purchaseOrderSummary;

  const healthyProductsCount =
    inventorySummary && inventorySummary.totalProducts != null
      ? Math.max(
          inventorySummary.totalProducts -
            inventorySummary.lowStockProducts -
            inventorySummary.outOfStockProducts,
          0
        )
      : 0;

  const criticalCount =
    stockAlerts?.criticalAlerts.filter((a) => a.priority === "CRITICAL").length || 0;
  const highCount =
    stockAlerts?.criticalAlerts.filter((a) => a.priority === "HIGH").length || 0;
  const mediumCount =
    stockAlerts?.criticalAlerts.filter((a) => a.priority === "MEDIUM").length || 0;
  const lowCount =
    stockAlerts?.criticalAlerts.filter((a) => a.priority === "LOW").length || 0;

  // recent transactions for charts
  const recentTransactions = recentActivity?.recentTransactions || [];

  // --------- Chart Data ---------

  // 1) Inventory health pie chart
  const inventoryPieData = useMemo(() => {
    if (!inventorySummary) return null;

    return {
      labels: ["Healthy", "Low Stock", "Out of Stock"],
      datasets: [
        {
          label: "Products",
          data: [
            healthyProductsCount,
            inventorySummary.lowStockProducts,
            inventorySummary.outOfStockProducts,
          ],
          borderWidth: 1,
        },
      ],
    };
  }, [inventorySummary, healthyProductsCount]);

  // 2) Alerts by priority bar chart
  const alertsPriorityData = useMemo(() => {
    if (!stockAlerts) return null;

    return {
      labels: ["Critical", "High", "Medium", "Low"],
      datasets: [
        {
          label: "Active Alerts",
          data: [criticalCount, highCount, mediumCount, lowCount],
          borderWidth: 1,
        },
      ],
    };
  }, [stockAlerts, criticalCount, highCount, mediumCount, lowCount]);

  // 3) Inventory trend line chart (recent IN vs OUT)
  const inventoryTrendData = useMemo(() => {
    if (!recentTransactions.length) return null;

    // Sort oldest → newest
    const sorted = [...recentTransactions].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );

    const labels = sorted.map((t) =>
      new Date(t.timestamp).toLocaleDateString(undefined, {
        day: "2-digit",
        month: "short",
      })
    );

    const values = sorted.map((t) =>
      t.type === "IN" ? t.quantity : -Math.abs(t.quantity)
    );

    return {
      labels,
      datasets: [
        {
          label: "Net Movement (IN positive, OUT negative)",
          data: values,
          borderWidth: 2,
          tension: 0.3,
        },
      ],
    };
  }, [recentTransactions]);

  // 4) Top restocked items (based on IN transactions)
  const topRestockedData = useMemo(() => {
    const ins = recentTransactions.filter((t) => t.type === "IN");
    if (!ins.length) return null;

    const totals = new Map<string, number>();
    ins.forEach((t) => {
      const key = `${t.product.sku} – ${t.product.name}`;
      totals.set(key, (totals.get(key) || 0) + t.quantity);
    });

    const sorted = Array.from(totals.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5);

    return {
      labels: sorted.map(([label]) => label),
      datasets: [
        {
          label: "Restocked Quantity",
          data: sorted.map(([, qty]) => qty),
          borderWidth: 1,
        },
      ],
    };
  }, [recentTransactions]);

  // --------- CSV Export handlers ---------

  const downloadFile = async (path: string, filename: string) => {
    try {
      const headers: HeadersInit = {};
      if (token) headers["Authorization"] = `Bearer ${token}`;

      const res = await fetch(`${API_BASE}${path}`, { headers });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(
          `Failed to download (${res.status}): ${text.substring(0, 120)}`
        );
      }

      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);

      showToast("success", `Download started: ${filename}`);
    } catch (err: any) {
      console.error(err);
      showToast("error", err.message || "Download failed");
    }
  };

  // --------- Render states ---------

  if (loading) {
    return (
      <div className="p-6 text-gray-100">
        <h1 className="text-2xl font-semibold mb-4">Analytics & Reports</h1>
        <p>Loading dashboard analytics…</p>
      </div>
    );
  }

  if (error || !dashboard) {
    return (
      <div className="p-6 text-red-400">
        <h1 className="text-2xl font-semibold mb-4">Analytics & Reports</h1>
        <p className="mb-2">Error: {error || "Failed to load dashboard data"}</p>
        <p className="text-sm text-gray-400">
          Ensure backend is running at <code>{API_BASE}</code> and you are logged in
          with a valid token.
        </p>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6 text-gray-100">
      {/* Header + Export buttons */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Analytics & Reports</h1>
          <p className="text-sm text-gray-400">
            High-level view of inventory health, alerts, stock movement and purchase
            orders, with exportable reports.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => downloadFile("/data/export/products", "products.csv")}
            className="px-3 py-2 rounded-md bg-slate-800 hover:bg-slate-700 text-xs font-medium border border-slate-600"
          >
            Export Products CSV
          </button>
          <button
            onClick={() =>
              downloadFile("/data/export/low-stock-report", "low-stock-report.csv")
            }
            className="px-3 py-2 rounded-md bg-amber-800 hover:bg-amber-700 text-xs font-medium border border-amber-600"
          >
            Export Low-Stock CSV
          </button>
          <button
            onClick={() =>
              downloadFile("/data/export/template", "product-import-template.csv")
            }
            className="px-3 py-2 rounded-md bg-sky-800 hover:bg-sky-700 text-xs font-medium border border-sky-600"
          >
            Download Import Template
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* Inventory */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700">
          <div className="text-xs uppercase text-gray-400">Total Products</div>
          <div className="text-2xl font-semibold mt-1">
            {inventorySummary?.totalProducts ?? 0}
          </div>
          <div className="text-[11px] text-gray-500 mt-1">
            Inventory value:{" "}
            <span className="font-mono">
              ₹{inventorySummary?.totalInventoryValue.toFixed(2)}
            </span>
          </div>
        </div>

        <div className="bg-slate-900 rounded-xl p-4 border border-amber-700/70">
          <div className="text-xs uppercase text-gray-400">Low Stock</div>
          <div className="text-2xl font-semibold mt-1 text-amber-300">
            {inventorySummary?.lowStockProducts ?? 0}
          </div>
          <div className="text-[11px] text-gray-500 mt-1">Needs review</div>
        </div>

        <div className="bg-slate-900 rounded-xl p-4 border border-red-700/70">
          <div className="text-xs uppercase text-gray-400">Out of Stock</div>
          <div className="text-2xl font-semibold mt-1 text-red-400">
            {inventorySummary?.outOfStockProducts ?? 0}
          </div>
          <div className="text-[11px] text-gray-500 mt-1">Critical risk</div>
        </div>

        <div className="bg-slate-900 rounded-xl p-4 border border-emerald-700/70">
          <div className="text-xs uppercase text-gray-400">Sales (Tracked)</div>
          <div className="text-2xl font-semibold mt-1 text-emerald-300">
            ₹{salesMetrics?.totalSalesValue.toFixed(2) ?? "0.00"}
          </div>
          <div className="text-[11px] text-gray-500 mt-1">
            Transactions: {salesMetrics?.transactionsCount ?? 0} • Growth:{" "}
            {salesMetrics ? `${salesMetrics.salesGrowth.toFixed(1)}%` : "N/A"}
          </div>
        </div>
      </div>

      {/* Charts row: Inventory health + Alerts + Inventory trend */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
        {/* Inventory health pie */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700">
          <h2 className="font-semibold text-lg mb-2">Inventory Health</h2>
          {inventoryPieData ? (
            <Pie
              data={inventoryPieData}
              options={{
                plugins: {
                  legend: { position: "bottom" as const },
                },
              }}
            />
          ) : (
            <p className="text-sm text-gray-500">No inventory data available.</p>
          )}
        </div>

        {/* Alerts priority bar */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700">
          <h2 className="font-semibold text-lg mb-2">Alerts by Priority</h2>
          {alertsPriorityData ? (
            <Bar
              data={alertsPriorityData}
              options={{
                responsive: true,
                scales: {
                  x: { grid: { display: false } },
                  y: { beginAtZero: true },
                },
                plugins: {
                  legend: { display: false },
                },
              }}
            />
          ) : (
            <p className="text-sm text-gray-500">No alert analytics yet.</p>
          )}

          <div className="mt-3 text-[11px] text-gray-500">
            Total alerts: {stockAlerts?.totalAlerts ?? 0} • Predicted stockouts:{" "}
            {stockAlerts?.predictedStockoutAlerts ?? 0}
          </div>
        </div>

        {/* Inventory trend */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700">
          <h2 className="font-semibold text-lg mb-2">Recent Inventory Movement</h2>
          {inventoryTrendData ? (
            <Line
              data={inventoryTrendData}
              options={{
                responsive: true,
                plugins: {
                  legend: { display: false },
                  tooltip: { enabled: true },
                },
                scales: {
                  x: { grid: { display: false } },
                  y: { beginAtZero: true },
                },
              }}
            />
          ) : (
            <p className="text-sm text-gray-500">
              No recent transactions to plot. Perform stock-in/stock-out to see trend.
            </p>
          )}
          <p className="mt-2 text-[11px] text-gray-500">
            Positive values = stock-in, negative values = stock-out.
          </p>
        </div>
      </div>

      {/* Top restocked + Purchase order summary */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Top restocked items */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700">
          <h2 className="font-semibold text-lg mb-2">Top Restocked Items</h2>
          {topRestockedData ? (
            <Bar
              data={topRestockedData}
              options={{
                responsive: true,
                indexAxis: "y" as const,
                scales: {
                  x: { beginAtZero: true },
                  y: { grid: { display: false } },
                },
                plugins: {
                  legend: { display: false },
                },
              }}
            />
          ) : (
            <p className="text-sm text-gray-500">
              No recent restocks detected in transaction history.
            </p>
          )}
        </div>

        {/* Purchase order summary & upcoming deliveries */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700 flex flex-col">
          <h2 className="font-semibold text-lg mb-3">Purchase Orders Summary</h2>

          <div className="grid grid-cols-3 gap-3 text-center mb-3">
            <div className="bg-slate-800 rounded-lg p-3 border border-slate-700">
              <div className="text-[11px] uppercase text-gray-400">Total POs</div>
              <div className="text-xl font-semibold mt-1">
                {poSummary?.totalPOs ?? 0}
              </div>
            </div>
            <div className="bg-slate-800 rounded-lg p-3 border border-amber-700/70">
              <div className="text-[11px] uppercase text-gray-400">Pending</div>
              <div className="text-xl font-semibold mt-1 text-amber-300">
                {poSummary?.pendingPOs ?? 0}
              </div>
            </div>
            <div className="bg-slate-800 rounded-lg p-3 border border-emerald-700/70">
              <div className="text-[11px] uppercase text-gray-400">Approved</div>
              <div className="text-xl font-semibold mt-1 text-emerald-300">
                {poSummary?.approvedPOs ?? 0}
              </div>
            </div>
          </div>

          <h3 className="text-sm font-semibold mb-2">Upcoming Deliveries</h3>
          <div className="overflow-x-auto flex-1">
            <table className="min-w-full text-xs">
              <thead>
                <tr className="border-b border-slate-700 bg-slate-800/60">
                  <th className="px-2 py-2 text-left">PO Number</th>
                  <th className="px-2 py-2 text-left">Product</th>
                  <th className="px-2 py-2 text-center">Qty</th>
                  <th className="px-2 py-2 text-center">ETA</th>
                </tr>
              </thead>
              <tbody>
                {poSummary?.upcomingDeliveries && poSummary.upcomingDeliveries.length > 0 ? (
                  poSummary.upcomingDeliveries.map((po) => (
                    <tr
                      key={po.id}
                      className="border-b border-slate-800/60 hover:bg-slate-800/40"
                    >
                      <td className="px-2 py-2 font-mono">{po.poNumber}</td>
                      <td className="px-2 py-2">
                        <div className="font-medium">{po.product.name}</div>
                        <div className="text-[11px] text-gray-400">
                          {po.product.sku} • {po.product.category}
                        </div>
                      </td>
                      <td className="px-2 py-2 text-center">{po.quantity}</td>
                      <td className="px-2 py-2 text-center">
                        {po.expectedDelivery || "N/A"}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={4}
                      className="px-2 py-3 text-center text-gray-500 text-xs"
                    >
                      No upcoming deliveries.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Critical Alerts & Recent Activity Tables */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
        {/* Critical / AI alerts */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700">
          <h2 className="font-semibold text-lg mb-2">Critical / AI Alerts</h2>
          <p className="text-xs text-gray-400 mb-2">
            AI-predicted stockouts and high-priority inventory issues.
          </p>

          <div className="overflow-x-auto max-h-80">
            <table className="min-w-full text-xs">
              <thead>
                <tr className="border-b border-slate-700 bg-slate-800/60">
                  <th className="px-2 py-2 text-left">Product</th>
                  <th className="px-2 py-2 text-left">Priority</th>
                  <th className="px-2 py-2 text-left">Message</th>
                  <th className="px-2 py-2 text-center">Shortfall</th>
                </tr>
              </thead>
              <tbody>
                {stockAlerts?.criticalAlerts && stockAlerts.criticalAlerts.length > 0 ? (
                  stockAlerts.criticalAlerts.map((alert) => (
                    <tr
                      key={alert.id}
                      className="border-b border-slate-800/60 hover:bg-slate-800/40"
                    >
                      <td className="px-2 py-2">
                        <div className="font-medium">{alert.product.name}</div>
                        <div className="text-[11px] text-gray-400">
                          {alert.product.sku} • {alert.product.category}
                        </div>
                      </td>
                      <td className="px-2 py-2">
                        <span
                          className={`px-2 py-1 rounded-full text-[10px] ${
                            alert.priority === "CRITICAL"
                              ? "bg-red-900/70 text-red-200"
                              : alert.priority === "HIGH"
                              ? "bg-orange-900/70 text-orange-200"
                              : alert.priority === "MEDIUM"
                              ? "bg-amber-900/70 text-amber-200"
                              : "bg-slate-700 text-slate-100"
                          }`}
                        >
                          {alert.priority}
                        </span>
                      </td>
                      <td className="px-2 py-2">
                        <div className="font-medium">{alert.title}</div>
                        <div className="text-[11px] text-gray-400 line-clamp-2">
                          {alert.message}
                        </div>
                      </td>
                      <td className="px-2 py-2 text-center">
                        {alert.predictedShortfall != null
                          ? Math.round(alert.predictedShortfall)
                          : "-"}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={4}
                      className="px-2 py-3 text-center text-gray-500 text-xs"
                    >
                      No critical alerts at the moment.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recent transactions & POs */}
        <div className="bg-slate-900 rounded-xl p-4 border border-slate-700 space-y-4">
          <div>
            <h2 className="font-semibold text-lg mb-2">Recent Transactions</h2>
            <div className="overflow-x-auto max-h-40">
              <table className="min-w-full text-xs">
                <thead>
                  <tr className="border-b border-slate-700 bg-slate-800/60">
                    <th className="px-2 py-2 text-left">Product</th>
                    <th className="px-2 py-2 text-center">Type</th>
                    <th className="px-2 py-2 text-center">Qty</th>
                    <th className="px-2 py-2 text-left">When</th>
                  </tr>
                </thead>
                <tbody>
                  {recentActivity?.recentTransactions &&
                  recentActivity.recentTransactions.length > 0 ? (
                    recentActivity.recentTransactions.map((t) => (
                      <tr
                        key={t.id}
                        className="border-b border-slate-800/60 hover:bg-slate-800/40"
                      >
                        <td className="px-2 py-2">
                          <div className="font-medium">{t.product.name}</div>
                          <div className="text-[11px] text-gray-400">
                            {t.product.sku}
                          </div>
                        </td>
                        <td className="px-2 py-2 text-center">
                          <span
                            className={`px-2 py-1 rounded-full text-[10px] ${
                              t.type === "IN"
                                ? "bg-emerald-900/60 text-emerald-200"
                                : "bg-red-900/60 text-red-200"
                            }`}
                          >
                            {t.type}
                          </span>
                        </td>
                        <td className="px-2 py-2 text-center">{t.quantity}</td>
                        <td className="px-2 py-2 text-left">
                          {new Date(t.timestamp).toLocaleString()}
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td
                        colSpan={4}
                        className="px-2 py-3 text-center text-gray-500 text-xs"
                      >
                        No recent inventory transactions.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div>
            <h2 className="font-semibold text-lg mb-2">Recent Purchase Orders</h2>
            <div className="overflow-x-auto max-h-40">
              <table className="min-w-full text-xs">
                <thead>
                  <tr className="border-b border-slate-700 bg-slate-800/60">
                    <th className="px-2 py-2 text-left">PO</th>
                    <th className="px-2 py-2 text-left">Product</th>
                    <th className="px-2 py-2 text-center">Qty</th>
                    <th className="px-2 py-2 text-center">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {recentActivity?.recentPurchaseOrders &&
                  recentActivity.recentPurchaseOrders.length > 0 ? (
                    recentActivity.recentPurchaseOrders.map((po) => (
                      <tr
                        key={po.id}
                        className="border-b border-slate-800/60 hover:bg-slate-800/40"
                      >
                        <td className="px-2 py-2 font-mono">{po.poNumber}</td>
                        <td className="px-2 py-2">
                          <div className="font-medium">{po.product.name}</div>
                          <div className="text-[11px] text-gray-400">
                            {po.product.sku}
                          </div>
                        </td>
                        <td className="px-2 py-2 text-center">{po.quantity}</td>
                        <td className="px-2 py-2 text-center">
                          <span
                            className={`px-2 py-1 rounded-full text-[10px] ${
                              po.status === "PENDING"
                                ? "bg-amber-900/60 text-amber-200"
                                : po.status === "APPROVED"
                                ? "bg-emerald-900/60 text-emerald-200"
                                : "bg-slate-700 text-slate-100"
                            }`}
                          >
                            {po.status}
                          </span>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td
                        colSpan={4}
                        className="px-2 py-3 text-center text-gray-500 text-xs"
                      >
                        No recent purchase orders.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-4 right-4 z-50">
          <div
            className={`px-4 py-2 rounded-md text-sm shadow-lg ${
              toast.type === "success"
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

export default AnalyticsReportsPage;
