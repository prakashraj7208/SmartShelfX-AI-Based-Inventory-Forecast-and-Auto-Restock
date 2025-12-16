import React, { useEffect, useState } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { Link } from "react-router-dom";

const LowStock: React.FC = () => {
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchLowStock = async () => {
    try {
      setLoading(true);
      const res = await API.get("/products/low-stock");
      setProducts(res.data.data || []);
    } catch (err) {
      console.error("Failed to fetch low stock products", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLowStock();
  }, []);

  return (
    <AppLayout>
      <div className="container mt-4">
        <h2>📉 Low Stock Products</h2>
        <p className="text-muted">Products below their reorder level</p>

        {loading ? (
          <p>Loading...</p>
        ) : products.length === 0 ? (
          <p>No low stock products.</p>
        ) : (
          <table className="table table-bordered table-striped">
            <thead className="table-warning">
              <tr>
                <th>Name</th>
                <th>SKU</th>
                <th>Category</th>
                <th>Current Stock</th>
                <th>Reorder Level</th>
                <th>Vendor</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>{p.sku}</td>
                  <td>{p.category}</td>
                  <td>{p.currentStock}</td>
                  <td>{p.reorderLevel}</td>
                  <td>{p.vendorName || "No Vendor"}</td>
                  <td>
                    <Link to={`/products/${p.id}`} className="btn btn-primary btn-sm">
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </AppLayout>
  );
};

export default LowStock;
