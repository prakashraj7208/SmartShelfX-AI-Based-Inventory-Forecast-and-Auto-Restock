//package com.example.smartshelfx.service;
//
//import com.example.smartshelfx.model.Product;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//public class ForecastService {
//
//    private final OpenAiClient ai;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public ForecastService(OpenAiClient ai) {
//        this.ai = ai;
//    }
//
//    /**
//     * MAIN FORECAST FUNCTION
//     */
//    public Map<String, Object> generateForecast(Product product) {
//
//        try {
//            String prompt = """
//                    You are SmartShelfX AI.
//                    Generate a JSON ONLY response for demand forecasting.
//
//                    PRODUCT DETAILS:
//                    - Name: %s
//                    - SKU: %s
//                    - Current Stock: %d
//
//                    JSON FORMAT (STRICT):
//                    {
//                      "sku": "string",
//                      "forecast": [int, int, int, int, int, int, int],
//                      "totalDemand": int,
//                      "currentStock": int,
//                      "riskOfStockout": boolean,
//                      "recommendedOrder": int,
//                      "reasoning": "string"
//                    }
//
//                    RULES:
//                    - Return ONLY JSON.
//                    - Do NOT include extra explanation.
//                    - forecast[] must contain exactly 7 integers (next 7 days).
//                    """.formatted(
//                    product.getName(),
//                    product.getSku(),
//                    product.getCurrentStock()
//            );
//
//            // Call AI
//            String aiRaw = ai.chat(prompt);
//            System.out.println("🟦 AI RAW RESPONSE: " + aiRaw);
//
//            // Extract only the JSON content
//            String cleaned = extractJson(aiRaw);
//
//            // Convert to Map
//            return objectMapper.readValue(cleaned, Map.class);
//
//        } catch (Exception e) {
//            System.out.println("🔥 FORECAST ERROR: " + e.getMessage());
//            return fallback(product);
//        }
//    }
//
//    /**
//     * Extract JSON from an OpenAI response safely.
//     */
//    private String extractJson(String response) {
//        if (response == null) return "{}";
//
//        int start = response.indexOf("{");
//        int end = response.lastIndexOf("}");
//
//        if (start >= 0 && end >= 0) {
//            return response.substring(start, end + 1);
//        }
//        return "{}";
//    }
//
//    /**
//     * SAFE fallback if AI fails.
//     */
//    private Map<String, Object> fallback(Product product) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("sku", product.getSku());
//        map.put("forecast", new int[]{0, 0, 0, 0, 0, 0, 0});
//        map.put("totalDemand", 0);
//        map.put("currentStock", product.getCurrentStock());
//        map.put("riskOfStockout", false);
//        map.put("recommendedOrder", 0);
//        map.put("reasoning", "AI unavailable. Fallback forecast applied.");
//        return map;
//    }
//}
