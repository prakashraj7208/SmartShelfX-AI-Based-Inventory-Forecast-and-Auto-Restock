import React, { useState } from "react";
import API from "../services/api";
import AppLayout from "../components/AppLayout";
import { useNavigate } from "react-router-dom";

const roles = ["ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VENDOR"];

const CreateUser: React.FC = () => {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    role: "ROLE_MANAGER",
    phone: "",
    email: "",
    password: "",
    address: "",
    city: "",
    country: "",
  });

  const handleChange = (e: any) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    try {
      await API.post("/admin/users", form);
      navigate("/admin/users");
    } catch (err) {
      console.error("User create failed", err);
    }
  };

  return (
    <AppLayout>
      <h2>Create New User</h2>

      <form className="mt-4" onSubmit={handleSubmit}>
        
        <div className="row">
          <div className="col-md-6 mb-3">
            <label>First Name</label>
            <input className="form-control" name="firstName" onChange={handleChange} required />
          </div>

          <div className="col-md-6 mb-3">
            <label>Last Name</label>
            <input className="form-control" name="lastName" onChange={handleChange} required />
          </div>
        </div>

        <div className="mb-3">
          <label>Role</label>
          <select className="form-control" name="role" onChange={handleChange}>
            {roles.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        </div>

        <div className="mb-3">
          <label>Phone</label>
          <input className="form-control" name="phone" onChange={handleChange} required />
        </div>

        <div className="mb-3">
          <label>Email</label>
          <input type="email" className="form-control" name="email" onChange={handleChange} required />
        </div>

        <div className="mb-3">
          <label>Password</label>
          <input type="password" className="form-control" name="password" onChange={handleChange} required />
        </div>

        <button className="btn btn-primary">Create User</button>
      </form>
    </AppLayout>
  );
};

export default CreateUser;
