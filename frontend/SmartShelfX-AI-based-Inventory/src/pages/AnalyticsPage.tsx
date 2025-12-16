/* ---------- FULL CORRECTED AnalyticsPage.tsx WITH AUTH HEADERS ---------- */

import React, { useEffect, useMemo, useState } from "react";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
} from "chart.js";
import { Line } from "react-chartjs-2";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

const API_BASE = "http://localhost:8080";
const token = localStorage.getItem("token");                // <-- JWT Token loaded

/* ---------- Interfaces ---------- */

interface Vendor {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

interface Product {
  id: number;
  name: string;
  sku: string;
  category: string;
  price: number;
  costPrice?: number | null;
  currentStock: number;
  reorderLevel: number;
  safetyStock: number;
  stockStatus?: string;
  stockStatusColor?: string;
  suggestedReorderQuantity?: number;
  lowStock?: boolean;
  criticalStock?: boolean;
}

interface ProductsResponse {
  status: string;
  message: string;
  data: {
    content: Product[];
    currentPage: number;
    totalPages: number;
    pageSize: number;
    totalElements: number;
    lastPage: boolean;
  };
}

interface VendorsResponse {
  status: string;
  message: string;
  data: Vendor[];
}

type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL" | string;
type AiDecisionType = "ORDER_NOW" | "WAIT" | "MONITOR" | string;

/* ---------- AI Interfaces ---------- */

interface AiDecisionRaw {
  decision?: AiDecisionType;
  expectedDemand?: number;
  forecastPeriodDays?: number;
  recommendedReorderQuantity?: number;
  recommendedOrderQty?: number;
  riskLevel?: RiskLevel;
  explanation?: string;
  managerSummary?: string;
  [key: string]: any;
}

interface InventoryAiResponse {
  productId?: number;
  sku?: string;
  productName?: string;
  currentStock?: number;
  aiDecision?: AiDecisionRaw;
  createdPurchaseOrder?: {
    id: number;
    poNumber: string;
  } | null;
  [key: string]: any;
}

interface PredictionRow {
  productId: number;
  sku: string;
  name: string;
  currentStock: number;
  decision: AiDecisionType;
  expectedDemand: number | null;
  forecastPeriodDays: number | null;
  recommendedOrderQty: number | null;
  riskLevel: RiskLevel | null;
  managerSummary?: string;
  explanation?: string;
}

interface ToastState {
  type: "success" | "error";
  message: string;
}

/* ---------- Component ---------- */

const AnalyticsPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [aiRows, setAiRows] = useState<PredictionRow[]>([]);
  const [loadingProductId, setLoadingProductId] = useState<number | null>(null);
  const [bulkRunning, setBulkRunning] = useState(false);

  const [selectedRowForChart, setSelectedRowForChart] = useState<PredictionRow | null>(null);

