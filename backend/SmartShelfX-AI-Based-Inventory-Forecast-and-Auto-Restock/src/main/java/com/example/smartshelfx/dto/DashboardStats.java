package com.example.smartshelfx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {
    private Long totalProducts;
    private Long totalUsers;
    private Long lowStockItems;
    private Long pendingPurchaseOrders;
    private Long unreadAlerts;
    private Double inventoryValue;
    private Long todayStockIns;
    private Long todayStockOuts;
    private Double forecastAccuracy;

    // Additional metrics
    private Long criticalStockItems;
    private Long outOfStockItems;
    private Double monthlySales;
    private Double monthlyPurchases;
}