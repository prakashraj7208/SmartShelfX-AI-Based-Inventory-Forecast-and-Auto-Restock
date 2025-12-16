import React, { useEffect, useState, useContext } from "react";
import API from "../services/api";
import { AuthContext } from "../context/AuthContext";
import AppLayout from "../components/AppLayout";
import moment from "moment";

interface Transaction {
  id: number;
  quantity: number;
  type: "IN" | "OUT";
  timestamp: string;
  notes: string | null;
  referenceNumber: string | null;
  handledBy: {
    id: number;
    fullName: string;
    role: string;
  };
  product: {
    id: number;
    name: string;
    sku: string;
  };
}

const RecentTransactions: React.FC = () => {
  const { role } = useContext(AuthContext);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const res = await API.get("/inventory/transactions/recent");

      // backend returns: { status, message, data: [...] }
      setTransactions(res.data.data || []);
    } catch (err) {
      console.error("Error fetching recent transactions", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  return (
    <AppLayout>
      <div className="container mt-4">
        <h2 className="mb-3">Recent Transactions</h2>

        {loading ? (
          <p>Loading...</p>
        ) : transactions.length === 0 ? (
          <p>No recent transactions found.</p>
        ) : (
          <table className="table table-bordered table-striped">
            <thead className="table-dark">
              <tr>
                <th>ID</th>
                <th>Product</th>
                <th>SKU</th>
                <th>Type</th>
                <th>Quantity</th>
                <th>Handled By</th>
                <th>Notes</th>
                <th>Timestamp</th>
              </tr>
            </thead>

            <tbody>
              {transactions.map((t) => (
                <tr key={t.id}>
                  <td>{t.id}</td>

                  <td>
                    <a href={`/products/${t.product.id}`} className="text-primary">
                      {t.product.name}
                    </a>
                  </td>

                  <td>{t.product.sku}</td>

                  <td>
                    {t.type === "IN" ? (
                      <span className="badge bg-success">IN</span>
                    ) : (
                      <span className="badge bg-danger">OUT</span>
                    )}
                  </td>

                  <td>{t.quantity}</td>

                  <td>{t.handledBy.fullName}</td>

                  <td>{t.notes || "-"}</td>

                  <td>{moment(t.timestamp).format("YYYY-MM-DD HH:mm")}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </AppLayout>
  );
};

export default RecentTransactions;
