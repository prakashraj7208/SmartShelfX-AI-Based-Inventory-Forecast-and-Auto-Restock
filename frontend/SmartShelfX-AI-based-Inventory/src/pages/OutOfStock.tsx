import React, { useEffect, useState } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { Link } from "react-router-dom";

const OutOfStock: React.FC = () => {
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchOutOfStock = async () => {
    try {
      setLoading(true);
      const res = await API.get("/products/out-of-stock");
      setProducts(res.data.data || []);
    } catch (err) {
      console.error("Failed to fetch out of stock products", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOutOfStock();
  }, []);

  return (
    <AppLayout>
      <div className="container mt-4">
        <h2>❌ Out of Stock Products</h2>
        <p className="text-danger">These products are not available in inventory</p>

        {loading ? (
          <p>Loading...</p>
        ) : products.length === 0 ? (
          <p>No out of stock products.</p>
        ) : (
          <table className="table table-bordered table-striped">
            <thead className="table-dark">
              <tr>
                <th>Name</th>
                <th>SKU</th>
                <th>Category</th>
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

export default OutOfStock;
