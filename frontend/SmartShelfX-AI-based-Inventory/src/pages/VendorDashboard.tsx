import React, { useEffect, useRef, useState } from "react";
import Chart from "chart.js/auto";
import { addToCart, loadCart, type CartItem } from "../utils/cartUtils";

const API_BASE = "http://localhost:8080";

const VendorDashboard: React.FC = () => {
  const chartRef = useRef<HTMLCanvasElement | null>(null);
  const chartInstance = useRef<Chart | null>(null);
  const [products, setProducts] = useState<any[]>([]);
  const token = localStorage.getItem("token") || "";

  useEffect(() => {
    loadVendorProducts();
    renderStockChart();

    return () => {
      if (chartInstance.current) {
        chartInstance.current.destroy();
        chartInstance.current = null;
      }
    };
  }, []);

  async function loadVendorProducts() {
    try {
      const res = await fetch(`${API_BASE}/api/products?vendorOnly=true`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const json = await res.json();
      const content =
        json?.data?.content ?? json?.content ?? json?.data ?? (Array.isArray(json) ? json : []);
      setProducts(content);
    } catch (err) {
      console.error("Error loading vendor products", err);
    }
  }

  function renderStockChart() {
    const ctx = chartRef.current;
    if (!ctx) return;

    if (chartInstance.current) chartInstance.current.destroy();

    chartInstance.current = new Chart(ctx, {
      type: "line",
      data: {
        labels: ["Mon", "Tue", "Wed", "Thu", "Fri"],
        datasets: [
          {
            label: "Stock Movement",
            data: [10, 25, 15, 30, 22],
            borderWidth: 2,
          },
        ],
      },
    });
  }

  const renderImage = (p: any) => {
    if (p.imageData && p.imageType) {
      return `data:${p.imageType};base64,${p.imageData}`;
    }
    return "/no-image.png";
  };

  const handleAddToCart = (p: any) => {
    const item: CartItem = {
      productId: p.id,
      name: p.name,
      price: p.price,
      quantity: 1,
      sku: p.sku,
      imageUrl: renderImage(p),
    };

    addToCart(item);
    alert(`${p.name} added to cart`);
  };

  return (
    <div className="container py-4">
      <h2>Vendor Dashboard</h2>

      {/* Summary Cards */}
      <div className="row mt-4">
        <div className="col-md-4">
          <div className="card shadow-sm p-3">
            <h5>Total Products</h5>
            <h2>{products.length}</h2>
          </div>
        </div>

        <div className="col-md-4">
          <div className="card shadow-sm p-3">
            <h5>Items in Cart</h5>
            <h2>{loadCart().reduce((s, i) => s + i.quantity, 0)}</h2>
          </div>
        </div>

        <div className="col-md-4">
          <div className="card shadow-sm p-3">
            <h5>Low Stock Items</h5>
            <h2>{products.filter((p) => p.currentStock < p.reorderLevel).length}</h2>
          </div>
        </div>
      </div>

      {/* Stock Chart */}
      <div className="card p-3 my-4 shadow-sm">
        <h5>Weekly Stock Trend</h5>
        <canvas id="stockTrendChart" ref={chartRef}></canvas>
      </div>

      {/* Product Grid */}
      <div className="mt-4">
        <h4>Your Products</h4>
        <div className="row g-3 mt-2">
          {products.map((p) => (
            <div key={p.id} className="col-sm-6 col-md-4 col-lg-3">
              <div className="card h-100 shadow-sm">
                <img
                  src={renderImage(p)}
                  className="card-img-top"
                  style={{ height: 140, objectFit: "cover" }}
                  alt={p.name}
                />

                <div className="card-body d-flex flex-column">
                  <h6 className="fw-bold">{p.name}</h6>
                  <p className="text-muted small">{p.sku}</p>
                  <p className="fw-bold text-primary">₹{p.price}</p>
                  <p className="text-muted small">Stock: {p.currentStock}</p>

                  <div className="mt-auto d-flex justify-content-between">
                    <button
                      className="btn btn-sm btn-outline-success"
                      onClick={() => handleAddToCart(p)}
                    >
                      Add to Cart
                    </button>

                    <a
                      className="btn btn-sm btn-outline-primary"
                      href={`/product/${p.id}`}
                    >
                      View
                    </a>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {products.length === 0 && (
          <div className="alert alert-info mt-3">No products found.</div>
        )}
      </div>
    </div>
  );
};

export default VendorDashboard;
