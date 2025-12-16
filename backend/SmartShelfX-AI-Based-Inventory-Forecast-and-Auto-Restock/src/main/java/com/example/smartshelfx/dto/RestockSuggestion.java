package com.example.smartshelfx.dto;

import com.example.smartshelfx.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockSuggestion {
    private Long productId;
    private String productName;
    private String sku;
    private Integer currentStock;
    private Integer reorderLevel;
    private Integer predictedDemand;
    private Integer suggestedQuantity;
    private String urgency; // HIGH, MEDIUM, LOW
    private User vendor;
    private BigDecimal estimatedCost;
    private Integer daysUntilStockout;
    private String recommendationReason;
}