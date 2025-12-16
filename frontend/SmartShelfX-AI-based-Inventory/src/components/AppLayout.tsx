import React from "react";
import Navbar from "./Navbar";
import Sidebar from "./Sidebar";

const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <>
      <Navbar />
      <div className="d-flex">
        <Sidebar />

        {/* PAGE CONTENT */}
        <div className="container mt-4">
          {children}
        </div>
      </div>
    </>
  );
};

export default AppLayout;
