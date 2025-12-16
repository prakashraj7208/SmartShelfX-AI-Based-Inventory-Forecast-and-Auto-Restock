import React, { useEffect, useState } from "react";
import "bootstrap/dist/css/bootstrap.min.css";

interface CartItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  imageData?: string;
  imageType?: string;
}

const CartPage: React.FC = () => {
  const [cart, setCart] = useState<CartItem[]>([]);

  useEffect(() => {
    const stored = JSON.parse(localStorage.getItem("cart") || "[]");
    setCart(stored);
  }, []);

  const saveCart = (updated: CartItem[]) => {
    setCart(updated);
    localStorage.setItem("cart", JSON.stringify(updated));
  };

  const increaseQty = (id: number) => {
    const updated = cart.map((item) =>
      item.id === id ? { ...item, quantity: item.quantity + 1 } : item
    );
    saveCart(updated);
  };

  const decreaseQty = (id: number) => {
    const updated = cart
      .map((item) =>
        item.id === id ? { ...item, quantity: Math.max(1, item.quantity - 1) } : item
      )
      .filter((item) => item.quantity > 0);
    saveCart(updated);
  };

  const removeItem = (id: number) => {
    const updated = cart.filter((item) => item.id !== id);
    saveCart(updated);
  };

  const clearCart = () => {
    saveCart([]);
  };

  const total = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  const getImage = (item: CartItem) => {
    if (!item.imageData) return "/no-image.png";
    return `data:${item.imageType};base64,${item.imageData}`;
  };

  return (
    <div className="container py-4">
      <h2 className="mb-4 fw-bold">Your Cart</h2>

      {cart.length === 0 ? (
        <div className="alert alert-info text-center">Your cart is empty.</div>
      ) : (
        <>
          <table className="table table-bordered align-middle">
            <thead className="table-dark">
              <tr>
                <th>Product</th>
                <th style={{ width: "120px" }}>Quantity</th>
                <th>Price</th>
                <th>Total</th>
                <th style={{ width: "120px" }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {cart.map((item) => (
                <tr key={item.id}>
                  <td>
                    <img
                      src={getImage(item)}
                      alt={item.name}
                      width={60}
                      className="me-2 rounded"
                    />
                    {item.name}
                  </td>
                  <td>
                    <div className="d-flex align-items-center gap-2">
                      <button
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() => decreaseQty(item.id)}
                      >
                        -
                      </button>
                      <span>{item.quantity}</span>
                      <button
                        className="btn btn-outline-secondary btn-sm"
                        onClick={() => increaseQty(item.id)}
                      >
                        +
                      </button>
                    </div>
                  </td>
                  <td>₹{item.price.toFixed(2)}</td>
                  <td>₹{(item.price * item.quantity).toFixed(2)}</td>
                  <td>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => removeItem(item.id)}
                    >
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="d-flex justify-content-between align-items-center mt-3">
            <h4>Total: ₹{total.toFixed(2)}</h4>

            <div className="d-flex gap-2">
              <button className="btn btn-warning" onClick={clearCart}>
                Clear Cart
              </button>

              <a href="/checkout" className="btn btn-success">
                Proceed to Checkout
              </a>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default CartPage;
