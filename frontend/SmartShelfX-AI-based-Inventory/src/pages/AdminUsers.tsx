import React, { useEffect, useState } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { Link } from "react-router-dom";

interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: string;
}

const AdminUsers: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  const loadUsers = async () => {
    try {
      const res = await API.get("/admin/users");

      // Correct path: response.data.data is the array
      if (Array.isArray(res.data.data)) {
        setUsers(res.data.data);
      } else {
        console.error("Unexpected API format:", res.data);
        setUsers([]);
      }
    } catch (err) {
      console.error("Error loading users", err);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const deleteUser = async (id: number) => {
    if (!confirm("Are you sure you want to delete this user?")) return;

    try {
      await API.delete(`/admin/users/${id}`);
      loadUsers();
    } catch (err) {
      console.error("Delete failed", err);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  return (
    <AppLayout>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2>Users Management</h2>
        <Link to="/admin/users/create" className="btn btn-primary">
          + Add User
        </Link>
      </div>

      {loading ? (
        <p>Loading users...</p>
      ) : (
        <table className="table table-striped">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th style={{ width: "180px" }}>Actions</th>
            </tr>
          </thead>

          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.firstName} {u.lastName}</td>
                <td>{u.email}</td>
                <td>{u.phone}</td>
                <td>{u.role}</td>
                <td>
                  <Link to={`/admin/users/edit/${u.id}`} className="btn btn-sm btn-warning me-2">
                    Edit
                  </Link>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => deleteUser(u.id)}
                  >
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

export default AdminUsers;
