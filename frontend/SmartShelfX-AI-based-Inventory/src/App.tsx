import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Unauthorized from "./pages/Unauthorized";

import ProtectedRoute from "./components/ProtectedRoute";

import AdminDashboard from "./pages/AdminDashboard";
import ManagerDashboard from "./pages/ManagerDashboard";
import VendorDashboard from "./pages/VendorDashboard";

import AdminUsers from "./pages/AdminUsers";
import CreateUser from "./pages/CreateUser";
import EditUser from "./pages/EditUser";

import Products from "./pages/Products";
import AddProduct from "./pages/AddProduct";
import EditProduct from "./pages/EditProduct";
import ProductDetails from "./pages/ProductDetails";
import StockOut from "./pages/StockOut";
import StockIn from "./pages/StockIn";
import RecentTransactions from "./pages/RecentTransactions";
import OutOfStock from "./pages/OutOfStock";
import CriticalStock from "./pages/CriticalStock";
import LowStock from "./pages/LowStock";
import AnalyticsPage from "./pages/AnalyticsPage";
import ManualPurchaseOrderPage from "./pages/ManualPurchaseOrderPage";
import AlertsPage from "./pages/AlertsPage";
import AnalyticsReportsPage from "./pages/AnalyticsReportsPage";
import CartPage from "./pages/CartPage";
import CheckoutPage from "./pages/CheckoutPage";
import ProductCatalogPage from "./pages/ProductCatalogPage";
import ProductDetailsPage from "./pages/ProductDetailsPage";


function App() {
  return (
    <AuthProvider>   {/* ✅ FIX: wrap everything */}
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/unauthorized" element={<Unauthorized />} />

          {/* Admin → Users */}
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
                <AdminUsers />
              </ProtectedRoute>
            }
          />
          <Route
            path="/stock-in"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_MANAGER"]}>
                <StockIn />
              </ProtectedRoute>
            }
          />

          <Route
            path="/stock-out"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_MANAGER", "ROLE_VENDOR"]}>
                <StockOut />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/transactions"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_MANAGER"]}>
                <RecentTransactions />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/low-stock"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_MANAGER"]}>
                <LowStock />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin/critical-stock"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_MANAGER"]}>
                <CriticalStock />
              </ProtectedRoute>
            }
          />
          <Route path="/admin/analytics" element={<AnalyticsPage />} />
          <Route
            path="/admin/out-of-stock"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_MANAGER"]}>
                <OutOfStock />
              </ProtectedRoute>
            }
          />

          <Route
            path="/products"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN", "ROLE_MANAGER"]}>
                <Products />
              </ProtectedRoute>
            }
          />

          <Route path="/products/create" element={<AddProduct />} />
          <Route path="/products/edit/:id" element={<EditProduct />} />
          <Route path="/products/:id" element={<ProductDetails />} />
          <Route path="/admin/ManualPurchaseOrderPage" element={<ManualPurchaseOrderPage />} />
              <Route path="/alerts" element={<AlertsPage />} />
          <Route
            path="/admin/users/create"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
                <CreateUser />
              </ProtectedRoute>
            }
          />
<Route path="/AnalyticsReportsPage" element={<AnalyticsReportsPage/>} />
          <Route
            path="/admin/users/edit/:id"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
                <EditUser />
              </ProtectedRoute>
            }
          />
          <Route path="/products" element={<ProductCatalogPage/>} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
<Route path="/product/:id" element={<ProductDetailsPage />} />

          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute allowedRoles={["ROLE_ADMIN"]}>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/manager/dashboard"
            element={
              <ProtectedRoute allowedRoles={["ROLE_MANAGER", "ROLE_ADMIN"]}>
                <ManagerDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/vendor/dashboard"
            element={
              <ProtectedRoute allowedRoles={["ROLE_VENDOR"]}>
                 <VendorDashboard />
              </ProtectedRoute>
            }
          />

          <Route path="*" element={<Login />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
