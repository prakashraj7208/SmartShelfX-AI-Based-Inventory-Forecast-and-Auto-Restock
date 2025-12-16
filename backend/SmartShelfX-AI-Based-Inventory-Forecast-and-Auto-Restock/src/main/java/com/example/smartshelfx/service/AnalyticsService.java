package com.example.smartshelfx.service;

import com.example.smartshelfx.dto.RestockSuggestion;
import com.example.smartshelfx.model.Alert;
import com.example.smartshelfx.model.PurchaseOrder;
import com.example.smartshelfx.model.StockTransaction;
import com.example.smartshelfx.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Corrected AnalyticsService:
 * - Uses LocalDateTime where repository expects LocalDateTime.
 * - Uses LocalDate where repository expects LocalDate.
 * - Handles nulls and type conversions from primitive long -> Long safely.
 * - Defensive programming to avoid cascading exceptions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AlertRepository alertRepository;
    private final ForecastRepository forecastRepository;

    public DashboardData getDashboardData() {
        try {
            return DashboardData.builder()
                    .inventorySummary(getInventorySummary())
                    .stockAlerts(getStockAlerts())
                    .recentActivity(getRecentActivity())
                    .purchaseOrderSummary(getPurchaseOrderSummary())
                    .salesMetrics(getSalesMetrics())
                    .aiInsights(getAIInsights())
                    .build();
        } catch (Exception e) {
            log.error("Error assembling dashboard data: {}", e.getMessage(), e);
            // Return an "empty" dashboard object rather than throw, keeping API stable
            return DashboardData.builder()
                    .inventorySummary(new InventorySummary(0L, 0L, 0L, 0.0))
                    .stockAlerts(new StockAlerts(0L, 0L, 0L, Collections.emptyList()))
                    .recentActivity(new RecentActivity(0L, 0L, Collections.emptyList(), Collections.emptyList()))
                    .purchaseOrderSummary(new PurchaseOrderSummary(0L, 0L, 0L, Collections.emptyList()))
                    .salesMetrics(new SalesMetrics(0.0, 0.0, 0))
                    .aiInsights(new AIInsights(Collections.emptyList(), 0.0, 0L))
                    .build();
        }
    }

    public InventorySummary getInventorySummary() {
        long totalProductsPrimitive = productRepository.count();
        Long totalProducts = Long.valueOf(totalProductsPrimitive);

        long lowStockCount = safeCollectionSize(productRepository.findLowStockProducts());
        long outOfStockCount = safeCollectionSize(productRepository.findOutOfStockProducts());

        Double totalValue = productRepository.calculateTotalInventoryValue();
        if (totalValue == null) totalValue = 0.0;

        return new InventorySummary(totalProducts, lowStockCount, outOfStockCount, totalValue);
    }

    /**
     * Alert analytics (counts by type/priority, recent unread)
     */
    public Map<String, Object> getAlertAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        try {
            long totalAlertsPrimitive = alertRepository.count();
            Long totalAlerts = Long.valueOf(totalAlertsPrimitive);
            long unreadAlertsPrimitive = alertRepository.countByIsReadFalse();
            Long unreadAlerts = Long.valueOf(unreadAlertsPrimitive);

            // Count by type (may return empty list)
            Map<String, Long> alertsByType = new HashMap<>();
            List<Object[]> typeCounts = alertRepository.countUnreadAlertsByType();
            if (typeCounts != null) {
                for (Object[] row : typeCounts) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        alertsByType.put(row[0].toString(), ((Number) row[1]).longValue());
                    }
                }
            }

            // Count by priority
            Map<String, Long> alertsByPriority = new HashMap<>();
            List<Object[]> priorityCounts = alertRepository.countUnreadAlertsByPriority();
            if (priorityCounts != null) {
                for (Object[] row : priorityCounts) {
                    if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                        alertsByPriority.put(row[0].toString(), ((Number) row[1]).longValue());
                    }
                }
            }

            // Recent unread alerts in last 7 days (repository expects LocalDateTime)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Long recentUnread = alertRepository.countRecentUnreadAlerts(sevenDaysAgo);
            if (recentUnread == null) recentUnread = 0L;

            analytics.put("totalAlerts", totalAlerts);
            analytics.put("unreadAlerts", unreadAlerts);
            analytics.put("alertsByType", alertsByType);
            analytics.put("alertsByPriority", alertsByPriority);
            analytics.put("recentAlerts", recentUnread);

        } catch (Exception e) {
            log.error("Error computing alert analytics: {}", e.getMessage(), e);
            analytics.put("totalAlerts", 0L);
            analytics.put("unreadAlerts", 0L);
            analytics.put("alertsByType", Collections.emptyMap());
            analytics.put("alertsByPriority", Collections.emptyMap());
            analytics.put("recentAlerts", 0L);
        }
        return analytics;
    }

    public StockAlerts getStockAlerts() {
        try {
            Long totalAlerts = Long.valueOf(alertRepository.countByIsReadFalse());
            Long lowStockAlerts = Long.valueOf(alertRepository.countByTypeAndIsReadFalse(Alert.AlertType.LOW_STOCK));
            Long predictedAlerts = Long.valueOf(alertRepository.countByTypeAndIsReadFalse(Alert.AlertType.PREDICTED_STOCKOUT));
            List<Alert> critical = alertRepository.findCriticalAlerts();
            if (critical == null) critical = Collections.emptyList();

            return new StockAlerts(totalAlerts, lowStockAlerts, predictedAlerts, critical);
        } catch (Exception e) {
            log.error("Failed to fetch stock alerts: {}", e.getMessage(), e);
            return new StockAlerts(0L, 0L, 0L, Collections.emptyList());
        }
    }

    public RecentActivity getRecentActivity() {
        try {
            // Start of today (LocalDate -> LocalDateTime) and end (exclusive)
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

            Long todayIns = stockTransactionRepository.countTodayStockIns(startOfToday, startOfTomorrow);
            Long todayOuts = stockTransactionRepository.countTodayStockOuts(startOfToday, startOfTomorrow);
            if (todayIns == null) todayIns = 0L;
            if (todayOuts == null) todayOuts = 0L;

            // Recent transactions (native method expects limit)
            List<StockTransaction> recentTransactions = stockTransactionRepository.findRecentTransactions(10);
            if (recentTransactions == null) recentTransactions = Collections.emptyList();

            List<PurchaseOrder> recentPOs = purchaseOrderRepository.findRecentPendingOrders();
            if (recentPOs == null) recentPOs = Collections.emptyList();

            return new RecentActivity(todayIns, todayOuts, recentTransactions, recentPOs);
        } catch (Exception e) {
            log.error("Error while getting recent activity: {}", e.getMessage(), e);
            return new RecentActivity(0L, 0L, Collections.emptyList(), Collections.emptyList());
        }
    }

    public PurchaseOrderSummary getPurchaseOrderSummary() {
        try {
            Long total = Long.valueOf(purchaseOrderRepository.count());
            Long pending = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.PENDING);
            Long approved = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.APPROVED);

            // Repository expects LocalDate, so send LocalDate
            LocalDate today = LocalDate.now();
            LocalDate nextWeek = today.plusDays(7);

            List<PurchaseOrder> upcomingDeliveries =
                    purchaseOrderRepository.findUpcomingDeliveries(today, nextWeek);

            if (upcomingDeliveries == null) upcomingDeliveries = Collections.emptyList();

            return new PurchaseOrderSummary(total, pending, approved, upcomingDeliveries);

        } catch (Exception e) {
            log.error("Error building purchase order summary: {}", e.getMessage(), e);
            return new PurchaseOrderSummary(0L, 0L, 0L, Collections.emptyList());
        }
    }



    public SalesMetrics getSalesMetrics() {
        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(30);

            List<Object[]> monthSales = stockTransactionRepository.findSalesBetweenDates(start, end);
            if (monthSales == null) monthSales = Collections.emptyList();
            Double currentTotal = calculateSales(monthSales);

            LocalDateTime prevStart = end.minusDays(60);
            LocalDateTime prevEnd = end.minusDays(30);

            List<Object[]> prevSales = stockTransactionRepository.findSalesBetweenDates(prevStart, prevEnd);
            if (prevSales == null) prevSales = Collections.emptyList();
            Double prevTotal = calculateSales(prevSales);

            Double growth = (prevTotal != null && prevTotal > 0)
                    ? ((currentTotal - prevTotal) / prevTotal) * 100.0
                    : 0.0;

            int transactionCount = monthSales.size();

            return new SalesMetrics(currentTotal, growth, transactionCount);
        } catch (Exception e) {
            log.error("Error computing sales metrics: {}", e.getMessage(), e);
            return new SalesMetrics(0.0, 0.0, 0);
        }
    }

    private Double calculateSales(List<Object[]> list) {
        if (list == null || list.isEmpty()) return 0.0;
        return list.stream()
                .mapToDouble(o -> {
                    // defensive: some rows may be [productId, sumQty]
                    try {
                        Number qty = (Number) o[1];
                        double avgPrice = 10.0; // fallback mock average price
                        return qty.doubleValue() * avgPrice;
                    } catch (Exception ex) {
                        return 0.0;
                    }
                })
                .sum();
    }

    public AIInsights getAIInsights() {
        try {
            Double accuracy = forecastRepository.findAverageAccuracy();
            if (accuracy == null) accuracy = 0.0;
            Long aiAlerts = Long.valueOf(alertRepository.countByTypeAndIsReadFalse(Alert.AlertType.PREDICTED_STOCKOUT));
            // restock suggestions left empty for now - integrate ForecastService later
            return new AIInsights(Collections.emptyList(), accuracy, aiAlerts);
        } catch (Exception e) {
            log.error("Error computing AI insights: {}", e.getMessage(), e);
            return new AIInsights(Collections.emptyList(), 0.0, 0L);
        }
    }

    // ---------- Helpers ----------
    private static int safeCollectionSize(Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    // ---------- DTOs ----------
    @Data @Builder
    public static class DashboardData {
        private InventorySummary inventorySummary;
        private StockAlerts stockAlerts;
        private RecentActivity recentActivity;
        private PurchaseOrderSummary purchaseOrderSummary;
        private SalesMetrics salesMetrics;
        private AIInsights aiInsights;
    }

    @Data @Builder
    public static class InventorySummary {
        private Long totalProducts;
        private Long lowStockProducts;
        private Long outOfStockProducts;
        private Double totalInventoryValue;

        public InventorySummary(Long totalProducts, Long lowStockProducts, Long outOfStockProducts, Double totalInventoryValue) {
            this.totalProducts = totalProducts;
            this.lowStockProducts = lowStockProducts;
            this.outOfStockProducts = outOfStockProducts;
            this.totalInventoryValue = totalInventoryValue;
        }
    }

    @Data @Builder
    public static class StockAlerts {
        private Long totalAlerts;
        private Long lowStockAlerts;
        private Long predictedStockoutAlerts;
        private List<Alert> criticalAlerts;

        public StockAlerts(Long totalAlerts, Long lowStockAlerts, Long predictedStockoutAlerts, List<Alert> criticalAlerts) {
            this.totalAlerts = totalAlerts;
            this.lowStockAlerts = lowStockAlerts;
            this.predictedStockoutAlerts = predictedStockoutAlerts;
            this.criticalAlerts = criticalAlerts;
        }
    }

    @Data @Builder
    public static class RecentActivity {
        private Long recentStockIns;
        private Long recentStockOuts;
        private List<StockTransaction> recentTransactions;
        private List<PurchaseOrder> recentPurchaseOrders;

        public RecentActivity(Long recentStockIns, Long recentStockOuts, List<StockTransaction> recentTransactions, List<PurchaseOrder> recentPurchaseOrders) {
            this.recentStockIns = recentStockIns;
            this.recentStockOuts = recentStockOuts;
            this.recentTransactions = recentTransactions;
            this.recentPurchaseOrders = recentPurchaseOrders;
        }
    }

    @Data @Builder
    public static class PurchaseOrderSummary {
        private Long totalPOs;
        private Long pendingPOs;
        private Long approvedPOs;
        private List<PurchaseOrder> upcomingDeliveries;

        public PurchaseOrderSummary(Long totalPOs, Long pendingPOs, Long approvedPOs, List<PurchaseOrder> upcomingDeliveries) {
            this.totalPOs = totalPOs;
            this.pendingPOs = pendingPOs;
            this.approvedPOs = approvedPOs;
            this.upcomingDeliveries = upcomingDeliveries;
        }
    }

    @Data @Builder
    public static class SalesMetrics {
        private Double totalSalesValue;
        private Double salesGrowth;
        private Integer transactionsCount;

        public SalesMetrics(Double totalSalesValue, Double salesGrowth, Integer transactionsCount) {
            this.totalSalesValue = totalSalesValue;
            this.salesGrowth = salesGrowth;
            this.transactionsCount = transactionsCount;
        }
    }

    @Data @Builder
    public static class AIInsights {
        private List<RestockSuggestion> restockSuggestions;
        private Double forecastAccuracy;
        private Long aiGeneratedAlerts;

        public AIInsights(List<RestockSuggestion> restockSuggestions, Double forecastAccuracy, Long aiGeneratedAlerts) {
            this.restockSuggestions = restockSuggestions;
            this.forecastAccuracy = forecastAccuracy;
            this.aiGeneratedAlerts = aiGeneratedAlerts;
        }
    }
}
