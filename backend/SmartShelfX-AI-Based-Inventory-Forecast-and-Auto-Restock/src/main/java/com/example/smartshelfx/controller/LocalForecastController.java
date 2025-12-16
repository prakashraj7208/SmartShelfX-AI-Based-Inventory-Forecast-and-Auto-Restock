package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.model.Product;
import com.example.smartshelfx.service.LocalForecastService;
import com.example.smartshelfx.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/local-forecast")
@RequiredArgsConstructor
@Slf4j
public class LocalForecastController {

    private final LocalForecastService localForecastService;
    private final ProductService productService;

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Map<String, Object>> getLocalForecast(@PathVariable Long productId) {
        try {
            Map<String, Object> forecast = localForecastService.generateForecast(productId);
            return ApiResponse.success("Local forecast generated", forecast);
        } catch (Exception e) {
            return ApiResponse.error("Local forecasting failed: " + e.getMessage(), null);
        }
    }

    @GetMapping("/restock-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<List<Map<String, Object>>> getRestockSuggestions() {
        try {
            List<Product> products = productService.getAllProducts();
            List<Map<String, Object>> suggestions = new ArrayList<>();

            for (Product p : products) {
                Map<String, Object> fc = localForecastService.generateForecast(p.getId());

                if (!fc.containsKey("next7DaysPrediction")) continue;

                double expected = (double) fc.get("next7DaysPrediction");
                int currentStock = p.getCurrentStock();

                double deficit = expected - currentStock;

                if (deficit > 0) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("productId", p.getId());
                    row.put("productName", p.getName());
                    row.put("currentStock", currentStock);
                    row.put("forecast7DayDemand", Math.round(expected));
                    row.put("suggestedReorderQty", Math.round(deficit));
                    row.put("urgency", currentStock < p.getReorderLevel() ? "HIGH" : "MEDIUM");

                    suggestions.add(row);
                }
            }

            return ApiResponse.success("Local restock suggestions generated", suggestions);

        } catch (Exception e) {
            return ApiResponse.error("Failed to generate local suggestions: " + e.getMessage(), null);
        }
    }
}
