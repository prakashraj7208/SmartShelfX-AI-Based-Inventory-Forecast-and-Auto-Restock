package com.example.smartshelfx.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "SKU is required")
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "cost_price")
    private BigDecimal costPrice;

    @Column(name = "current_stock")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer currentStock = 0;

    @Column(name = "reorder_level")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel = 10;

    @Column(name = "safety_stock")
    @Min(value = 0, message = "Safety stock cannot be negative")
    private Integer safetyStock = 5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private User vendor;

    private String imageName;
    private String imageType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays = 7;

    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // In Product entity, add this to transactions field:
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("product")  // Add this line
    private List<StockTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<PurchaseOrder> purchaseOrders = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Forecast> forecasts = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Alert> alerts = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ FIXED: Added return statement
    public @Min(value = 0, message = "Stock cannot be negative") Integer getMinStock() {
        return this.safetyStock; // or return a calculated value
    }

    // ✅ ADDITIONAL HELPER METHODS
    public boolean isLowStock() {
        return this.currentStock <= this.reorderLevel;
    }

    public boolean isCriticalStock() {
        return this.currentStock <= this.safetyStock;
    }

    public Integer getStockDeficit() {
        return Math.max(0, this.reorderLevel - this.currentStock);
    }

    public Integer getSuggestedReorderQuantity() {
        // Suggest ordering enough to reach reorder level + safety buffer
        return Math.max(0, (this.reorderLevel + this.safetyStock) - this.currentStock);
    }

    // ✅ BUSINESS LOGIC METHODS
    public void increaseStock(Integer quantity) {
        if (quantity > 0) {
            this.currentStock += quantity;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void decreaseStock(Integer quantity) {
        if (quantity > 0 && this.currentStock >= quantity) {
            this.currentStock -= quantity;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public boolean canFulfillOrder(Integer requestedQuantity) {
        return this.currentStock >= requestedQuantity && Boolean.TRUE.equals(this.active);
    }

    // ✅ CALCULATION METHODS
    @JsonIgnore
    public BigDecimal getInventoryValue() {
        if (this.price == null || this.currentStock == null) {
            return BigDecimal.ZERO;
        }
        return this.price.multiply(BigDecimal.valueOf(this.currentStock));
    }


    public Double getStockToSalesRatio() {
        if (this.currentStock == 0) return 0.0;
        // This would typically use historical sales data
        return this.currentStock / (double) this.reorderLevel;
    }

    // ✅ STATUS METHODS
    public String getStockStatus() {
        if (this.currentStock <= this.safetyStock) {
            return "CRITICAL";
        } else if (this.currentStock <= this.reorderLevel) {
            return "LOW";
        } else {
            return "NORMAL";
        }
    }

    public String getStockStatusColor() {
        switch (getStockStatus()) {
            case "CRITICAL": return "red";
            case "LOW": return "orange";
            default: return "green";
        }
    }
    // ✅ ADD THIS METHOD TO FIX THE COMPILATION ERROR
    public String getVendorName() {
        if (this.vendor == null) {
            return "No Vendor Assigned";
        }
        return this.vendor.getFirstName() + " " + this.vendor.getLastName();
    }
    public Long vendorId(){
        if (this.vendor == null) {
            return null;
        }
        return this.vendor.getId();
    }
}