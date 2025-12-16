// src/pages/ProductCatalogPage.tsx

import React, { useEffect, useMemo, useState } from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import {
  addToCart,
  loadCart,
  type CartItem,
} from "../utils/cartUtils"; // FIXED IMPORT PATH


const API_BASE = "http://localhost:8080";

interface Product {
  id: number;
  name: string;
  sku?: string;
  category?: string;
  price: number;
  currentStock?: number;
  imageData?: string | null;
  imageType?: string | null;
  vendorName?: string | null;
  vendorId?: number | null;
}

const ProductCatalogPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [vendors, setVendors] = useState<{ id: number; name: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState<number | null>(null);

  const [search, setSearch] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");
  const [selectedVendor, setSelectedVendor] = useState("");

  const token = localStorage.getItem("token") || "";

  // ---------------------------
  // LOAD FILTERS
  // ---------------------------
  useEffect(() => {
    loadFilters();
    loadProducts();
  }, [page, search, selectedCategory, selectedVendor]);

  async function loadFilters() {
    try {
      const headers = token ? { Authorization: `Bearer ${token}` } : {};

      // both calls MUST include headers
      const [categoryRes, vendorRes] = await Promise.allSettled([
        fetch(`${API_BASE}/api/products/categories`, { headers }),
        fetch(`${API_BASE}/api/vendors`, { headers }),
      ]);

      // categories
      if (categoryRes.status === "fulfilled" && categoryRes.value.ok) {
        const json = await categoryRes.value.json();

        const arr =
          Array.isArray(json) ||
          json.data ||
          json.content ||
          json.categories ||
          [];

        setCategories(Array.isArray(arr) ? arr : []);
      }

      // vendors
      if (vendorRes.status === "fulfilled" && vendorRes.value.ok) {
        const json = await vendorRes.value.json();

        const arr =
          Array.isArray(json) ||
          json.data?.content ||
          json.data ||
          json.content ||
          [];

        setVendors(
          (Array.isArray(arr) ? arr : []).map((v: any) => ({
            id: v.id,
            name: `${v.firstName ?? ""} ${v.lastName ?? ""}`.trim() || v.email,
          }))
        );
      }
    } catch (err) {
      console.log("Filter load error:", err);
    }
  }

  // ---------------------------
  // LOAD PRODUCTS
  // ---------------------------
  async function loadProducts() {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        size: String(size),
        page: String(page),
        sortBy: "id",
        sortDir: "asc",
      });

      if (search) params.set("search", search);
      if (selectedCategory) params.set("category", selectedCategory);
      if (selectedVendor) params.set("vendor", selectedVendor);

      const res = await fetch(`${API_BASE}/api/products?${params.toString()}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      });

      if (!res.ok) throw new Error("Failed to fetch products");

      const json = await res.json();

      const content =
        json?.data?.content ??
        json?.content ??
        json?.data ??
        (Array.isArray(json) ? json : []);

      setProducts(Array.isArray(content) ? content : []);
      setTotalPages(json?.data?.totalPages ?? json?.totalPages ?? null);
    } catch (err) {
      console.error(err);
      setProducts([]);
    } finally {
      setLoading(false);
    }
  }

  // ---------------------------
  // FILTERED PRODUCTS (client-side)
  // ---------------------------
  const filtered = useMemo(() => {
    return products.filter((p) => {
      const q = search.toLowerCase();
      if (q && !p.name.toLowerCase().includes(q) && !p.sku?.toLowerCase().includes(q)) {
        return false;
      }
      if (selectedCategory && p.category !== selectedCategory) return false;
      if (selectedVendor && String(p.vendorId) !== selectedVendor) return false;
      return true;
    });
  }, [products, search, selectedCategory, selectedVendor]);

  // ---------------------------
  // ADD TO CART
  // ---------------------------
  const handleAddToCart = (p: Product) => {
    const item: CartItem = {
      productId: p.id,
      name: p.name,
      quantity: 1,
      price: p.price,
      imageUrl:
        p.imageData && p.imageType
          ? `data:${p.imageType};base64,${p.imageData}`
          : undefined,
    };

    addToCart(item);
    alert(`${p.name} added to cart`);
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between mb-3">
        <h2>Products</h2>

        <a href="/cart" className="btn btn-outline-primary">
          Cart ({loadCart().reduce((s, i) => s + i.quantity, 0)})
        </a>
      </div>

      {/* Filters */}
      <div className="row mb-3">
        <div className="col-md-3 mb-2">
          <input
            className="form-control"
            placeholder="Search name or SKU"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>

        <div className="col-md-3 mb-2">
          <select
            className="form-select"
            value={selectedCategory}
            onChange={(e) => {
              setSelectedCategory(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>

        <div className="col-md-3 mb-2">
          <select
            className="form-select"
            value={selectedVendor}
            onChange={(e) => {
              setSelectedVendor(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Vendors</option>
            {vendors.map((v) => (
              <option key={v.id} value={String(v.id)}>
                {v.name}
              </option>
            ))}
          </select>
        </div>

        <div className="col-md-3 text-end mb-2">
          <button
            className="btn btn-secondary me-2"
            onClick={() => {
              setSearch("");
              setSelectedCategory("");
              setSelectedVendor("");
              setPage(0);
            }}
          >
            Reset
          </button>

          <button className="btn btn-primary" onClick={loadProducts}>
            Refresh
          </button>
        </div>
      </div>

      {/* Product List */}
      {loading ? (
        <div className="alert alert-secondary">Loading...</div>
      ) : filtered.length === 0 ? (
        <div className="alert alert-info">No products found.</div>
      ) : (
        <div className="row g-3">
          {filtered.map((p) => (
            <div key={p.id} className="col-sm-6 col-md-4 col-lg-3">
              <div className="card h-100">
                <img
                  src={
                    p.imageData && p.imageType
                      ? `data:${p.imageType};base64,${p.imageData}`
                      : "/no-image.png"
                  }
                  className="card-img-top"
                  style={{ objectFit: "cover", height: 150 }}
                />

                <div className="card-body d-flex flex-column">
                  <h6>{p.name}</h6>
                  <small className="text-muted">{p.sku}</small>

                  <div className="mt-auto d-flex justify-content-between">
                    <div>
                      <strong>₹{p.price.toFixed(2)}</strong>
                      <div className="small">Stock: {p.currentStock ?? "-"}</div>
                    </div>

                    <button
                      className="btn btn-sm btn-outline-primary"
                      onClick={() => handleAddToCart(p)}
                    >
                      Add
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      <div className="d-flex justify-content-between mt-4">
        <span>
          Page {page + 1} {totalPages ? `of ${totalPages}` : ""}
        </span>

        <div>
          <button
            className="btn btn-outline-secondary me-2"
            disabled={page <= 0}
            onClick={() => setPage(page - 1)}
          >
            Prev
          </button>

          <button
            className="btn btn-outline-secondary"
            disabled={totalPages !== null && page + 1 >= totalPages}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductCatalogPage;
