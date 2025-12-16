import { createContext, useState, useEffect } from "react";
import API from "../services/api";

export const AuthContext = createContext<any>(null);

export const AuthProvider = ({ children }: any) => {
  const [token, setToken] = useState(localStorage.getItem("token"));
  const [role, setRole] = useState(localStorage.getItem("role"));
  const [user, setUser] = useState<any>(
    localStorage.getItem("user")
      ? JSON.parse(localStorage.getItem("user") as string)
      : null
  );

  // Fetch user from backend
  const fetchMe = async () => {
    try {
      const res = await API.get("/auth/me");
      const userData = res.data.data;

      setUser(userData);
      localStorage.setItem("user", JSON.stringify(userData));
    } catch (err) {
      console.error("Failed to fetch user details", err);
    }
  };

  // On initial load, if token exists → fetch user
  useEffect(() => {
    if (token && !user) {
      fetchMe();
    }
  }, []);

  // LOGIN
  const login = async (accessToken: string, role: string) => {
    setToken(accessToken);
    setRole(role);

    localStorage.setItem("token", accessToken);
    localStorage.setItem("role", role);

    await fetchMe(); // fetch user immediately after login
  };

  // LOGOUT
  const logout = () => {
    setToken(null);
    setRole(null);
    setUser(null);
    localStorage.clear();
  };

  return (
    <AuthContext.Provider value={{ token, role, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
