package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnalyticsService.DashboardData>> getDashboardData() {
        try {
            AnalyticsService.DashboardData dashboardData = analyticsService.getDashboardData();
            return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", dashboardData));
        } catch (Exception e) {
            log.error("Error retrieving dashboard data: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve dashboard data", null));
        }
    }

    @GetMapping("/inventory-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnalyticsService.InventorySummary>> getInventorySummary() {
        try {
            AnalyticsService.InventorySummary summary = analyticsService.getInventorySummary();
            return ResponseEntity.ok(ApiResponse.success("Inventory summary retrieved", summary));
        } catch (Exception e) {
            log.error("Error retrieving inventory summary: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve inventory summary", null));
        }
    }

    @GetMapping("/stock-alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnalyticsService.StockAlerts>> getStockAlerts() {
        try {
            AnalyticsService.StockAlerts alerts = analyticsService.getStockAlerts();
            return ResponseEntity.ok(ApiResponse.success("Stock alerts retrieved", alerts));
        } catch (Exception e) {
            log.error("Error retrieving stock alerts: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve stock alerts", null));
        }
    }

    @GetMapping("/sales-metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnalyticsService.SalesMetrics>> getSalesMetrics() {
        try {
            AnalyticsService.SalesMetrics metrics = analyticsService.getSalesMetrics();
            return ResponseEntity.ok(ApiResponse.success("Sales metrics retrieved", metrics));
        } catch (Exception e) {
            log.error("Error retrieving sales metrics: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve sales metrics", null));
        }
    }

    @GetMapping("/alert-analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertAnalytics() {
        try {
            Map<String, Object> analytics = analyticsService.getAlertAnalytics();
            return ResponseEntity.ok(ApiResponse.success("Alert analytics retrieved", analytics));
        } catch (Exception e) {
            log.error("Error retrieving alert analytics: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve alert analytics", null));
        }
    }
}