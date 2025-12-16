import { Navigate } from "react-router-dom";
import { useContext } from "react";
import { AuthContext } from "../context/AuthContext";

const ProtectedRoute = ({ children, allowedRoles }: any) => {
  const { token, role } = useContext(AuthContext);

  const isAuthenticated = !!token;

  console.log("ProtectedRoute:", { isAuthenticated, role, allowedRoles });

  // NOT LOGGED IN → redirect to login
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // ROLE NOT ALLOWED
  if (!allowedRoles.includes(role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
};

export default ProtectedRoute;
