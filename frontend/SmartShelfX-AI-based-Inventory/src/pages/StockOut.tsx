import React, { useEffect, useState, useContext } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { AuthContext } from "../context/AuthContext";

const StockOut: React.FC = () => {
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

    const selected = products.find((p) => p.id == productId);
    if (!selected) return;

    if (Number(quantity) > selected.currentStock) {
      alert("Cannot remove more than current stock!");
      setLoading(false);
      return;
    }

    try {
      await API.post("/inventory/stock-out", {
        productId: Number(productId),
        quantity: Number(quantity),
        handledBy: user.id,  
        notes,
      });

      alert("Stock removed successfully!");
      setProductId("");
      setQuantity("");
      setNotes("");
    } catch (err) {
      console.error("Stock-Out failed", err);
      alert("Failed to remove stock");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  return (
    <AppLayout>
      <h2>Stock OUT</h2>

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
                {p.name} (Current: {p.currentStock})
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

        <button className="btn btn-danger" disabled={loading}>
          {loading ? "Processing..." : "Submit Stock-OUT"}
        </button>
      </form>
    </AppLayout>
  );
};

export default StockOut;
