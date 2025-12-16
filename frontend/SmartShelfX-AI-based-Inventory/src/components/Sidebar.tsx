import React, { useContext } from "react";
import { Link } from "react-router-dom";
import { AuthContext } from "../context/AuthContext";

const Sidebar: React.FC = () => {
  const { role } = useContext(AuthContext);

  // Menus for each role
  const adminMenu = [
    { label: "Dashboard", path: "/admin/dashboard" },
    { label: "Users", path: "/admin/users" },
    //{ label: "Vendors", path: "/admin/vendors" },
    { label: "Inventory", path: "/products" },
    //{ label: "Inventory", path: "/inventory" },
   // { label: "Purchase Orders", path: "/purchase-orders" },
    { label: "Analytics", path: "/admin/analytics" },
    //{ label: "productdetails", path: "/productdetails" },
    { label: "Recent Transactions", path: "/admin/transactions" },
    { label: "Critical Stock", path: "/admin/critical-stock" },
    { label: "Low Stock", path: "/admin/low-stock" },
    { label: "Out of Stock", path: "/admin/out-of-stock" },
    { label: "PurchaseOrderPage", path: "/admin/ManualPurchaseOrderPage" },
    { label: "AnalyticsReportsPage", path: "/AnalyticsReportsPage" },
  ];

  const managerMenu = [
    { label: "Dashboard", path: "/manager/dashboard" },
   // { label: "Products", path: "/vendors/products" },
   { label: "Inventory", path: "/products" },
    { label: "Stock In", path: "/stock-in" },
    { label: "Stock Out", path: "/stock-out" },
    // { label: "Inventory", path: "/products" },
    { label: "Alerts", path: "/alerts" },
    //{ label: "Analytics", path: "/manager/analytics" },
    { label: "Recent Transactions", path: "/admin/transactions" },
    { label: "Critical Stock", path: "/admin/critical-stock" },
    { label: "Low Stock", path: "/admin/low-stock" },
    { label: "Out of Stock", path: "/admin/out-of-stock" },
  ];

  const vendorMenu = [
    { label: "Dashboard", path: "/vendor/dashboard" },
    { label: "products", path: "/products" },
    { label: "Purchase Orders", path: "/vendor/:id" },
    { label: "CheckoutPage", path: "/checkout" },
    { label: "Profile", path: "/vendor/profile" }
  ];

  let menu: any[] = [];

  if (role === "ROLE_ADMIN") menu = adminMenu;
  else if (role === "ROLE_MANAGER") menu = managerMenu;
  else if (role === "ROLE_VENDOR") menu = vendorMenu;

  return (
    <div style={{ width: "250px", height: "100vh" }} className="bg-light border-end p-3">
      <h5 className="mb-3">Menu</h5>
      <ul className="list-group">
        {menu.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className="list-group-item list-group-item-action"
          >
            {item.label}
          </Link>
        ))}
      </ul>
    </div>
  );
};

export default Sidebar;
