import React, { useContext } from "react";
import { AuthContext } from "../context/AuthContext";
import NotificationBell from "./NotificationBell";
import { useNavigate } from "react-router-dom";

const Navbar: React.FC = () => {
  const { logout, role } = useContext(AuthContext);
  const navigate = useNavigate();

  return (
    <nav className="navbar navbar-dark bg-dark px-3">
      <span className="navbar-brand">SmartShelfX</span>

      <div className="text-white d-flex align-items-center">
        <span className="me-4">
          Role: <strong>{role}</strong>
        </span>
        <div className="flex items-center gap-4">
        <NotificationBell onOpenAlertsPage={() => navigate("/alerts")} />
        {/* user avatar etc */}
      </div>
        <button className="btn btn-danger btn-sm" onClick={logout}>
          Logout
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
