import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import API from "../services/api";
import AppLayout from "../components/AppLayout";

const EditProduct: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState<any>(null);
  const [image, setImage] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);

  const loadProduct = async () => {
    const res = await API.get(`/products/${id}`);
    setForm(res.data.data);
  };

  const handleChange = (e: any) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    setLoading(true);

    try {
      const formData = new FormData();

      // Convert updated product JSON → string
      formData.append("product", JSON.stringify({
        ...form,
        price: Number(form.price),
        costPrice: Number(form.costPrice),
        currentStock: Number(form.currentStock),
        reorderLevel: Number(form.reorderLevel),
        safetyStock: Number(form.safetyStock),
        leadTimeDays: Number(form.leadTimeDays),
      }));

      // Add new image only if selected
      if (image) {
        formData.append("image", image);
      }

      // IMPORTANT: axios PUT with multipart
      const res = await API({
        url: `/products/${id}`,
        method: "put",
        data: formData,
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });

      console.log("Updated:", res.data);
      navigate("/products");

    } catch (err) {
      console.error("Update failed:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProduct();
  }, []);

  if (!form) return <AppLayout>Loading...</AppLayout>;

  const existingImage = form.imageData
    ? `data:${form.imageType};base64,${form.imageData}`
    : null;

  return (
    <AppLayout>
      <h2>Edit Product</h2>

      <form onSubmit={handleSubmit} className="mt-4">

        <div className="row">
          <div className="col-md-6 mb-3">
            <label>Name</label>
            <input
              className="form-control"
              name="name"
              value={form.name}
              onChange={handleChange}
            />
          </div>

          <div className="col-md-6 mb-3">
            <label>SKU</label>
            <input
              className="form-control"
              name="sku"
              value={form.sku}
              onChange={handleChange}
            />
          </div>
        </div>

        <div className="mb-3">
          <label>Description</label>
          <textarea
            className="form-control"
            name="description"
            value={form.description}
            onChange={handleChange}
          />
        </div>

        <div className="row">
          <div className="col-md-6 mb-3">
            <label>Category</label>
            <input
              className="form-control"
              name="category"
              value={form.category}
              onChange={handleChange}
            />
          </div>

          <div className="col-md-6 mb-3">
            <label>Lead Time Days</label>
            <input
              className="form-control"
              name="leadTimeDays"
              type="number"
              value={form.leadTimeDays}
              onChange={handleChange}
            />
          </div>
        </div>

        {/* Show existing image */}
        {existingImage && (
          <div className="mb-3">
            <label>Existing Image</label>
            <img
              src={existingImage}
              alt="Product"
              className="img-fluid rounded border d-block mb-3"
              style={{ maxWidth: "200px" }}
            />
          </div>
        )}

        <div className="mb-3">
          <label>Change Image (optional)</label>
          <input
            type="file"
            accept="image/*"
            className="form-control"
            onChange={(e) => setImage(e.target.files?.[0] ?? null)}
          />
        </div>

        <button className="btn btn-primary" disabled={loading}>
          {loading ? "Saving..." : "Save Changes"}
        </button>
      </form>
    </AppLayout>
  );
};

export default EditProduct;
