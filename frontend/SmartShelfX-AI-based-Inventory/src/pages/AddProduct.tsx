import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../services/api";
import AppLayout from "../components/AppLayout";

const AddProduct: React.FC = () => {
    const navigate = useNavigate();

    const [form, setForm] = useState({
        name: "",
        sku: "",
        description: "",
        category: "",
        price: "",
        costPrice: "",
        currentStock: "",
        reorderLevel: "",
        safetyStock: "",
        leadTimeDays: "",
    });

    const [image, setImage] = useState<File | null>(null);
    const [loading, setLoading] = useState(false);

    const handleChange = (e: any) => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: any) => {
        e.preventDefault();
        setLoading(true);

        try {
            const formData = new FormData();

            // Convert product JSON -> string
            formData.append("product", JSON.stringify({
                ...form,
                price: Number(form.price),
                costPrice: Number(form.costPrice),
                currentStock: Number(form.currentStock),
                reorderLevel: Number(form.reorderLevel),
                safetyStock: Number(form.safetyStock),
                leadTimeDays: Number(form.leadTimeDays),
            }));

            // Append image if available
            if (image) {
                formData.append("image", image);
            }

            // CALL THE CORRECT ENDPOINT
            const res = await API.post("/products", formData, {
                headers: {
                    "Content-Type": "multipart/form-data",
                },
            });

            console.log("Saved:", res.data);
            navigate("/products");

        } catch (err) {
            console.error("Error adding product:", err);
        } finally {
            setLoading(false);
        }
    };


    return (
        <AppLayout>
            <h2>Add Product</h2>

            <form onSubmit={handleSubmit} className="mt-4">
                <div className="row">

                    <div className="col-md-6 mb-3">
                        <label>Name</label>
                        <input name="name" className="form-control" onChange={handleChange} required />
                    </div>

                    <div className="col-md-6 mb-3">
                        <label>SKU</label>
                        <input name="sku" className="form-control" onChange={handleChange} required />
                    </div>

                </div>

                <div className="mb-3">
                    <label>Description</label>
                    <textarea name="description" className="form-control" onChange={handleChange}></textarea>
                </div>

                <div className="row">

                    <div className="col-md-6 mb-3">
                        <label>Category</label>
                        <input name="category" className="form-control" onChange={handleChange} />
                    </div>

                    <div className="col-md-6 mb-3">
                        <label>Lead Time Days</label>
                        <input name="leadTimeDays" type="number" className="form-control" onChange={handleChange} />
                    </div>

                </div>

                <div className="row">

                    <div className="col-md-4 mb-3">
                        <label>Price</label>
                        <input name="price" type="number" className="form-control" onChange={handleChange} />
                    </div>

                    <div className="col-md-4 mb-3">
                        <label>Cost Price</label>
                        <input name="costPrice" type="number" className="form-control" onChange={handleChange} />
                    </div>

                    <div className="col-md-4 mb-3">
                        <label>Current Stock</label>
                        <input name="currentStock" type="number" className="form-control" onChange={handleChange} />
                    </div>

                </div>

                <div className="row">

                    <div className="col-md-6 mb-3">
                        <label>Reorder Level</label>
                        <input name="reorderLevel" type="number" className="form-control" onChange={handleChange} />
                    </div>

                    <div className="col-md-6 mb-3">
                        <label>Safety Stock</label>
                        <input name="safetyStock" type="number" className="form-control" onChange={handleChange} />
                    </div>

                </div>

                <div className="mb-3">
                    <label>Image (optional)</label>
                    <input
                        type="file"
                        accept="image/*"
                        className="form-control"
                        onChange={(e) => setImage(e.target.files?.[0] ?? null)}
                    />
                </div>

                <button className="btn btn-primary" disabled={loading}>
                    {loading ? "Saving..." : "Add Product"}
                </button>
            </form>
        </AppLayout>
    );
};

export default AddProduct;
