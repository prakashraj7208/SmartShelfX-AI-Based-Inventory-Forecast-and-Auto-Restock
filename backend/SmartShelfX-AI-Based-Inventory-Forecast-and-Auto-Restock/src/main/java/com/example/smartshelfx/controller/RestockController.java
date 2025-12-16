//package com.example.smartshelfx.controller;
//
//import com.example.smartshelfx.dto.RestockSuggestionDto;
//import com.example.smartshelfx.service.RestockService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/restock")
//public class RestockController {
//
//    private final RestockService restockService;
//
//    public RestockController(RestockService restockService) {
//        this.restockService = restockService;
//    }
//
//    /**
//     * GET /api/restock/suggestions?horizonDays=14&historyDays=90&onlyRisky=true
//     */
//    @GetMapping("/suggestions")
//    public ResponseEntity<?> getRestockSuggestions(
//            @RequestParam(required = false) Integer horizonDays,
//            @RequestParam(required = false) Integer historyDays,
//            @RequestParam(required = false) Boolean onlyRisky
//    ) {
//        try {
//            List<RestockSuggestionDto> suggestions =
//                    restockService.generateSuggestions(horizonDays, historyDays, onlyRisky);
//
//            return ResponseEntity.ok(suggestions);
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(
//                    "Error generating restock suggestions: " + e.getMessage()
//            );
//        }
//    }
//}
