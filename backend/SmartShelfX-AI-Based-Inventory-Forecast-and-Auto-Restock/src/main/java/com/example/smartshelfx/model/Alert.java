package com.example.smartshelfx.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(length = 500)
    private String actionUrl;

    // 🆕 AI-POWERED ALERT FIELDS
    @Column(length = 200)
    private String title;              // Alert title

    @Column(length = 1000)
    private String description;        // Detailed description

    @Column(length = 500)
    private String suggestedAction;    // AI suggested action

    private Double predictedShortfall; // AI predicted shortfall

    // 🆕 ADDITIONAL ENHANCEMENTS
    @Column(length = 50)
    private String source = "SYSTEM";  // SYSTEM, AI, MANUAL

    private LocalDateTime resolvedAt;  // When alert was resolved

    @Column(length = 1000)
    private String resolutionNotes;    // How was it resolved

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
        if (source == null) {
            source = "SYSTEM";
        }
    }

    // 🆕 HELPER METHODS
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !isRead && !isExpired();
    }

    public void markAsResolved(String notes) {
        this.isRead = true;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    // 🎯 ALERT TYPES
    public enum AlertType {
        // Stock-related alerts
        LOW_STOCK,
        OUT_OF_STOCK,
        RESTOCK_SUGGESTION,
        STOCKOUT_IMMINENT,
        PREDICTED_STOCKOUT,

        // AI-powered alerts
        AI_SUGGESTION,
        DEMAND_FORECAST,

        // Product-related alerts
        EXPIRY_WARNING,
        EXPIRED_PRODUCT,
        QUALITY_ISSUE,

        // Purchase & vendor alerts
        PURCHASE_ORDER_UPDATE,
        VENDOR_ISSUE,
        DELIVERY_DELAY,

        // System alerts
        SYSTEM_WARNING,
        INTEGRATION_ISSUE
    }

    // 🎯 PRIORITY LEVELS
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL,
        URGENT
    }

    // 🆕 FACTORY METHODS FOR COMMON ALERTS
    public static Alert createLowStockAlert(Product product, int currentStock, int reorderLevel) {
        Alert alert = new Alert();
        alert.setProduct(product);
        alert.setType(AlertType.LOW_STOCK);
        alert.setTitle("📉 Low Stock: " + product.getName());
        alert.setMessage(String.format("%s is running low. Current stock: %d, Reorder level: %d",
                product.getName(), currentStock, reorderLevel));
        alert.setDescription(String.format("Product '%s' has only %d units left, which is below the reorder level of %d. Consider restocking soon.",
                product.getName(), currentStock, reorderLevel));
        alert.setPriority(Priority.MEDIUM);
        alert.setSuggestedAction("Check inventory and place purchase order");
        return alert;
    }

    public static Alert createPredictedStockoutAlert(Product product, double predictedDemand, int currentStock) {
        Alert alert = new Alert();
        alert.setProduct(product);
        alert.setType(AlertType.PREDICTED_STOCKOUT);
        alert.setTitle("🚨 AI Predicts Stockout: " + product.getName());
        alert.setMessage(String.format("AI predicts demand of %.1f units, but only %d in stock",
                predictedDemand, currentStock));
        alert.setDescription(String.format("Based on AI analysis, product '%s' is predicted to have demand of %.1f units in the near future, but only %d units are currently in stock. Stockout risk is high.",
                product.getName(), predictedDemand, currentStock));
        alert.setPriority(Priority.HIGH);
        alert.setSuggestedAction("Generate purchase order immediately");
        alert.setPredictedShortfall(predictedDemand - currentStock);
        alert.setSource("AI");
        return alert;
    }

    public static Alert createExpiryAlert(Product product, LocalDateTime expiryDate) {
        Alert alert = new Alert();
        alert.setProduct(product);
        alert.setType(AlertType.EXPIRY_WARNING);
        alert.setTitle("⚠️ Expiring Soon: " + product.getName());
        alert.setMessage(String.format("%s expires on %s", product.getName(), expiryDate));
        alert.setDescription(String.format("Product '%s' is approaching its expiry date on %s. Consider discounting or other sales strategies.",
                product.getName(), expiryDate));
        alert.setPriority(Priority.MEDIUM);
        alert.setSuggestedAction("Check expiry date and plan promotions");
        alert.setExpiresAt(expiryDate);
        return alert;
    }

    // 🆕 BUILDER PATTERN FOR FLEXIBLE ALERT CREATION
    public static AlertBuilder builder() {
        return new AlertBuilder();
    }

    public static class AlertBuilder {
        private Alert alert = new Alert();

        public AlertBuilder product(Product product) {
            alert.setProduct(product);
            return this;
        }

        public AlertBuilder type(AlertType type) {
            alert.setType(type);
            return this;
        }

        public AlertBuilder title(String title) {
            alert.setTitle(title);
            return this;
        }

        public AlertBuilder message(String message) {
            alert.setMessage(message);
            return this;
        }

        public AlertBuilder description(String description) {
            alert.setDescription(description);
            return this;
        }

        public AlertBuilder priority(Priority priority) {
            alert.setPriority(priority);
            return this;
        }

        public AlertBuilder suggestedAction(String suggestedAction) {
            alert.setSuggestedAction(suggestedAction);
            return this;
        }

        public AlertBuilder predictedShortfall(Double predictedShortfall) {
            alert.setPredictedShortfall(predictedShortfall);
            return this;
        }

        public AlertBuilder source(String source) {
            alert.setSource(source);
            return this;
        }

        public AlertBuilder expiresInDays(int days) {
            alert.setExpiresAt(LocalDateTime.now().plusDays(days));
            return this;
        }

        public Alert build() {
            // Set default message if not provided
            if (alert.getMessage() == null && alert.getTitle() != null) {
                alert.setMessage(alert.getTitle());
            }
            // Set default title if not provided
            if (alert.getTitle() == null && alert.getMessage() != null) {
                alert.setTitle(alert.getMessage().length() > 50 ?
                        alert.getMessage().substring(0, 47) + "..." : alert.getMessage());
            }
            return alert;
        }
    }
}