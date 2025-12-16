import React, { useEffect, useState, useContext } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { AuthContext } from "../context/AuthContext";

const StockIn: React.FC = () => {
  const [products, setProducts] = useState<any[]>([]);
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [notes, setNotes] = useState("");
  const [loading, setLoading] = useState(false);

  const { user } = useContext(AuthContext);

  const loadProducts = async () => {
    const res = await API.get("/products");
    setProducts(res.data.data.content);
  };

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    setLoading(true);

    try {
      await API.post("/inventory/stock-in", {
        productId: Number(productId),
        quantity: Number(quantity),
        handledBy: user?.id, // logged-in user ID
        notes,
      });

      alert("Stock added successfully!");
      setProductId("");
      setQuantity("");
      setNotes("");
    } catch (err) {
      console.error("Stock-In failed", err);
      alert("Failed to add stock");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  return (
    <AppLayout>
      <h2>Stock IN</h2>

      <form onSubmit={handleSubmit} className="mt-4" style={{ maxWidth: "500px" }}>
        
        <div className="mb-3">
          <label>Select Product</label>
          <select
            className="form-control"
            value={productId}
            onChange={(e) => setProductId(e.target.value)}
            required
          >
            <option value="">-- Select Product --</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} (SKU: {p.sku})
              </option>
            ))}
          </select>
        </div>

        <div className="mb-3">
          <label>Quantity</label>
          <input
            type="number"
            className="form-control"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            required
          />
        </div>

        <div className="mb-3">
          <label>Notes (optional)</label>
          <textarea
            className="form-control"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>

        <button className="btn btn-success" disabled={loading}>
          {loading ? "Processing..." : "Submit Stock-IN"}
        </button>
      </form>
    </AppLayout>
  );
};

export default StockIn;
