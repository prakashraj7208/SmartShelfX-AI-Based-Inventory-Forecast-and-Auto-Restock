//package com.example.smartshelfx.controller;
//
//import com.example.smartshelfx.dto.ApiResponse;
//import com.example.smartshelfx.dto.DashboardStats;
//import com.example.smartshelfx.model.Product;
//import com.example.smartshelfx.service.AIService;
//import com.example.smartshelfx.service.DashboardService;
//import com.example.smartshelfx.service.ProductService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/dashboard")
//@RequiredArgsConstructor
//@Slf4j
//@CrossOrigin(origins = "http://localhost:5173")
//public class DashboardController {
//
//    private final DashboardService dashboardService;
//
//    @Autowired
//    private ProductService productService;
//
//    @Autowired
//    private AIService aiService;
//
//    @GetMapping("/stats")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
//    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats() {
//        try {
//            DashboardStats stats = dashboardService.getDashboardStats();
//            return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", stats));
//        } catch (Exception e) {
//            log.error("Error fetching dashboard stats: {}", e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body(ApiResponse.error("Failed to fetch dashboard stats", null));
//        }
//    }
//
//    @GetMapping("/analytics/inventory")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventoryAnalytics() {
//        try {
//            Map<String, Object> analytics = dashboardService.getInventoryAnalytics();
//            return ResponseEntity.ok(ApiResponse.success("Inventory analytics fetched", analytics));
//        } catch (Exception e) {
//            log.error("Error fetching inventory analytics: {}", e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body(ApiResponse.error("Failed to fetch inventory analytics", null));
//        }
//    }
//
//    @GetMapping("/analytics/sales")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesAnalytics() {
//        try {
//            Map<String, Object> analytics = dashboardService.getSalesAnalytics();
//            return ResponseEntity.ok(ApiResponse.success("Sales analytics fetched", analytics));
//        } catch (Exception e) {
//            log.error("Error fetching sales analytics: {}", e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body(ApiResponse.error("Failed to fetch sales analytics", null));
//        }
//    }
//
//    @GetMapping("/insights")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
//    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardInsights() {
//        try {
//            Map<String, Object> insights = new HashMap<>();
//
//            // Basic metrics
//            List<Product> allProducts = productService.getAllProducts();
//            List<Product> lowStockProducts = productService.getLowStockProducts();
//
//            insights.put("totalProducts", allProducts.size());
//            insights.put("lowStockCount", lowStockProducts.size());
//            insights.put("totalStockValue", calculateTotalStockValue(allProducts));
//
//            // AI-powered insights
//            try {
//                List<Map<String, Object>> aiRecommendations = aiService.getRestockRecommendations();
//                insights.put("aiRecommendations", aiRecommendations);
//            } catch (Exception e) {
//                insights.put("aiRecommendations", List.of(
//                        Map.of("message", "AI insights temporarily unavailable")
//                ));
//            }
//
//            // Critical alerts
//            long criticalAlerts = lowStockProducts.stream()
//                    .filter(p -> p.getCurrentStock() <= p.getSafetyStock())
//                    .count();
//            insights.put("criticalAlerts", criticalAlerts);
//
//            return ResponseEntity.ok(ApiResponse.success("Dashboard insights fetched", insights));
//
//        } catch (Exception e) {
//            log.error("Error fetching dashboard insights: {}", e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body(ApiResponse.error("Failed to load dashboard insights: " + e.getMessage(), null));
//        }
//    }
//
//
//    // ✅ FIXED: Proper BigDecimal multiplication
//    private double calculateTotalStockValue(List<Product> products) {
//        return products.stream()
//                .filter(p -> p.getPrice() != null && p.getCurrentStock() != null)
//                .mapToDouble(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getCurrentStock())).doubleValue())
//                .sum();
//    }
//
//    // ✅ ALTERNATIVE METHOD: Using BigDecimal for precision
//    private BigDecimal calculateTotalStockValuePrecise(List<Product> products) {
//        return products.stream()
//                .filter(p -> p.getPrice() != null && p.getCurrentStock() != null)
//                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getCurrentStock())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//}