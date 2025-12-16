package com.example.smartshelfx.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiForecastDecisionResult {

    // "ORDER_NOW", "WAIT", "MONITOR"
    private String decision;

    // Forecast-related
    private Integer expectedDemand;              // e.g. forecast for period
    private Integer forecastPeriodDays;          // e.g. 7 or 14

    // Inventory recommendations
    private Integer recommendedReorderQuantity;  // how much to reorder now
    private Integer recommendedOrderQty;         // same as above or 0

    // Risk & explanation
    private String riskLevel;        // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private String explanation;      // plain-language explanation
    private String managerSummary;   // short summary for UI card
}
