import axios from "axios";

const API_BASE = "http://localhost:8080";

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    Authorization: `Bearer ${localStorage.getItem("token")}`,
    "Content-Type": "application/json",
  },
});

export function getVendorPOs() {
  return api.get("/purchase-orders/vendor");
}

export function approvePO(id: number, approvedBy: string) {
  return api.put(`/purchase-orders/${id}/approve?approvedBy=${approvedBy}`);
}

export function markOrdered(id: number) {
  return api.put(`/purchase-orders/${id}/mark-ordered`);
}

export function markDelivered(id: number, receivedBy: string) {
  return api.put(`/purchase-orders/${id}/mark-delivered?receivedBy=${receivedBy}`);
}
