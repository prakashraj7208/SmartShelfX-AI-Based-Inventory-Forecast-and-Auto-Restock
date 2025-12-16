import React, { useState } from "react";
import API from "../services/api";

const roles = ["ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VENDOR"];

const Register: React.FC = () => {
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    role: "ROLE_MANAGER",
    phone: "",
    email: "",
    password: "",
    company: "",
    address: "",
    city: "",
    country: ""
  });

  const [message, setMessage] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);

    try {
      const res = await API.post("/auth/register", form);
      setMessage("Registration successful!");
    } catch (err) {
      setMessage("Registration failed");
    }
  };

  return (
    <div className="container mt-5 col-md-6">
      <div className="card p-4">
        <h3 className="text-center">Register New User</h3>

        {message && <div className="alert alert-info">{message}</div>}

        <form onSubmit={handleSubmit}>
          <div className="row">
            <div className="col-md-6 mb-3">
              <label>First Name</label>
              <input name="firstName" className="form-control" onChange={handleChange} required />
            </div>

            <div className="col-md-6 mb-3">
              <label>Last Name</label>
              <input name="lastName" className="form-control" onChange={handleChange} required />
            </div>
          </div>

          <div className="mb-3">
            <label>Role</label>
            <select name="role" className="form-control" onChange={handleChange}>
              {roles.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
          </div>

          <div className="mb-3">
            <label>Phone</label>
            <input name="phone" className="form-control" onChange={handleChange} required />
          </div>

          <div className="mb-3">
            <label>Email</label>
            <input type="email" name="email" className="form-control" onChange={handleChange} required />
          </div>

          <div className="mb-3">
            <label>Password</label>
            <input type="password" name="password" className="form-control" onChange={handleChange} required />
          </div>

          <div className="mb-3">
            <label>Company</label>
            <input name="company" className="form-control" onChange={handleChange} />
          </div>

          <div className="mb-3">
            <label>Address</label>
            <input name="address" className="form-control" onChange={handleChange} />
          </div>

          <div className="row">
            <div className="col-md-6 mb-3">
              <label>City</label>
              <input name="city" className="form-control" onChange={handleChange} />
            </div>
            <div className="col-md-6 mb-3">
              <label>Country</label>
              <input name="country" className="form-control" onChange={handleChange} />
            </div>
          </div>

          <button className="btn btn-primary w-100">Register</button>
        </form>
      </div>
    </div>
  );
};

export default Register;
