// src/pages/CheckoutPage.tsx
import React, { useEffect, useMemo, useState } from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import { type CartItem, loadCart, removeFromCart, updateQty, clearCart } from "../utils/cartUtils";

const API_BASE = "http://localhost:8080";

const CheckoutPage: React.FC = () => {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [notes, setNotes] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    setCart(loadCart());
  }, []);

  const total = useMemo(
    () => cart.reduce((s, it) => s + it.price * it.quantity, 0),
    [cart]
  );

  const handleQtyChange = (id: number, qty: number) => {
    const updated = updateQty(id, qty);
    setCart(updated);
  };

  const handleRemove = (id: number) => {
    const updated = removeFromCart(id);
    setCart(updated);
  };

  const handlePlaceOrder = async () => {
    if (cart.length === 0) {
      setMessage("Cart is empty.");
      return;
    }
    setSubmitting(true);
    setMessage(null);

    try {
      // Build a simple PO body with first vendor = null (you can expand to support vendor per-item)
      // Using assumption: backend expects product: {id}, vendor: {id}, quantity, unitPrice, notes
      const createdByUserId = 1; // adapt as needed
      // For demo: create separate POs per product (simple). You can change to batch PO endpoint if exists.
      const responses = [];
      for (const item of cart) {
        const body = {
          product: { id: item.id },
          vendor: null,
          quantity: item.quantity,
          unitPrice: item.price,
          notes: notes || `Order from frontend checkout. ${notes ? notes : ""}`,
        };
        const res = await fetch(`${API_BASE}/api/purchase-orders?createdBy=${createdByUserId}`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(body),
        }).catch(() => null);

        if (!res || !res.ok) {
          // If backend not available, fall back to simulation
          responses.push({ ok: false, message: "backend unavailable - simulated order created" });
        } else {
          const json = await res.json();
          responses.push({ ok: true, data: json });
        }
      }

      // If at least one was ok, treat as success
      const anyOk = responses.some((r) => r.ok);
      if (anyOk) {
        setMessage("Order placed successfully (some orders created). Clearing cart.");
        clearCart();
        setCart([]);
      } else {
        setMessage("Could not place order to backend — simulated success locally.");
        // optionally keep or clear cart: we clear to simulate completed checkout
        clearCart();
        setCart([]);
      }
    } catch (err: any) {
      console.error(err);
      setMessage(err.message || "Failed to place order");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-4">
      <h2>Checkout</h2>
      <div className="row">
        <div className="col-md-8">
          {cart.length === 0 ? (
            <div className="alert alert-info">Your cart is empty.</div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th style={{ width: 120 }}>Qty</th>
                  <th>Price</th>
                  <th>Total</th>
                  <th style={{ width: 120 }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {cart.map((it) => (
                  <tr key={it.id}>
                    <td>
                      <strong>{it.name}</strong>
                      <div className="small text-muted">{it.sku}</div>
                    </td>
                    <td>
                      <div className="input-group input-group-sm">
                        <button
                          className="btn btn-outline-secondary"
                          onClick={() => handleQtyChange(it.id, Math.max(1, it.quantity - 1))}
                        >
                          -
                        </button>
                        <input
                          type="number"
                          className="form-control text-center"
                          value={it.quantity}
                          min={1}
                          onChange={(e) => handleQtyChange(it.id, Math.max(1, Number(e.target.value)))}
                        />
                        <button
                          className="btn btn-outline-secondary"
                          onClick={() => handleQtyChange(it.id, it.quantity + 1)}
                        >
                          +
                        </button>
                      </div>
                    </td>
                    <td>₹{it.price.toFixed(2)}</td>
                    <td>₹{(it.price * it.quantity).toFixed(2)}</td>
                    <td>
                      <button className="btn btn-danger btn-sm" onClick={() => handleRemove(it.id)}>
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="col-md-4">
          <div className="card">
            <div className="card-body">
              <h5>Order Summary</h5>
              <p className="mb-1">Items: {cart.length}</p>
              <p className="mb-3">Total: <strong>₹{total.toFixed(2)}</strong></p>

              <div className="mb-3">
                <label className="form-label small">Notes (optional)</label>
                <textarea className="form-control" rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} />
              </div>

              <button className="btn btn-success w-100" onClick={handlePlaceOrder} disabled={submitting || cart.length===0}>
                {submitting ? "Placing order…" : "Place Order"}
              </button>

              {message && <div className="alert alert-info mt-3 small">{message}</div>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;
