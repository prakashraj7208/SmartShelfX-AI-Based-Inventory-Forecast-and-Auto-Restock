package com.example.smartshelfx.ai;
import com.example.smartshelfx.ai.dto.AiForecastDecisionResult;
import com.example.smartshelfx.model.*;
import com.example.smartshelfx.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmartShelfXAIService {

    private final ChatModel chatModel;
    private static final Logger log = LoggerFactory.getLogger(SmartShelfXAIService.class);
    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final ForecastRepository forecastRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AlertRepository alertRepository;



//    public SmartShelfXAIService(ChatModel chatModel) {
//        this.chatModel = chatModel;
//    }

    private String askModel(String prompt) {
        ChatResponse response =chatModel.call(new Prompt(new UserMessage(prompt)));
        return response.getResult()
                .getOutput()
                .getText();
    }

    // 1️⃣ Demand Forecast
    public String forecastDemand(String product, int currentStock, int last30DaysSales) {
        String prompt = """
                You are SmartShelfX AI. Return ONLY JSON.
                Predict next 7-day demand for the product.

                Product: %s
                Current Stock: %d
                Last 30 Days Sales: %d

                Return strict JSON:
                {
                    "product": "...",
                    "forecast_next_7_days": 0,
                    "daily_breakdown": {},
                    "recommendation": "..."
                }
                """.formatted(product, currentStock, last30DaysSales);

        return askModel(prompt);
    }

    // 2️⃣ Stockout prediction
    public String stockoutPrediction(String product, int currentStock, int dailyAvg) {
        String prompt = """
                You are SmartShelfX AI. Return ONLY JSON.
                Predict stockout date.

                Product: %s
                Current Stock: %d
                Daily Avg Usage: %d

                JSON:
                {
                    "product": "...",
                    "estimated_stockout_days": 0,
                    "risk_level": "...",
                    "action": "..."
                }
                """.formatted(product, currentStock, dailyAvg);

        return askModel(prompt);
    }

    // 3️⃣ Restock suggestion
    public String suggestRestock(String product, int currentStock, int forecast) {
        String prompt = """
                You are SmartShelfX AI. Return ONLY JSON.
                Suggest restocking quantity.

                Product: %s
                Current Stock: %d
                Forecast Next Week: %d

                JSON:
                {
                    "product": "...",
                    "suggested_order_qty": 0,
                    "reason": "..."
                }
                """.formatted(product, currentStock, forecast);

        return askModel(prompt);
    }

    // 4️⃣ Purchase Order generation
    public String generatePO(String product, int qty, String vendor) {
        String prompt = """
                You are SmartShelfX AI. Return ONLY JSON.
                Create purchase order details.

                Product: %s
                Quantity: %d
                Vendor: %s

                JSON:
                {
                    "vendor": "...",
                    "product": "...",
                    "quantity": 0,
                    "priority": "...",
                    "notes": "..."
                }
                """.formatted(product, qty, vendor);

        return askModel(prompt);
    }

    // 5️⃣ Analytics
    public String analyticsSummary(String inventoryJson) {
        String prompt = """
                You are SmartShelfX AI. Return ONLY JSON.
                Analyze inventory data.

                Inventory JSON:
                %s

                JSON:
                {
                    "top_selling_items": [],
                    "low_stock_items": [],
                    "overall_health": "...",
                    "insights": "..."
                }
                """.formatted(inventoryJson);

        return askModel(prompt);
    }

    // 6️⃣ Free-form chat
    public String chat(String message) {
        return askModel(message);
    }

        // ----------------------------------------------------------------
        // 1) AI DEMAND FORECAST FOR A PRODUCT (USING REAL DB DATA)
        // ----------------------------------------------------------------
        public String forecastForProduct(Long productId, int horizonDays) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last90Days = now.minusDays(90);

            // Recent sales history (last 90 days, OUT transactions)
            List<StockTransaction> recentTx =
                    stockTransactionRepository.findRecentTransactionsByProduct(productId)
                            .stream()
                            .filter(tx -> tx.getType() == StockTransaction.TransactionType.OUT)
                            .filter(tx -> tx.getTimestamp().isAfter(last90Days))
                            .collect(Collectors.toList());

            int totalOutLast30 = recentTx.stream()
                    .filter(tx -> tx.getTimestamp().isAfter(now.minusDays(30)))
                    .mapToInt(StockTransaction::getQuantity)
                    .sum();

            int totalOutLast90 = recentTx.stream()
                    .mapToInt(StockTransaction::getQuantity)
                    .sum();

            // Last saved forecast (if any)
            List<Forecast> recentForecasts =
                    forecastRepository.findRecentForecasts(productId, LocalDate.now().minusDays(30));

            // Build context for AI
            StringBuilder context = new StringBuilder();
            context.append("You are an AI inventory planner.\n\n");
            context.append("Product info:\n");
            context.append(" - Name: ").append(product.getName()).append("\n");
            context.append(" - SKU: ").append(product.getSku()).append("\n");
            context.append(" - Category: ").append(product.getCategory()).append("\n");
            context.append(" - Current stock: ").append(product.getCurrentStock()).append("\n");
            context.append(" - Reorder level: ").append(product.getReorderLevel()).append("\n");
            context.append(" - Safety stock: ").append(product.getSafetyStock()).append("\n");
            context.append(" - Lead time (days): ").append(product.getLeadTimeDays()).append("\n\n");

            context.append("Recent sales (based on OUT stock transactions):\n");
            context.append(" - Total sales last 30 days: ").append(totalOutLast30).append("\n");
            context.append(" - Total sales last 90 days: ").append(totalOutLast90).append("\n");
            context.append(" - Number of sales transactions last 90 days: ").append(recentTx.size()).append("\n\n");

            if (!recentForecasts.isEmpty()) {
                Forecast latest = recentForecasts.stream()
                        .max(Comparator.comparing(Forecast::getForecastDate))
                        .orElse(recentForecasts.getFirst());

                context.append("Last saved forecast in system:\n");
                context.append(" - Forecast date: ").append(latest.getForecastDate()).append("\n");
                context.append(" - Predicted demand: ").append(latest.getPredictedDemand()).append("\n");
                if (latest.getAccuracy() != null) {
                    context.append(" - Historical accuracy: ").append(String.format("%.2f", latest.getAccuracy())).append("\n");
                }
                context.append("\n");
            }

            context.append("Task:\n");
            context.append("Given the above data, forecast demand for the next ")
                    .append(horizonDays)
                    .append(" days.\n");
            context.append("Explain your reasoning briefly and give:\n");
            context.append(" - expected_demand\n");
            context.append(" - recommended_reorder_quantity (consider safety stock, lead time, current stock)\n");
            context.append(" - short, manager-friendly explanation.\n");

            String prompt = context.toString();
            log.info("Sending forecast prompt to AI for product {} ({})", productId, product.getName());

            // Use the simple String call – this is what we know works in your project
            return chatModel.call(prompt);
        }

        // ----------------------------------------------------------------
        // 2) AI RESTOCK SUGGESTION FOR A PRODUCT
        // ----------------------------------------------------------------
        public String restockSuggestionForProduct(Long productId) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last60Days = now.minusDays(60);

            List<StockTransaction> recentTx =
                    stockTransactionRepository.findRecentTransactionsByProduct(productId)
                            .stream()
                            .filter(tx -> tx.getType() == StockTransaction.TransactionType.OUT)
                            .filter(tx -> tx.getTimestamp().isAfter(last60Days))
                            .collect(Collectors.toList());

            int totalOut60 = recentTx.stream().mapToInt(StockTransaction::getQuantity).sum();
            double avgDailySales = totalOut60 / 60.0;

            // Pending/active POs for this product
            List<PurchaseOrder> activePos =
                    purchaseOrderRepository.findByProductIdOrderByCreatedAtDesc(productId)
                            .stream()
                            .filter(po -> po.getStatus() == PurchaseOrder.OrderStatus.PENDING
                                    || po.getStatus() == PurchaseOrder.OrderStatus.APPROVED
                                    || po.getStatus() == PurchaseOrder.OrderStatus.ORDERED)
                            .toList();

            int onOrderQty = activePos.stream().mapToInt(PurchaseOrder::getQuantity).sum();

            StringBuilder context = new StringBuilder();
            context.append("You are an AI assistant helping with smart inventory restocking.\n\n");
            context.append("Product info:\n");
            context.append(" - Name: ").append(product.getName()).append("\n");
            context.append(" - SKU: ").append(product.getSku()).append("\n");
            context.append(" - Category: ").append(product.getCategory()).append("\n");
            context.append(" - Current stock: ").append(product.getCurrentStock()).append("\n");
            context.append(" - Reorder level: ").append(product.getReorderLevel()).append("\n");
            context.append(" - Safety stock: ").append(product.getSafetyStock()).append("\n");
            context.append(" - Suggested reorder qty (simple rule): ").append(product.getSuggestedReorderQuantity()).append("\n");
            context.append(" - Lead time (days): ").append(product.getLeadTimeDays()).append("\n\n");

            context.append("Sales info (from stock transactions):\n");
            context.append(" - Total sales last 60 days: ").append(totalOut60).append("\n");
            context.append(" - Approx. avg daily sales: ").append(String.format("%.2f", avgDailySales)).append("\n\n");

            context.append("Purchase orders in pipeline:\n");
            context.append(" - Total quantity already on order: ").append(onOrderQty).append("\n");
            context.append(" - Number of active POs: ").append(activePos.size()).append("\n\n");

            context.append("Task:\n");
            context.append("Based on this data, decide whether we should place a new purchase order now.\n");
            context.append("Return:\n");
            context.append(" - decision: \"ORDER_NOW\" or \"WAIT\"\n");
            context.append(" - recommended_order_qty\n");
            context.append(" - short explanation for a warehouse manager.\n");

            String prompt = context.toString();
            log.info("Sending restock prompt to AI for product {} ({})", productId, product.getName());

            return chatModel.call(prompt);
        }

        // ----------------------------------------------------------------
        // 3) AI-POWERED INVENTORY ANALYTICS OVERVIEW
        // ----------------------------------------------------------------
        public String analyticsOverview() {

            Double totalInventoryValue = productRepository.calculateTotalInventoryValue();
            if (totalInventoryValue == null) {
                totalInventoryValue = 0.0;
            }

            List<Product> lowStock = productRepository.findLowStockProducts();
            List<Product> criticalStock = productRepository.findCriticalStockProducts();
            List<Product> outOfStock = productRepository.findOutOfStockProducts();

            List<Alert> criticalAlerts = alertRepository.findCriticalAlerts();
            List<PurchaseOrder> activePOs = purchaseOrderRepository.findActivePurchaseOrders();
            Double avgForecastAccuracy = forecastRepository.findAverageAccuracy();

            StringBuilder context = new StringBuilder();
            context.append("You are an AI warehouse analytics assistant.\n\n");

            context.append("High-level KPIs:\n");
            context.append(" - Total inventory value (approx): ").append(String.format("%.2f", totalInventoryValue)).append("\n");
            context.append(" - Low stock products: ").append(lowStock.size()).append("\n");
            context.append(" - Critical stock products: ").append(criticalStock.size()).append("\n");
            context.append(" - Out of stock products: ").append(outOfStock.size()).append("\n");
            context.append(" - Active purchase orders (PENDING/APPROVED/ORDERED): ").append(activePOs.size()).append("\n");
            if (avgForecastAccuracy != null) {
                context.append(" - Avg forecast accuracy: ").append(String.format("%.2f", avgForecastAccuracy)).append("\n");
            }
            context.append("\n");

            context.append("Top 5 low stock products (name, stock, reorder level):\n");
            lowStock.stream()
                    .sorted(Comparator.comparing(Product::getCurrentStock))
                    .limit(5)
                    .forEach(p -> context.append(" * ")
                            .append(p.getName()).append(" | stock=")
                            .append(p.getCurrentStock()).append(", reorderLevel=")
                            .append(p.getReorderLevel()).append("\n"));

            context.append("\nCritical alerts (HIGH/CRITICAL/URGENT):\n");
            criticalAlerts.stream()
                    .limit(5)
                    .forEach(a -> context.append(" * ")
                            .append(a.getTitle() != null ? a.getTitle() : a.getMessage())
                            .append(" [").append(a.getPriority()).append("] for product ")
                            .append(a.getProduct() != null ? a.getProduct().getName() : "N/A")
                            .append("\n"));

            context.append("\nTask:\n");
            context.append("Generate a concise dashboard-style analysis for the warehouse manager.\n");
            context.append("Highlight:\n");
            context.append(" - current risk areas (stockouts, overstock, vendor delays if visible)\n");
            context.append(" - top 3 actions they should take today\n");
            context.append("Keep it short and actionable.\n");

            String prompt = context.toString();
            log.info("Sending analytics overview prompt to AI");

            return chatModel.call(prompt);
        }

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * New: Ask AI for a structured decision for a specific product.
     * We assume: recentTransactions are OUT (sales) mainly.
     */
    public AiForecastDecisionResult analyzeProductInventory(
            Product product,
            List<StockTransaction> recentTransactions,
            List<PurchaseOrder> activePurchaseOrders,
            int forecastPeriodDays
    ) {
        int stock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int reorderLevel = product.getReorderLevel() != null ? product.getReorderLevel() : 0;
        int safetyStock = product.getSafetyStock() != null ? product.getSafetyStock() : 0;
        int leadTimeDays = product.getLeadTimeDays() != null ? product.getLeadTimeDays() : 7;

        // Simple stats from transactions
        int totalSales = recentTransactions.stream()
                .filter(t -> t.getType() == StockTransaction.TransactionType.OUT)
                .mapToInt(t -> t.getQuantity() != null ? t.getQuantity() : 0)
                .sum();

        long daysLookback = 30; // you can adjust later
        double dailyAvgSales = daysLookback > 0 ? (totalSales / (double) daysLookback) : 0.0;

        int onOrderQty = activePurchaseOrders.stream()
                .mapToInt(po -> po.getQuantity() != null ? po.getQuantity() : 0)
                .sum();

        String prompt = """
            You are an AI inventory planner for a warehouse system called SmartShelfX.

            PRODUCT SNAPSHOT:
            - Name: %s
            - SKU: %s
            - Category: %s
            - Current stock: %d
            - Reorder level: %d
            - Safety stock: %d
            - Lead time (days): %d
            - Price: %s

            SALES SNAPSHOT (last %d days):
            - Total units sold: %d
            - Average daily sales: %.2f

            PURCHASE ORDERS PIPELINE:
            - Total quantity already on order: %d

            TASK:
            You must:
            1) Forecast demand for the next %d days.
            2) Decide if we should "ORDER_NOW", "WAIT", or "MONITOR".
            3) Suggest how many units to order now if needed.
            4) Assess risk level ("LOW", "MEDIUM", "HIGH", "CRITICAL").
            5) Provide a brief explanation and a manager-friendly summary.

            VERY IMPORTANT:
            - Respond ONLY in strict JSON.
            - Do NOT include markdown, comments, or extra text.
            - Use exactly this JSON schema:

            {
              "decision": "ORDER_NOW | WAIT | MONITOR",
              "expectedDemand": 0,
              "forecastPeriodDays": %d,
              "recommendedReorderQuantity": 0,
              "recommendedOrderQty": 0,
              "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
              "explanation": "string",
              "managerSummary": "string"
            }
            """.formatted(
                product.getName(),
                product.getSku(),
                product.getCategory(),
                stock,
                reorderLevel,
                safetyStock,
                leadTimeDays,
                product.getPrice(),
                daysLookback,
                totalSales,
                dailyAvgSales,
                onOrderQty,
                forecastPeriodDays,
                forecastPeriodDays
        );

        String rawJson = chatModel.call(prompt);

        try {
            return objectMapper.readValue(rawJson, AiForecastDecisionResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI JSON: " + rawJson, e);
        }
    }


    public String buildCompressedInventorySummary() {
        try {
            List<Product> products = productRepository.findAll();

            List<Map<String, Object>> productSummary = products.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", p.getName());
                map.put("sku", p.getSku());
                map.put("stock", p.getCurrentStock());
                map.put("reorderLevel", p.getReorderLevel());
                map.put("safetyStock", p.getSafetyStock());
                map.put("category", p.getCategory());
                return map;
            }).toList();

            int totalTransactions = stockTransactionRepository.countTransactions();
            int totalOut = stockTransactionRepository.countType(StockTransaction.TransactionType.valueOf("OUT"));
            int totalIn = stockTransactionRepository.countType(StockTransaction.TransactionType.valueOf("IN"));

            int activePOs = purchaseOrderRepository.countActivePOs();

            Map<String, Object> map = new HashMap<>();
            map.put("products", productSummary);
            map.put("totalTransactions", totalTransactions);
            map.put("totalIn", totalIn);
            map.put("totalOut", totalOut);
            map.put("activePOs", activePOs);

            return new ObjectMapper().writeValueAsString(map);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build compressed inventory summary: " + e.getMessage());
        }
    }



}
