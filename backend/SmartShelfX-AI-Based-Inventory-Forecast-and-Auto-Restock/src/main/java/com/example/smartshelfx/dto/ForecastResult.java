package com.example.smartshelfx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastResult {
    private Long productId;
    private String productName;
    private String sku;
    private Integer currentStock;
    private Integer predictedDemand;
    private Double confidenceScore;
    private LocalDate forecastDate;
    private String recommendedAction;
    private Integer suggestedOrderQuantity;
    private Integer daysOfSupply;
    private String riskLevel;
}