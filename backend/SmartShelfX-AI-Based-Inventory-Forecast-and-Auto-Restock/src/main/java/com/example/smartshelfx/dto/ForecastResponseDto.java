package com.example.smartshelfx.dto;

import lombok.Data;
import java.util.List;

@Data
public class ForecastResponseDto {
    private String sku;
    private List<DailyForecastDto> forecast;
    private Integer totalDemand;
    private Integer currentStock;
    private Boolean riskOfStockout;
    private Integer recommendedOrder;
    private String reasoning;
}
