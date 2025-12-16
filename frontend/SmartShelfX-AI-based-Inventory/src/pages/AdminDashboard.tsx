import React, { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import API from "../services/api";
import { Link } from "react-router-dom";

// ⬇️ NEW: import recharts components
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";

const AdminDashboard: React.FC = () => {
  const [totalProducts, setTotalProducts] = useState<number | null>(null);
  const [lowCount, setLowCount] = useState(0);
  const [criticalCount, setCriticalCount] = useState(0);
  const [outCount, setOutCount] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadDashboard = async () => {
    try {
      setLoading(true);

      const [productsRes, lowRes, criticalRes, outRes] = await Promise.all([
        API.get("/products?size=1&page=0"), // just for totalElements
        API.get("/products/low-stock"),
        API.get("/products/critical-stock"),
        API.get("/products/out-of-stock"),
      ]);

      // total products from paged response
      const paged = productsRes.data?.data;
      if (paged && typeof paged.totalElements === "number") {
        setTotalProducts(paged.totalElements);
      }

      setLowCount((lowRes.data?.data || []).length);
      setCriticalCount((criticalRes.data?.data || []).length);
      setOutCount((outRes.data?.data || []).length);
    } catch (err) {
      console.error("Failed to load dashboard", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  // ⬇️ NEW: derive normal stock safely
  const normalCount = Math.max(
    0,
    (totalProducts ?? 0) - lowCount - criticalCount - outCount
  );

  // ⬇️ NEW: data for charts
  const barData = [
    { name: "Normal", value: normalCount },
    { name: "Low", value: lowCount },
    { name: "Critical", value: criticalCount },
    { name: "Out", value: outCount },
  ];

  const pieData = [
    { name: "Normal", value: normalCount },
    { name: "Low", value: lowCount },
    { name: "Critical", value: criticalCount },
    { name: "Out", value: outCount },
  ];

  const pieColors = ["#28a745", "#ffc107", "#dc3545", "#343a40"];

  return (
    <AppLayout>
      <div className="container mt-4">
        <h2>📊 Inventory Dashboard (Admin)</h2>
        <p className="text-muted">Overview of current inventory health.</p>

        {loading ? (
          <p>Loading dashboard...</p>
        ) : (
          <>
            {/* Summary Cards */}
            <div className="row mb-4">
              <div className="col-md-3 mb-3">
                <div className="card text-bg-primary">
                  <div className="card-body">
                    <h5 className="card-title">Total Products</h5>
                    <h2>{totalProducts ?? "-"}</h2>
                  </div>
                </div>
              </div>

              <div className="col-md-3 mb-3">
                <div className="card text-bg-success">
                  <div className="card-body">
                    <h5 className="card-title">Normal Stock</h5>
                    <h2>{normalCount}</h2>
                  </div>
                </div>
              </div>

              <div className="col-md-3 mb-3">
                <div className="card text-bg-warning">
                  <div className="card-body">
                    <h5 className="card-title">Low Stock</h5>
                    <h2>{lowCount}</h2>
                  </div>
                </div>
              </div>

              <div className="col-md-3 mb-3">
                <div className="card text-bg-danger">
                  <div className="card-body">
                    <h5 className="card-title">Critical Stock</h5>
                    <h2>{criticalCount}</h2>
                  </div>
                </div>
              </div>

              <div className="col-md-3 mb-3">
                <div className="card text-bg-dark">
                  <div className="card-body">
                    <h5 className="card-title">Out of Stock</h5>
                    <h2>{outCount}</h2>
                  </div>
                </div>
              </div>
            </div>

            {/* ⬇️ NEW: Charts Row */}
            <div className="row mb-4">
              {/* Bar chart */}
              <div className="col-md-6 mb-3">
                <div className="card h-100">
                  <div className="card-header">Stock Status (Bar Chart)</div>
                  <div className="card-body" style={{ height: 300 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={barData}>
                        <XAxis dataKey="name" />
                        <YAxis allowDecimals={false} />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="value" name="Count" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>

              {/* Pie chart */}
              <div className="col-md-6 mb-3">
                <div className="card h-100">
                  <div className="card-header">Stock Distribution (Pie Chart)</div>
                  <div className="card-body d-flex justify-content-center" style={{ height: 300 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie
                          data={pieData}
                          dataKey="value"
                          nameKey="name"
                          outerRadius={100}
                          label
                        >
                          {pieData.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill={pieColors[index]} />
                          ))}
                        </Pie>
                        <Tooltip />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            </div>

            {/* Quick links + Stock alerts */}
            <div className="row">
              <div className="col-md-6">
                <div className="card">
                  <div className="card-header">Quick Actions</div>
                  <div className="card-body">
                    <ul className="list-unstyled">
                      <li>
                        <Link to="/products">📦 View All Products</Link>
                      </li>
                      <li>
                        <Link to="/products/create">➕ Add New Product</Link>
                      </li>
                      <li>
                        <Link to="/stock-in">⬆️ Stock In</Link>
                      </li>
                      <li>
                        <Link to="/stock-out">⬇️ Stock Out</Link>
                      </li>
                      <li>
                        <Link to="/admin/transactions">📑 Recent Transactions</Link>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="col-md-6 mt-3 mt-md-0">
                <div className="card">
                  <div className="card-header">Stock Alerts</div>
                  <div className="card-body">
                    <p>
                      <strong>{lowCount}</strong> products are in{" "}
                      <span className="text-warning">low stock</span>.
                    </p>
                    <p>
                      <strong>{criticalCount}</strong> products are in{" "}
                      <span className="text-danger">critical stock</span>.
                    </p>
                    <p>
                      <strong>{outCount}</strong> products are{" "}
                      <span className="text-dark">out of stock</span>.
                    </p>
                    <div className="d-flex gap-2 flex-wrap">
                      <Link to="/admin/low-stock" className="btn btn-sm btn-warning">
                        View Low Stock
                      </Link>
                      <Link to="/admin/critical-stock" className="btn btn-sm btn-danger">
                        View Critical Stock
                      </Link>
                      <Link to="/admin/out-of-stock" className="btn btn-sm btn-dark">
                        View Out of Stock
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
};

export default AdminDashboard;
