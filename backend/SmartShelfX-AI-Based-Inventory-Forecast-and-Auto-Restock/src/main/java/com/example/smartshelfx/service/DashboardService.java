//package com.example.smartshelfx.service;
//
//import com.example.smartshelfx.dto.DashboardStats;
//import com.example.smartshelfx.model.Alert;
//import com.example.smartshelfx.model.PurchaseOrder;
//import com.example.smartshelfx.model.StockTransaction;
//import com.example.smartshelfx.repository.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class DashboardService {
//
//    private final ProductRepository productRepository;
//    private final UserRepository userRepository;
//    private final StockTransactionRepository stockTransactionRepository;
//    private final PurchaseOrderRepository purchaseOrderRepository;
//    private final AlertRepository alertRepository;
//    private final ForecastRepository forecastRepository;
//    private final ForecastService forecastService;
//    private final AlertService alertService; // Add this
//
//    public DashboardStats getDashboardStats() {
//
//        long totalProducts = productRepository.count();
//        long totalUsers = userRepository.count();
//        long lowStockProducts = productRepository.findLowStockProducts().size();
//        long criticalStockProducts = productRepository.findCriticalStockProducts().size();
//        long outOfStockProducts = productRepository.findOutOfStockProducts().size();
//        long pendingOrders = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.PENDING);
//        long unreadAlerts = alertRepository.countByIsReadFalse();
//
//        // Calculate inventory value
//        Double inventoryValue = productRepository.calculateTotalInventoryValue();
//        if (inventoryValue == null) inventoryValue = 0.0;
//
//        // FIX: Stock ins/outs now require LocalDateTime range
//        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
//        LocalDateTime endOfDay = startOfDay.plusDays(1);
//
//        long todayStockIns = stockTransactionRepository.countTodayStockIns(startOfDay, endOfDay);
//        long todayStockOuts = stockTransactionRepository.countTodayStockOuts(startOfDay, endOfDay);
//
//        // Forecast accuracy
//        Double forecastAccuracy = forecastRepository.findAverageAccuracy();
//        if (forecastAccuracy == null) forecastAccuracy = 0.0;
//
//        // Mock monthly values
//        Double monthlySales = 15000.0;
//        Double monthlyPurchases = 12000.0;
//
//        return DashboardStats.builder()
//                .totalProducts(totalProducts)
//                .totalUsers(totalUsers)
//                .lowStockItems(lowStockProducts)
//                .criticalStockItems(criticalStockProducts)
//                .outOfStockItems(outOfStockProducts)
//                .pendingPurchaseOrders(pendingOrders)
//                .unreadAlerts(unreadAlerts)
//                .inventoryValue(inventoryValue)
//                .todayStockIns(todayStockIns)
//                .todayStockOuts(todayStockOuts)
//                .forecastAccuracy(forecastAccuracy)
//                .monthlySales(monthlySales)
//                .monthlyPurchases(monthlyPurchases)
//                .build();
//    }
//
//
//    public Map<String, Object> getInventoryAnalytics() {
//        Map<String, Object> analytics = new HashMap<>();
//
//        // Category distribution
//        List<Object[]> categoryStats = productRepository.findProductCountByCategory();
//        analytics.put("categoryDistribution", categoryStats);
//
//        // Stock status
//        long outOfStock = productRepository.findOutOfStockProducts().size();
//        long lowStock = productRepository.findLowStockProducts().size();
//        long criticalStock = productRepository.findCriticalStockProducts().size();
//        long inStock = productRepository.count() - outOfStock - lowStock - criticalStock;
//
//        analytics.put("outOfStock", outOfStock);
//        analytics.put("lowStock", lowStock);
//        analytics.put("criticalStock", criticalStock);
//        analytics.put("inStock", inStock);
//
//        // Recent transactions - FIXED: Use the correct method
//        List<StockTransaction> recentTransactions = stockTransactionRepository.findRecentTransactions(10);
//        analytics.put("recentTransactions", recentTransactions);
//
//        // Recent alerts - FIXED: Use AlertService instead of direct repository call
//        List<Alert> recentAlerts = alertService.getRecentAlerts();
//        analytics.put("recentAlerts", recentAlerts);
//
//        // Purchase order status
//        Map<String, Long> poStatus = new HashMap<>();
//        for (PurchaseOrder.OrderStatus status : PurchaseOrder.OrderStatus.values()) {
//            Long count = purchaseOrderRepository.countByStatus(status);
//            poStatus.put(status.name(), count);
//        }
//        analytics.put("purchaseOrderStatus", poStatus);
//
//        return analytics;
//    }
//
//    public Map<String, Object> getSalesAnalytics() {
//        Map<String, Object> analytics = new HashMap<>();
//
//        // Mock sales data - in production, calculate from transactions
//        analytics.put("dailySales", generateMockDailySales());
//        analytics.put("topSellingProducts", generateMockTopSellingProducts());
//        analytics.put("salesTrend", generateMockSalesTrend());
//
//        return analytics;
//    }
//
//    // Mock data generators - replace with actual calculations
//    private List<Map<String, Object>> generateMockDailySales() {
//        return List.of(
//                Map.of("date", "2024-01-01", "sales", 1500),
//                Map.of("date", "2024-01-02", "sales", 1800),
//                Map.of("date", "2024-01-03", "sales", 1200),
//                Map.of("date", "2024-01-04", "sales", 2000),
//                Map.of("date", "2024-01-05", "sales", 1700)
//        );
//    }
//
//    private List<Map<String, Object>> generateMockTopSellingProducts() {
//        return List.of(
//                Map.of("product", "Laptop", "sales", 45, "revenue", 45000),
//                Map.of("product", "Mouse", "sales", 120, "revenue", 6000),
//                Map.of("product", "Keyboard", "sales", 80, "revenue", 8000),
//                Map.of("product", "Monitor", "sales", 25, "revenue", 25000),
//                Map.of("product", "Headphones", "sales", 60, "revenue", 9000)
//        );
//    }
//
//    private List<Map<String, Object>> generateMockSalesTrend() {
//        return List.of(
//                Map.of("month", "Jan", "sales", 45000, "forecast", 48000),
//                Map.of("month", "Feb", "sales", 52000, "forecast", 50000),
//                Map.of("month", "Mar", "sales", 48000, "forecast", 52000),
//                Map.of("month", "Apr", "sales", 55000, "forecast", 54000)
//        );
//    }
//}