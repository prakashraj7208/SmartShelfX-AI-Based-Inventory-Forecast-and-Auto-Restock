//package com.example.smartshelfx.controller;
//
//import com.example.smartshelfx.dto.ForecastingRequest;
//import com.example.smartshelfx.dto.ForecastingResponse;
//import com.example.smartshelfx.service.AIService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/")
//@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
//public class AIController {
//
//    @Autowired
//    private AIService aiService;
//
//    // -------------------------------------------------------------------------
//    // 1️⃣ USER REQUEST → DEMAND FORECAST
//    // -------------------------------------------------------------------------
//    @PostMapping("/forecast")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
//    public ResponseEntity<ForecastingResponse> getDemandForecast(
//            @RequestBody ForecastingRequest request
//    ) {
//        try {
//            ForecastingResponse response = aiService.getDemandForecast(request);
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            ForecastingResponse error = new ForecastingResponse();
//            error.setMessage("Error generating forecast: " + e.getMessage());
//            return ResponseEntity.internalServerError().body(error);
//        }
//    }
//
//    // -------------------------------------------------------------------------
//    // 2️⃣ RESTOCK RECOMMENDATION API
//    // -------------------------------------------------------------------------
//    @GetMapping("/recommendations")
//    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
//    public ResponseEntity<?> getRestockRecommendations() {
//        try {
//            return ResponseEntity.ok(aiService.getRestockRecommendations());
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError()
//                    .body(Map.of("error", "Failed to get recommendations: " + e.getMessage()));
//        }
//    }
//
//
//
//    // -------------------------------------------------------------------------
//    // 3️⃣ AI HEALTH CHECK
//    // -------------------------------------------------------------------------
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, String>> checkAIHealth() {
//        try {
//            // Simple test using dummy request
//            ForecastingRequest req = new ForecastingRequest();
//            req.setProductSku("TEST");
//            req.setPeriod("daily");
//            req.setStartDate(java.time.LocalDate.now().minusDays(7));
//            req.setEndDate(java.time.LocalDate.now());
//
//            aiService.getDemandForecast(req);
//
//            return ResponseEntity.ok(Map.of(
//                    "status", "UP",
//                    "message", "AI service reachable"
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.ok(Map.of(
//                    "status", "DOWN",
//                    "message", "AI service offline → using fallback mode"
//            ));
//        }
//    }
//}