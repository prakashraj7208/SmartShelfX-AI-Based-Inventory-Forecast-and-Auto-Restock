package com.example.smartshelfx.controller;

import com.example.smartshelfx.ai.SmartShelfXAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/v2")   // <— different from your old /api/ai paths
@RequiredArgsConstructor
public class SmartShelfXAIv2Controller {

    private final SmartShelfXAIService smartShelfXAIService;

    // 1) DEMAND FORECAST USING REAL DATA
    @GetMapping("/forecast/{productId}")
    public ResponseEntity<String> forecast(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "7") int horizonDays
    ) {
        String result = smartShelfXAIService.forecastForProduct(productId, horizonDays);
        return ResponseEntity.ok(result);
    }

    // 2) RESTOCK SUGGESTION USING REAL DATA
    @GetMapping("/restock/{productId}")
    public ResponseEntity<String> restock(@PathVariable Long productId) {
        String result = smartShelfXAIService.restockSuggestionForProduct(productId);
        return ResponseEntity.ok(result);
    }

    // 3) DASHBOARD / ANALYTICS OVERVIEW
    @GetMapping("/analytics/overview")
    public ResponseEntity<String> analyticsOverview() {
        String result = smartShelfXAIService.analyticsOverview();
        return ResponseEntity.ok(result);
    }
}
