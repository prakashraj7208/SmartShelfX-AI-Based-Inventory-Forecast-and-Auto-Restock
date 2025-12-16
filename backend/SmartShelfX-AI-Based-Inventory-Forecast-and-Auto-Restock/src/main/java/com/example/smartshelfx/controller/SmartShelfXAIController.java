package com.example.smartshelfx.controller;

import com.example.smartshelfx.ai.SmartShelfXAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class SmartShelfXAIController {

    private final SmartShelfXAIService ai;

    public SmartShelfXAIController(SmartShelfXAIService ai) {
        this.ai = ai;
    }

    @GetMapping("/forecast")
    public String forecast(@RequestParam String product,
                           @RequestParam int stock,
                           @RequestParam int sales) {
        return ai.forecastDemand(product, stock, sales);
    }

    @GetMapping("/stockout")
    public String stockout(@RequestParam String product,
                           @RequestParam int stock,
                           @RequestParam int dailyAvg) {
        return ai.stockoutPrediction(product, stock, dailyAvg);
    }

    @GetMapping("/restock")
    public String restock(@RequestParam String product,
                          @RequestParam int stock,
                          @RequestParam int forecast) {
        return ai.suggestRestock(product, stock, forecast);
    }

    @GetMapping("/po")
    public String po(@RequestParam String product,
                     @RequestParam int qty,
                     @RequestParam String vendor) {
        return ai.generatePO(product, qty, vendor);
    }

    @PostMapping("/analytics")
    public String analytics(@RequestBody String inventoryJson) {
        return ai.analyticsSummary(inventoryJson);
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return ai.chat(message);
    }
    @GetMapping("/analytics")
    public ResponseEntity<String> getAnalytics() {
        try {
            // Fetch full inventory data from DB
            String inventoryJson = ai.buildCompressedInventorySummary();

            // Pass it to the AI engine
            String result = ai.analyticsSummary(inventoryJson);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

}
