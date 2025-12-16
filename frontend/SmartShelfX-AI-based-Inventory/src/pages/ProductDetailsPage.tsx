import React, { useEffect, useState } from "react";
import { addToCart, type CartItem } from "../utils/cartUtils";

const API_BASE = "http://localhost:8080";

const ProductDetailsPage: React.FC = () => {
  const [product, setProduct] = useState<any>(null);
  const id = window.location.pathname.split("/").pop();
  const token = localStorage.getItem("token") || "";

  useEffect(() => {
    loadProduct();
  }, []);

  async function loadProduct() {
    const res = await fetch(`${API_BASE}/api/products/${id}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined
    });

    const json = await res.json();
    setProduct(json.data ?? json);
  }

  if (!product) return <div className="container mt-5">Loading...</div>;

  const img =
    product.imageData && product.imageType
      ? `data:${product.imageType};base64,${product.imageData}`
      : "/no-image.png";

  const handleAddToCart = () => {
    const item: CartItem = {
      productId: product.id,
      name: product.name,
      price: product.price,
      quantity: 1,
      imageUrl: img,
      sku: product.sku,
    };

    addToCart(item);
    alert(`${product.name} added to cart`);
  };

  return (
    <div className="container mt-4">
      <h2>{product.name}</h2>
      <div className="row mt-3">
        <div className="col-md-5">
          <img src={img} className="img-fluid rounded shadow" alt={product.name} />
        </div>

        <div className="col-md-7">
          <h4>₹{product.price}</h4>
          <p><strong>SKU:</strong> {product.sku}</p>
          <p><strong>Stock:</strong> {product.currentStock}</p>
          <p><strong>Category:</strong> {product.category}</p>
          <p><strong>Vendor:</strong> {product.vendorName}</p>

          <button className="btn btn-primary mt-3" onClick={handleAddToCart}>
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductDetailsPage;
