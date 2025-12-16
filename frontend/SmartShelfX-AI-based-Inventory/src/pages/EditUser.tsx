import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import API from "../services/api";
import AppLayout from "../components/AppLayout";

const roles = ["ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VENDOR"];

const EditUser: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState<any>(null);

  const loadUser = async () => {
    const res = await API.get(`/admin/users/${id}`);
    setForm(res.data);
  };

  const handleChange = (e: any) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    await API.put(`/admin/users/${id}`, form);
    navigate("/admin/users");
  };

  useEffect(() => {
    loadUser();
  }, []);

  if (!form) return <AppLayout>Loading...</AppLayout>;

  return (
    <AppLayout>
      <h2>Edit User</h2>

      <form onSubmit={handleSubmit} className="mt-4">

        <div className="row">
          <div className="col-md-6 mb-3">
            <label>First Name</label>
            <input className="form-control" name="firstName" value={form.firstName} onChange={handleChange} />
          </div>

          <div className="col-md-6 mb-3">
            <label>Last Name</label>
            <input className="form-control" name="lastName" value={form.lastName} onChange={handleChange} />
          </div>
        </div>

        <div className="mb-3">
          <label>Role</label>
          <select className="form-control" name="role" value={form.role} onChange={handleChange}>
            {roles.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        </div>

        <div className="mb-3">
          <label>Phone</label>
          <input className="form-control" name="phone" value={form.phone} onChange={handleChange} />
        </div>

        <div className="mb-3">
          <label>Email</label>
          <input className="form-control" name="email" value={form.email} onChange={handleChange} />
        </div>

        <button className="btn btn-primary">Save Changes</button>
      </form>
    </AppLayout>
  );
};

export default EditUser;
