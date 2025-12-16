import React, { useEffect, useState } from "react";

const API_BASE = "http://localhost:8080";

const ManualPurchaseOrderPage: React.FC = () => {
  const [vendors, setVendors] = useState<any[]>([]);
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const [selectedVendor, setSelectedVendor] = useState("");
  const [selectedProduct, setSelectedProduct] = useState("");
  const [quantity, setQuantity] = useState<number>(1);
  const [unitPrice, setUnitPrice] = useState<number>(0);
  const [notes, setNotes] = useState("");

  const [poResponse, setPoResponse] = useState<any>(null);
  const [emailLoading, setEmailLoading] = useState(false);

  // -------------------------------------------------------------
  // FETCH VENDORS
  // -------------------------------------------------------------
  const fetchVendors = async () => {
    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`${API_BASE}/api/vendors`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = await res.json();
      setVendors(data.data || []);
    } catch (err) {
      console.error("Vendor fetch error:", err);
    }
  };

  // -------------------------------------------------------------
  // FETCH PRODUCTS (NON-PAGINATED VERSION)
  // -------------------------------------------------------------
  const fetchProducts = async () => {
    try {
      const token = localStorage.getItem("token");

      const res = await fetch(`${API_BASE}/api/products?size=1000&page=0`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const json = await res.json();

      if (json?.data?.content) {
        setProducts(json.data.content);
      }
    } catch (err) {
      console.error("Products fetch error:", err);
    }
  };

  useEffect(() => {
    Promise.all([fetchVendors(), fetchProducts()]).finally(() =>
      setLoading(false)
    );
  }, []);

  // -------------------------------------------------------------
  // CREATE PURCHASE ORDER
  // -------------------------------------------------------------
  const createPurchaseOrder = async () => {
    if (!selectedVendor || !selectedProduct || quantity <= 0) {
      alert("Please fill out all fields");
      return;
    }

    const token = localStorage.getItem("token");

    const body = {
      product: { id: Number(selectedProduct) },
      vendor: { id: Number(selectedVendor) },
      quantity: Number(quantity),
      unitPrice: Number(unitPrice),
      notes,
    };

    try {
      const res = await fetch(
        `${API_BASE}/api/purchase-orders?createdBy=${selectedVendor}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(body),
        }
      );

      const json = await res.json();
      setPoResponse(json.data);

      alert("Purchase Order Created Successfully!");
    } catch (err) {
      console.error("PO Error:", err);
    }
  };

  // -------------------------------------------------------------
  // SEND EMAIL (Manually)
  // -------------------------------------------------------------
  const sendEmail = async () => {
    if (!poResponse) {
      alert("Create a PO first!");
      return;
    }

    setEmailLoading(true);

    const vendorEmail = poResponse.vendor?.email;
    const vendorName = poResponse.vendor?.fullName;
    const poNumber = poResponse.poNumber;
    const productName = poResponse.product?.name;
    const qty = poResponse.quantity;

    const token = localStorage.getItem("token");

    try {
      const res = await fetch(
        `${API_BASE}/api/email/test-po?email=${vendorEmail}&vendorName=${vendorName}&poNumber=${poNumber}&productName=${productName}&quantity=${qty}`,
        {
          method: "GET",
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      await res.text();

      alert("Email sent successfully!");
    } catch (err) {
      console.error("Email send error:", err);
      alert("Failed to send email");
    }

    setEmailLoading(false);
  };

  if (loading)
    return (
      <div className="p-6 text-center text-xl font-semibold">Loading...</div>
    );

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Manual Purchase Order</h1>

      <div className="space-y-5 bg-white shadow p-6 rounded-lg">
        {/* Vendor */}
        <div>
          <label className="block font-semibold mb-1">Select Vendor</label>
          <select
            className="w-full border p-2 rounded"
            value={selectedVendor}
            onChange={(e) => setSelectedVendor(e.target.value)}
          >
            <option value="">-- Select Vendor --</option>
            {vendors.map((v) => (
              <option key={v.id} value={v.id}>
                {v.fullName} ({v.email})
              </option>
            ))}
          </select>
        </div>

        {/* Product */}
        <div>
          <label className="block font-semibold mb-1">Select Product</label>
          <select
            className="w-full border p-2 rounded"
            value={selectedProduct}
            onChange={(e) => {
              setSelectedProduct(e.target.value);

              const p = products.find(
                (prod) => prod.id === Number(e.target.value)
              );
              if (p) setUnitPrice(p.costPrice || p.price || 0);
            }}
          >
            <option value="">-- Select Product --</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} (SKU: {p.sku})
              </option>
            ))}
          </select>
        </div>

        {/* Quantity */}
        <div>
          <label className="block font-semibold mb-1">Quantity</label>
          <input
            type="number"
            className="w-full border p-2 rounded"
            min={1}
            value={quantity}
            onChange={(e) => setQuantity(Number(e.target.value))}
          />
        </div>

        {/* Unit Price */}
        <div>
          <label className="block font-semibold mb-1">Unit Price</label>
          <input
            type="number"
            className="w-full border p-2 rounded"
            value={unitPrice}
            onChange={(e) => setUnitPrice(Number(e.target.value))}
          />
        </div>

        {/* Notes */}
        <div>
          <label className="block font-semibold mb-1">Notes</label>
          <textarea
            className="w-full border p-2 rounded"
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>

        {/* Create PO Button */}
        <button
          onClick={createPurchaseOrder}
          className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
        >
          Create Purchase Order
        </button>
      </div>

      {/* After PO created */}
      {poResponse && (
        <div className="mt-8 p-6 bg-green-50 rounded-lg shadow">
          <h2 className="text-xl font-bold mb-3">PO Created</h2>
          <p>
            <b>PO Number:</b> {poResponse.poNumber}
          </p>
          <p>
            <b>Vendor:</b> {poResponse.vendor.fullName}
          </p>
          <p>
            <b>Product:</b> {poResponse.product.name}
          </p>
          <p>
            <b>Quantity:</b> {poResponse.quantity}
          </p>

          {/* Send Email Button */}
          <button
            onClick={sendEmail}
            disabled={emailLoading}
            className="mt-4 w-full bg-purple-600 text-white py-2 rounded hover:bg-purple-700 disabled:bg-gray-400"
          >
            {emailLoading ? "Sending Email..." : "Send Email to Vendor"}
          </button>
        </div>
      )}
    </div>
  );
};

export default ManualPurchaseOrderPage;
