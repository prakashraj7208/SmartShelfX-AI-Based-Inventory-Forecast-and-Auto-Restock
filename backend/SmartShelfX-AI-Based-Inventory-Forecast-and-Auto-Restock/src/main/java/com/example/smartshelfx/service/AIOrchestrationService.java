package com.example.smartshelfx.service;

import com.example.smartshelfx.ai.SmartShelfXAIService;
import com.example.smartshelfx.ai.dto.AiForecastDecisionResult;
import com.example.smartshelfx.model.*;
import com.example.smartshelfx.repository.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIOrchestrationService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ForecastRepository forecastRepository;
    private final AlertRepository alertRepository;
    private final SmartShelfXAIService smartShelfXAIService;

    @Transactional
    public InventoryAiResponse forecastAndMaybeReorder(Long productId, boolean autoCreatePo) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // 1) Recent sales (last 30 days)
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        List<StockTransaction> recentTransactions =
                stockTransactionRepository.findByProductIdOrderByTimestampDesc(productId)
                        .stream()
                        .filter(t -> t.getTimestamp() != null &&
                                !t.getTimestamp().isBefore(start) &&
                                !t.getTimestamp().isAfter(end))
                        .toList();

        // 2) Active POs (PENDING or APPROVED)
        List<PurchaseOrder> activePos =
                purchaseOrderRepository.findByProductIdOrderByCreatedAtDesc(productId)
                        .stream()
                        .filter(po -> po.getStatus() == PurchaseOrder.OrderStatus.PENDING
                                || po.getStatus() == PurchaseOrder.OrderStatus.APPROVED
                                || po.getStatus() == PurchaseOrder.OrderStatus.ORDERED)
                        .toList();

        int forecastPeriodDays = 14; // 2 weeks

        // 3) Call AI
        AiForecastDecisionResult aiResult = smartShelfXAIService.analyzeProductInventory(
                product,
                recentTransactions,
                activePos,
                forecastPeriodDays
        );

        // 4) Save forecast
        Forecast forecast = new Forecast();
        forecast.setProduct(product);
        forecast.setForecastDate(LocalDate.now());
        forecast.setPredictedDemand(aiResult.getExpectedDemand());
        forecast.setForecastPeriodDays(aiResult.getForecastPeriodDays());
        forecast.setAlgorithmUsed("AI_OPENROUTER_DEEPSEEK");
        forecast.setCreatedAt(LocalDateTime.now());
        forecastRepository.save(forecast);

        // 5) Create alert based on decision / risk
        Alert alert = null;
        if ("ORDER_NOW".equalsIgnoreCase(aiResult.getDecision())) {
            alert = Alert.createPredictedStockoutAlert(
                    product,
                    aiResult.getExpectedDemand() != null ? aiResult.getExpectedDemand() : 0,
                    product.getCurrentStock() != null ? product.getCurrentStock() : 0
            );
            alert.setSuggestedAction("AI recommends ordering "
                    + aiResult.getRecommendedOrderQty()
                    + " units now. " + aiResult.getExplanation());
            alert.setSource("AI");

            // risk ↔ priority mapping
            Alert.Priority priority = switch (
                    aiResult.getRiskLevel() != null ? aiResult.getRiskLevel().toUpperCase() : ""
                    ) {
                case "CRITICAL" -> Alert.Priority.CRITICAL;
                case "HIGH" -> Alert.Priority.HIGH;
                case "LOW" -> Alert.Priority.LOW;
                default -> Alert.Priority.MEDIUM;
            };
            alert.setPriority(priority);

            alertRepository.save(alert);
        }

        // 6) Optionally create Purchase Order
        PurchaseOrder po = null;
        if (autoCreatePo && "ORDER_NOW".equalsIgnoreCase(aiResult.getDecision())
                && aiResult.getRecommendedOrderQty() != null
                && aiResult.getRecommendedOrderQty() > 0) {

            po = new PurchaseOrder();
            po.setPoNumber("PO-" + System.currentTimeMillis());
            po.setProduct(product);
            po.setVendor(product.getVendor()); // may be null if no vendor assigned
            po.setQuantity(aiResult.getRecommendedOrderQty());

            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            po.setUnitPrice(unitPrice);
            po.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(po.getQuantity())));

            po.setStatus(PurchaseOrder.OrderStatus.PENDING);
            po.setCreatedAt(LocalDateTime.now());
            po.setUpdatedAt(LocalDateTime.now());

            if (product.getLeadTimeDays() != null) {
                po.setExpectedDelivery(LocalDate.now().plusDays(product.getLeadTimeDays()));
            }

            purchaseOrderRepository.save(po);
        }

        // 7) Build response for frontend
        InventoryAiResponse response = new InventoryAiResponse();
        response.setProductId(product.getId());
        response.setProductName(product.getName());
        response.setSku(product.getSku());
        response.setCurrentStock(product.getCurrentStock());
        response.setReorderLevel(product.getReorderLevel());
        response.setSafetyStock(product.getSafetyStock());
        response.setLeadTimeDays(product.getLeadTimeDays());
        response.setVendorName(product.getVendorName());

        response.setAi(aiResult);

        if (forecast != null) {
            response.setForecastId(forecast.getId());
            response.setForecastDate(forecast.getForecastDate());
        }

        if (alert != null) {
            response.setAlertId(alert.getId());
            response.setAlertTitle(alert.getTitle());
            response.setAlertPriority(alert.getPriority().name());
        }

        if (po != null) {
            response.setPoId(po.getId());
            response.setPoNumber(po.getPoNumber());
            response.setPoStatus(po.getStatus().name());
        }

        return response;
    }

    // DTO for frontend JSON
    @Data
    @AllArgsConstructor
    public static class InventoryAiResponse {
        public InventoryAiResponse() {}

        private Long productId;
        private String productName;
        private String sku;
        private Integer currentStock;
        private Integer reorderLevel;
        private Integer safetyStock;
        private Integer leadTimeDays;
        private String vendorName;

        private Long forecastId;
        private LocalDate forecastDate;

        private Long alertId;
        private String alertTitle;
        private String alertPriority;

        private Long poId;
        private String poNumber;
        private String poStatus;

        private AiForecastDecisionResult ai;
    }
}
