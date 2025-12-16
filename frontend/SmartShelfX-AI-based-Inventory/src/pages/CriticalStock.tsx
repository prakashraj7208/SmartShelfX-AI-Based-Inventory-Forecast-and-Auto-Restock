import React, { useEffect, useState } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { Link } from "react-router-dom";

const CriticalStock: React.FC = () => {
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchCritical = async () => {
    try {
      setLoading(true);
      const res = await API.get("/products/critical-stock");
      setProducts(res.data.data || []);
    } catch (err) {
      console.error("Failed to fetch critical stock products", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCritical();
  }, []);

  return (
    <AppLayout>
      <div className="container mt-4">
        <h2>🚨 Critical Stock Products</h2>
        <p className="text-danger">Products critically low — restock immediately</p>

        {loading ? (
          <p>Loading...</p>
        ) : products.length === 0 ? (
          <p>No critical stock products.</p>
        ) : (
          <table className="table table-bordered table-striped">
            <thead className="table-danger">
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

export default CriticalStock;
