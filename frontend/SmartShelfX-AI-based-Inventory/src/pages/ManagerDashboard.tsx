import React, { useEffect, useState } from "react";
import AppLayout from "../components/AppLayout";
import API from "../services/api";
import { Link } from "react-router-dom";

const ManagerDashboard: React.FC = () => {
  const [totalProducts, setTotalProducts] = useState<number | null>(null);
  const [lowCount, setLowCount] = useState(0);
  const [criticalCount, setCriticalCount] = useState(0);
  const [outCount, setOutCount] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadDashboard = async () => {
    try {
      setLoading(true);

      const [productsRes, lowRes, criticalRes, outRes] = await Promise.all([
        API.get("/products?size=1&page=0"),
        API.get("/products/low-stock"),
        API.get("/products/critical-stock"),
        API.get("/products/out-of-stock"),
      ]);

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

  return (
    <AppLayout>
      <div className="container mt-4">
        <h2>📊 Inventory Dashboard (Manager)</h2>
        <p className="text-muted">Quick view of inventory and stock alerts.</p>

        {loading ? (
          <p>Loading dashboard...</p>
        ) : (
          <>
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

            <div className="card">
              <div className="card-header">Quick Actions</div>
              <div className="card-body">
                <ul className="list-unstyled">
                  <li>
                    <Link to="/products">📦 View Products</Link>
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
                  <li>
                    <Link to="/admin/low-stock">⚠️ Low Stock Items</Link>
                  </li>
                </ul>
              </div>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
};

export default ManagerDashboard;