  const [analyticsOverview, setAnalyticsOverview] = useState<string | null>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);

  // PO modal states
  const [poModalOpen, setPoModalOpen] = useState(false);
  const [poSubmitting, setPoSubmitting] = useState(false);
  const [poVendorId, setPoVendorId] = useState<number | "">("");
  const [poQuantity, setPoQuantity] = useState<number | "">("");
  const [poUnitPrice, setPoUnitPrice] = useState<number | "">("");
  const [poNotes, setPoNotes] = useState<string>("");
  const [poProduct, setPoProduct] = useState<Product | null>(null);

  const [toast, setToast] = useState<ToastState | null>(null);

  /* ---------- Toast ---------- */

  const showToast = (type: ToastState["type"], message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  };

  /* ---------- JSON Helper ---------- */

  const getJsonSafe = async <T,>(res: Response): Promise<T> => {
    const type = res.headers.get("content-type") || "";
    if (!type.includes("application/json")) {
      const txt = await res.text();
      throw new Error(`Expected JSON but got: ${txt.substring(0, 200)}`);
    }
    return (await res.json()) as T;
  };

  /* ---------- Initial Load: Products + Vendors + AI Overview ---------- */

  useEffect(() => {
    const load = async () => {
      setLoadingInitial(true);
      setError(null);

      try {
        /* --------------------- PRODUCTS (AUTH HEADER ADDED) --------------------- */
        const prodRes = await fetch(
          `${API_BASE}/api/products?size=50&page=0&sortBy=id&sortDir=asc`,
          {
            headers: {
              Authorization: `Bearer ${token}`,   // <-- FIXED
            },
          }
        );
        if (!prodRes.ok) throw new Error(`Failed to load products: ${prodRes.status}`);

        const prodJson = await getJsonSafe<ProductsResponse>(prodRes);
        setProducts(prodJson.data.content || []);

        /* --------------------- VENDORS (AUTH HEADER ADDED) --------------------- */
        const vendRes = await fetch(`${API_BASE}/api/vendors`, {
          headers: { Authorization: `Bearer ${token}` }, // <-- FIXED
        });

        if (!vendRes.ok) throw new Error(`Failed to load vendors: ${vendRes.status}`);

        const vendJson = await getJsonSafe<VendorsResponse>(vendRes);
        setVendors(vendJson.data || []);

        /* --------------------- AI OVERVIEW (AUTH HEADER ADDED) --------------------- */
        setAnalyticsLoading(true);

        const aiRes = await fetch(`${API_BASE}/api/ai/v2/analytics/overview`, {
          headers: { Authorization: `Bearer ${token}` }, // <-- FIXED
        });

        if (aiRes.ok) {
          const txt = await aiRes.text();
          setAnalyticsOverview(txt);
        } else {
          setAnalyticsOverview(null);
        }
      } catch (err: any) {
        setError(err.message || "Failed to load analytics");
      } finally {
        setAnalyticsLoading(false);
        setLoadingInitial(false);
      }
    };

    load();
  }, []);

  /* ---------- AI for Single Product (AUTH HEADER ADDED) ---------- */

  const runAiForProduct = async (product: Product) => {
    setLoadingProductId(product.id);

    try {
      const res = await fetch(
        `${API_BASE}/api/ai/v2/forecast-and-reorder/${product.id}?autoCreatePo=false`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,     // <-- FIXED
          },
        }
      );

      if (!res.ok) throw new Error(`AI call failed: ${res.status}`);

      const data = await getJsonSafe<InventoryAiResponse>(res);

      const aiDecision: AiDecisionRaw =
        data.aiDecision || (data as any).decision || {};

      const row: PredictionRow = {
        productId: product.id,
        sku: product.sku,
        name: product.name,
        currentStock: product.currentStock,
        decision: aiDecision.decision || "MONITOR",
        expectedDemand: aiDecision.expectedDemand ?? null,
        forecastPeriodDays: aiDecision.forecastPeriodDays ?? null,
        recommendedOrderQty:
          aiDecision.recommendedOrderQty ??
          aiDecision.recommendedReorderQuantity ??
          product.suggestedReorderQuantity ??
          null,
        riskLevel: aiDecision.riskLevel ?? null,
        managerSummary: aiDecision.managerSummary,
        explanation: aiDecision.explanation,
      };

      setAiRows((prev) => [...prev.filter((x) => x.productId !== product.id), row]);
      if (!selectedRowForChart) setSelectedRowForChart(row);

      showToast("success", `AI complete for ${product.sku}`);
    } catch (e: any) {
      showToast("error", e.message);
    } finally {
      setLoadingProductId(null);
    }
  };

  /* ---------- Bulk AI ---------- */

  const runAiOnLowStock = async () => {
    const targets = products.filter(
      (p) => p.lowStock || p.criticalStock || p.stockStatus === "LOW" || p.stockStatus === "CRITICAL"
    );

    if (targets.length === 0) {
      showToast("error", "No low-stock products.");
      return;
    }

    setBulkRunning(true);
    for (const p of targets) {
      await runAiForProduct(p);
    }
    setBulkRunning(false);

    showToast("success", "AI completed for all low-stock products");
  };

  /* ---------- Chart Data ---------- */

  const chartData = useMemo(() => {
    if (!selectedRowForChart) return null;

    const days = selectedRowForChart.forecastPeriodDays || 7;
    const demand = selectedRowForChart.expectedDemand || 0;
    const perDay = demand / days;

    const labels = [...Array(days)].map((_, i) => `Day ${i + 1}`);
    const values = labels.map((_, i) => Math.round(perDay * (0.8 + i / (days * 10))));

    return {
      labels,
      datasets: [
        {
          label: `Forecasted demand for ${selectedRowForChart.sku}`,
          data: values,
          borderWidth: 2,
          tension: 0.3,
        },
      ],
    };
  }, [selectedRowForChart]);

  /* ---------- OPEN PO MODAL ---------- */

  const openPoModal = (row: PredictionRow) => {
    const p = products.find((x) => x.id === row.productId) || null;

    setPoProduct(p);
    setPoVendorId(vendors[0]?.id ?? "");
    setPoQuantity(row.recommendedOrderQty ?? "");
    setPoUnitPrice(p?.costPrice ?? p?.price ?? "");
    setPoNotes(`AI-suggested PO for ${row.name}.`);
    setPoModalOpen(true);
  };

  const closePoModal = () => {
    setPoModalOpen(false);
    setPoProduct(null);
    setPoVendorId("");
    setPoQuantity("");
    setPoUnitPrice("");
    setPoNotes("");
  };

  /* ---------- Submit PO (AUTH HEADER ADDED) ---------- */

  const handleSubmitPo = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!poProduct || !poVendorId) return;

    setPoSubmitting(true);

    try {
      const body = {
        product: { id: poProduct.id },
        vendor: { id: poVendorId },
        quantity: Number(poQuantity),
        unitPrice: Number(poUnitPrice),
        notes: poNotes,
      };

      const res = await fetch(
        `${API_BASE}/api/purchase-orders?createdBy=1`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,         // <-- FIXED
            "Content-Type": "application/json",
          },
          body: JSON.stringify(body),
        }
      );

      if (!res.ok) throw new Error(await res.text());

      showToast("success", "Purchase Order created successfully!");
      closePoModal();
    } catch (err: any) {
      showToast("error", err.message);
    } finally {
      setPoSubmitting(false);
    }
  };

  /* ---------- Derived Stats ---------- */

  const lowStockCount = products.filter((p) => p.stockStatus === "LOW" || p.lowStock).length;
  const criticalStockCount = products.filter(
    (p) => p.stockStatus === "CRITICAL" || p.criticalStock
  ).length;
  const normalStockCount = products.filter((p) => p.stockStatus === "NORMAL").length;

  const suggestedRestocks = aiRows.filter((r) => r.decision === "ORDER_NOW");

  /* ---------- RENDER UI ---------- */

  if (loadingInitial) return <div className="p-6">Loading AI Analytics…</div>;
  if (error)
    return (
      <div className="p-6 text-red-500">
        Error loading: {error}
        <br />
        Check backend & login token.
      </div>
    );

  return (
    <div className="p-6 text-gray-100 space-y-6">

      {/** HEADER **/}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">AI Inventory Analytics</h1>
          <p className="text-gray-400 text-sm">
            Demand forecasting, stockout risks, and AI restocking recommendations
          </p>
        </div>

        <button
          onClick={runAiOnLowStock}
          disabled={bulkRunning}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 rounded-md"
        >
          {bulkRunning ? "Running AI..." : "Run AI on Low Stock"}
        </button>
      </div>

      {/** KPIs **/}
      <div className="grid grid-cols-4 gap-4">
        <div className="p-4 bg-slate-900 rounded-lg border border-slate-700">
          <div className="text-xs">Total Products</div>
          <div className="text-3xl">{products.length}</div>
        </div>

        <div className="p-4 bg-slate-900 rounded-lg border border-amber-700">
          <div className="text-xs">Low Stock</div>
          <div className="text-3xl text-amber-300">{lowStockCount}</div>
        </div>

        <div className="p-4 bg-slate-900 rounded-lg border border-red-700">
          <div className="text-xs">Critical</div>
          <div className="text-3xl text-red-400">{criticalStockCount}</div>
        </div>

        <div className="p-4 bg-slate-900 rounded-lg border border-emerald-700">
          <div className="text-xs">Healthy</div>
          <div className="text-3xl text-emerald-300">{normalStockCount}</div>
        </div>
      </div>

      {/** Forecast + Overview **/}
      <div className="grid grid-cols-3 gap-4">
        
        {/** FORECAST CHART **/}
        <div className="col-span-2 p-4 bg-slate-900 rounded-lg border border-slate-700">
          <h2 className="text-lg font-semibold mb-2">Forecast Trends</h2>
          {chartData ? (
            <Line data={chartData} />
          ) : (
            <div className="text-gray-500 text-center h-40 flex items-center justify-center">
              Run AI on a product to see forecast
            </div>
          )}
        </div>

        {/** OVERVIEW **/}
        <div className="p-4 bg-slate-900 rounded-lg border border-slate-700">
          <h2 className="font-semibold mb-2">AI Overview</h2>
          {analyticsLoading ? (
            <p>Loading...</p>
          ) : analyticsOverview ? (
            <pre className="text-xs whitespace-pre-wrap">{analyticsOverview}</pre>
          ) : (
            <p>No overview</p>
          )}
        </div>
      </div>

      {/** AI Table **/}
      <div className="p-4 bg-slate-900 rounded-lg border border-slate-700">
        <h2 className="font-semibold text-lg mb-2">AI Demand Predictions</h2>

        <table className="w-full text-sm">
          <thead className="bg-slate-800">
            <tr>
              <th className="px-3 py-2 text-left">SKU</th>
              <th className="px-3 py-2 text-left">Product</th>
              <th className="px-3 py-2 text-center">Current Stock</th>
              <th className="px-3 py-2 text-center">Demand</th>
              <th className="px-3 py-2 text-center">Days</th>
              <th className="px-3 py-2 text-center">Action</th>
              <th className="px-3 py-2 text-center">Risk</th>
              <th className="px-3 py-2 text-center">PO Qty</th>
              <th className="px-3 py-2 text-center">Tools</th>
            </tr>
          </thead>

          <tbody>
            {products.map((p) => {
              const row = aiRows.find((r) => r.productId === p.id);
              const loading = loadingProductId === p.id;

              return (
                <tr key={p.id} className="border-b border-slate-700">
                  <td className="px-3 py-2">{p.sku}</td>
                  <td className="px-3 py-2">{p.name}</td>
                  <td className="px-3 py-2 text-center">{p.currentStock}</td>
                  <td className="px-3 py-2 text-center">
                    {row?.expectedDemand ? Math.round(row.expectedDemand) : "-"}
                  </td>
                  <td className="px-3 py-2 text-center">
                    {row?.forecastPeriodDays ?? "-"}
                  </td>
                  <td className="px-3 py-2 text-center">{row?.decision ?? "-"}</td>
                  <td className="px-3 py-2 text-center">{row?.riskLevel ?? "-"}</td>
                  <td className="px-3 py-2 text-center">
                    {row?.recommendedOrderQty ?? "-"}
                  </td>
                  <td className="px-3 py-2 text-center">
                    <button
                      onClick={() => runAiForProduct(p)}
                      className="px-2 py-1 bg-slate-700 rounded-md text-xs"
                    >
                      {loading ? "Analyzing…" : "Run AI"}
                    </button>
                    {row?.decision === "ORDER_NOW" && (
                      <button
                        onClick={() => openPoModal(row)}
                        className="ml-2 px-2 py-1 bg-emerald-700 rounded-md text-xs"
                      >
                        PO
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/** Suggested Restock **/}
      <div className="p-4 bg-slate-900 rounded-lg border border-slate-700">
        <h2 className="font-semibold text-lg mb-2">Suggested Restock</h2>

        <table className="w-full text-sm">
          <thead className="bg-slate-800">
            <tr>
              <th className="px-3 py-2">SKU</th>
              <th className="px-3 py-2">Product</th>
              <th className="px-3 py-2 text-center">Stock</th>
              <th className="px-3 py-2 text-center">Qty</th>
              <th className="px-3 py-2 text-center">Risk</th>
              <th className="px-3 py-2 text-center">Generate PO</th>
            </tr>
          </thead>

          <tbody>
            {suggestedRestocks.map((row) => {
              const p = products.find((prod) => prod.id === row.productId);

              return (
                <tr key={row.productId} className="border-b border-slate-700">
                  <td className="px-3 py-2">{row.sku}</td>
                  <td className="px-3 py-2">{row.name}</td>
                  <td className="px-3 py-2 text-center">{p?.currentStock}</td>
                  <td className="px-3 py-2 text-center">{row.recommendedOrderQty}</td>
                  <td className="px-3 py-2 text-center">{row.riskLevel}</td>
                  <td className="px-3 py-2 text-center">
                    <button
                      onClick={() => openPoModal(row)}
                      className="px-3 py-1 bg-emerald-700 rounded-md text-xs"
                    >
                      Create PO
                    </button>
                  </td>
                </tr>
              );
            })}

            {suggestedRestocks.length === 0 && (
              <tr>
                <td colSpan={6} className="text-center py-4 text-gray-500">
                  No AI-suggested restocks yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/** PO Modal **/}
      {poModalOpen && poProduct && (
        <div className="fixed inset-0 bg-black/60 z-40 flex items-center justify-center">
          <div className="bg-slate-900 rounded-xl p-5 border border-slate-700 w-full max-w-lg">
            <div className="flex justify-between mb-3">
              <h3 className="font-semibold text-lg">Create Purchase Order</h3>
              <button onClick={closePoModal} className="text-xl">×</button>
            </div>

            <form onSubmit={handleSubmitPo} className="space-y-3">

              <div className="flex flex-col">
                <label>Vendor</label>
                <select
                  value={poVendorId}
                  onChange={(e) => setPoVendorId(Number(e.target.value))}
                  className="bg-slate-800 p-2 rounded"
                >
                  <option value="">Select vendor</option>
                  {vendors.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.firstName} {v.lastName}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col">
                <label>Quantity</label>
                <input
                  type="number"
                  value={poQuantity}
                  onChange={(e) => setPoQuantity(Number(e.target.value))}
                  className="bg-slate-800 p-2 rounded"
                />
              </div>

              <div className="flex flex-col">
                <label>Unit Price</label>
                <input
                  type="number"
                  value={poUnitPrice}
                  onChange={(e) => setPoUnitPrice(Number(e.target.value))}
                  className="bg-slate-800 p-2 rounded"
                />
              </div>

              <div className="flex flex-col">
                <label>Notes</label>
                <textarea
                  rows={3}
                  value={poNotes}
                  onChange={(e) => setPoNotes(e.target.value)}
                  className="bg-slate-800 p-2 rounded"
                ></textarea>
              </div>

              <div className="flex justify-end gap-2">
                <button type="button" onClick={closePoModal} className="px-3 py-1 border">
                  Cancel
                </button>
                <button type="submit" className="px-3 py-1 bg-emerald-600 rounded">
                  {poSubmitting ? "Saving…" : "Create PO"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/** Toast **/}
      {toast && (
        <div className="fixed bottom-4 right-4 bg-black p-3 rounded-md text-sm">
          {toast.message}
        </div>
      )}
    </div>
  );
};

export default AnalyticsPage;
