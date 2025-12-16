import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import API from "../services/api";

const ProductDetails: React.FC = () => {
  const { id } = useParams();
  const [product, setProduct] = useState<any>(null);

  const loadProduct = async () => {
    const res = await API.get(`/products/${id}`);
    setProduct(res.data.data);
  };

  useEffect(() => {
    loadProduct();
  }, []);

  if (!product) return <AppLayout>Loading...</AppLayout>;

  const imageUrl = product.imageData
    ? `data:${product.imageType};base64,${product.imageData}`
    : null;

  return (
    <AppLayout>
      <h2>{product.name}</h2>

      <div className="row mt-4">

        <div className="col-md-4">
          {imageUrl ? (
            <img src={imageUrl} alt={product.name} className="img-fluid rounded border" />
          ) : (
            <div className="border p-4 text-center">No Image</div>
          )}
        </div>

        <div className="col-md-8">
          <h4>Product Information</h4>
          <p><strong>SKU:</strong> {product.sku}</p>
          <p><strong>Category:</strong> {product.category}</p>
          <p><strong>Description:</strong> {product.description}</p>
          <p><strong>Price:</strong> {product.price}</p>
          <p><strong>Stock:</strong> {product.currentStock}</p>
          <p><strong>Reorder Level:</strong> {product.reorderLevel}</p>
          <p><strong>Vendor:</strong> {product.vendorName}</p>

          <h5 className="mt-4">Stock Status: 
            <span style={{ color: product.stockStatusColor, marginLeft: 10 }}>
              {product.stockStatus}
            </span>
          </h5>
        </div>
      </div>

      <h4 className="mt-5">Transactions</h4>
      <table className="table table-bordered">
        <thead>
          <tr>
            <th>Type</th>
            <th>Qty</th>
            <th>Date</th>
            <th>Handled By</th>
            <th>Notes</th>
          </tr>
        </thead>
        <tbody>
          {product.transactions?.map((t: any) => (
            <tr key={t.id}>
              <td>{t.type}</td>
              <td>{t.quantity}</td>
              <td>{t.timestamp}</td>
              <td>{t.handledBy.fullName}</td>
              <td>{t.notes}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </AppLayout>
  );
};

export default ProductDetails;
