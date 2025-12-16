package com.example.smartshelfx.controller;

import com.example.smartshelfx.service.AIOrchestrationService;
import com.example.smartshelfx.service.AIOrchestrationService.InventoryAiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/v2")
@RequiredArgsConstructor
public class SmartInventoryAIController {

    private final AIOrchestrationService aiOrchestrationService;

    /**
     * Full pipeline:
     * 1) Read DB
     * 2) Call AI
     * 3) Save Forecast
     * 4) Create Alert (if needed)
     * 5) Create PO (if autoCreatePo = true && ORDER_NOW)
     * 6) Return structured JSON for React
     */
    @PostMapping("/forecast-and-reorder/{productId}")
    public ResponseEntity<InventoryAiResponse> forecastAndReorder(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "true") boolean autoCreatePo
    ) {
        InventoryAiResponse response =
                aiOrchestrationService.forecastAndMaybeReorder(productId, autoCreatePo);
        return ResponseEntity.ok(response);
    }
}
