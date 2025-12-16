import React, { useEffect, useState } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { Link } from "react-router-dom";

interface Product {
    id: number;
    name: string;
    sku: string;
    vendor: string;
    category: string;
    currentStock: number;
    reorderLevel: number;
}

const Products: React.FC = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState("");

    const loadProducts = async () => {
        setLoading(true);
        try {
            const res = await API.get("/products");

            const payload = res.data;

            // content array exists? → GOOD!
            if (payload?.data?.content && Array.isArray(payload.data.content)) {
                setProducts(payload.data.content);
            }
            else {
                console.error("Unexpected products API format:", payload);
                setProducts([]);
            }
        } catch (err) {
            console.error("Error loading products", err);
            setProducts([]);
        } finally {
            setLoading(false);
        }
    };


    const handleSearch = async () => {
        if (!search.trim()) return loadProducts();

        try {
            const res = await API.get(`/products/search?query=${search}`);
            const payload = res.data;

            if (payload?.data?.content && Array.isArray(payload.data.content)) {
                setProducts(payload.data.content);
            } else {
                setProducts([]);
            }
        } catch (err) {
            console.error("Search failed", err);
        }
    };


    const deleteProduct = async (id: number) => {
        if (!confirm("Delete this product?")) return;
        try {
            await API.delete(`/products/${id}`);
            loadProducts();
        } catch (err) {
            console.error("Delete failed", err);
        }
    };

    useEffect(() => {
        loadProducts();
    }, []);

    return (
        <AppLayout>
            <div className="d-flex justify-content-between align-items-center">
                <h2>Products</h2>
                <Link to="/products/create" className="btn btn-primary">
                    + Add Product
                </Link>
            </div>

            {/* Search bar */}
            <div className="input-group my-3" style={{ maxWidth: "400px" }}>
                <input
                    type="text"
                    placeholder="Search products..."
                    className="form-control"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
                <button className="btn btn-secondary" onClick={handleSearch}>Search</button>
            </div>

            {loading ? (
                <p>Loading...</p>
            ) : (
                <table className="table table-striped">
                    <thead>
                        <tr>
                            <th>SKU</th>
                            <th>Name</th>
                            <th>Vendor</th>
                            <th>Category</th>
                            <th>Stock</th>
                            <th>Reorder Level</th>
                            <th>Actions</th>
                        </tr>
                    </thead>

                    <tbody>
                        {products.map((p) => (
                            <tr key={p.id}>
                                <td>{p.sku}</td>
                                <td>{p.name}</td>
                                <td>{p.vendor}</td>
                                <td>{p.category}</td>
                                <td>{p.currentStock}</td>
                                <td>{p.reorderLevel}</td>
                                <td>
                                    {/* VIEW BUTTON */}
                                    <Link to={`/products/${p.id}`} className="btn btn-sm btn-info me-2">
                                        View
                                    </Link>

                                    {/* EDIT BUTTON */}
                                    <Link to={`/products/edit/${p.id}`} className="btn btn-sm btn-warning me-2">
                                        Edit
                                    </Link>

                                    {/* DELETE BUTTON */}
                                    <button className="btn btn-sm btn-danger" onClick={() => deleteProduct(p.id)}>
                                        Delete
                                    </button>
                                </td>

                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </AppLayout>
    );
};

export default Products;
