package com.example.smartshelfx.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestockSuggestionDto {
    private Long productId;
    private String sku;
    private String name;
    private Integer currentStock;
    private Integer predictedDemand;
    private Boolean riskOfStockout;
    private Integer recommendedOrder;
    private String reason;
}
