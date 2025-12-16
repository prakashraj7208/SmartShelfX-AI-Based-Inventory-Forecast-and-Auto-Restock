//package com.example.smartshelfx.service;
//
//import com.example.smartshelfx.dto.ForecastingRequest;
//import com.example.smartshelfx.dto.ForecastingResponse;
//import com.example.smartshelfx.model.Product;
//import com.example.smartshelfx.repository.ProductRepository;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.HttpStatusCodeException;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
///**
// * Gemini-backed AIService.
// *
// * Uses Google Generative Language REST endpoint (text-bison-001) as a simple JSON-producing LLM.
// *
// * Notes:
// * - Set ai.gemini.apiKey in application.properties (or env)
// * - Optionally override ai.gemini.endpoint if you use different model / version.
// *
// * This class preserves the existing entry points used by controllers:
// *  - public ForecastingResponse getDemandForecast(ForecastingRequest req)
// *  - public Map<String,Object> getProductForecast(Long productId)
// *  - public List<Map<String,Object>> getRestockRecommendations()
// *
// * The service builds a deterministic prompt asking the model to output strict JSON to parse safely.
// */
//@Slf4j
//@Service
//public class AIService {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//    private final ProductRepository productRepository;
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    private final String geminiEndpoint;
//    private final String apiKey; // simple API key flow (append ?key=... to URL)
//    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//    public AIService(
//            ProductRepository productRepository,
//            @Value("${ai.gemini.apiKey:}") String apiKey,
//            @Value("${ai.gemini.endpoint:https://generativelanguage.googleapis.com/v1beta2/models/text-bison-001:generate}") String geminiEndpoint
//    ) {
//        this.productRepository = productRepository;
//        this.apiKey = apiKey;
//        this.geminiEndpoint = geminiEndpoint;
//    }
//
//    // ----------------------------------------------------------
//    // DEMAND FORECAST (Java → Gemini)
//    // ----------------------------------------------------------
//    public ForecastingResponse getDemandForecast(ForecastingRequest req) {
//        try {
//            Optional<Product> opt = productRepository.findBySku(req.getProductSku());
//            if (opt.isEmpty()) {
//                ForecastingResponse err = new ForecastingResponse();
//                err.setMessage("Product not found: " + req.getProductSku());
//                return err;
//            }
//            Product product = opt.get();
//
//            // Build history points from request or fallbacks
//            List<Map<String, Object>> historyList = new ArrayList<>();
//            if (req.getStartDate() != null && req.getEndDate() != null) {
//                // the controller side caller may supply dates; for safety we still create history fallback if missing
//                // but for simplicity we'll create a 7-day synthetic history if req.history not provided
//            }
//
//            // create fallback synthetic (7 days) if no history supplied via ForecastingRequest
//            // (Your ForecastingRequest DTO in Java uses LocalDate start/end; for legacy usage we create 7 day history)
//            for (int i = 7; i >= 1; i--) {
//                Map<String, Object> h = new HashMap<>();
//                h.put("date", LocalDate.now().minusDays(i).format(fmt));
//                h.put("quantity", Math.max(1, product.getCurrentStock() == null ? 1 : product.getCurrentStock()));
//                historyList.add(h);
//            }
//
//            // Build payload for Gemini prompt
//            String prompt = buildForecastPrompt(product.getSku(), product.getName(), historyList, 7);
//
//            Map<String, Object> geminiResp = callGemini(prompt, 0.0, 1); // deterministic
//
//            if (geminiResp == null) {
//                ForecastingResponse err = new ForecastingResponse();
//                err.setMessage("Empty response from Gemini AI");
//                return err;
//            }
//
//            // parse the content string (expected to be strict JSON)
//            String rawContent = extractGeminiContent(geminiResp);
//            Map<String, Object> parsed = tryParseJson(rawContent);
//
//            if (parsed == null || !parsed.containsKey("forecast")) {
//                // fallback: return mock/synthetic forecast based on last observed value
//                ForecastingResponse fallback = buildFallbackForecast(product);
//                fallback.setMessage("Gemini response invalid — fallback used");
//                return fallback;
//            }
//
//            @SuppressWarnings("unchecked")
//            List<Map<String, Object>> forecastList = (List<Map<String, Object>>) parsed.get("forecast");
//            double confidence = parsed.containsKey("confidence") ? Double.parseDouble(parsed.get("confidence").toString()) : 0.5;
//
//            Map<String, Double> predictions = new LinkedHashMap<>();
//            for (Map<String, Object> item : forecastList) {
//                String date = item.get("date").toString();
//                double d = Double.parseDouble(item.get("predictedDemand").toString());
//                predictions.put(date, d);
//            }
//
//            ForecastingResponse fr = new ForecastingResponse();
//            fr.setProductSku(product.getSku());
//            fr.setProductName(product.getName());
//            fr.setPredictions(predictions);
//            fr.setAccuracy(confidence);
//            return fr;
//
//        } catch (HttpStatusCodeException ex) {
//            log.error("Gemini forecasting failed: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
//            ForecastingResponse err = new ForecastingResponse();
//            err.setMessage("Gemini error: " + ex.getResponseBodyAsString());
//            return err;
//        } catch (Exception e) {
//            log.error("AIService getDemandForecast exception: {}", e.getMessage(), e);
//            ForecastingResponse err = new ForecastingResponse();
//            err.setMessage("AI exception: " + e.getMessage());
//            return err;
//        }
//    }
//
//    // ----------------------------------------------------------
//    // PRODUCT FORECAST (used by ForecastController)
//    // Returns the raw map (same shape as Python microservice did)
//    // ----------------------------------------------------------
//    public Map<String, Object> getProductForecast(Long productId) {
//        Optional<Product> opt = productRepository.findById(productId);
//        if (opt.isEmpty()) {
//            return Map.of("error", "Product not found");
//        }
//        Product product = opt.get();
//
//        // create 7-day synthetic history
//        List<Map<String, Object>> historyList = new ArrayList<>();
//        for (int i = 7; i >= 1; i--) {
//            historyList.add(Map.of(
//                    "date", LocalDate.now().minusDays(i).format(fmt),
//                    "quantity", Math.max(1, product.getCurrentStock() == null ? 1 : product.getCurrentStock())
//            ));
//        }
//
//        String prompt = buildForecastPrompt(product.getSku(), product.getName(), historyList, 7);
//        try {
//            Map<String, Object> geminiResp = callGemini(prompt, 0.0, 1);
//            if (geminiResp == null) return Map.of("error", "Empty Gemini response");
//
//            String raw = extractGeminiContent(geminiResp);
//            Map<String, Object> parsed = tryParseJson(raw);
//            if (parsed == null) {
//                // fallback format — produce same structure as previous python service
//                return Map.of(
//                        "productId", product.getId(),
//                        "forecast", List.of(),
//                        "confidence", 0.0,
//                        "model", "gemini-fallback"
//                );
//            }
//            // attach metadata if missing
//            parsed.putIfAbsent("productId", product.getId());
//            parsed.putIfAbsent("model", "gemini:text-bison-001");
//            return parsed;
//        } catch (Exception e) {
//            log.error("getProductForecast error: {}", e.getMessage(), e);
//            return Map.of("error", e.getMessage());
//        }
//    }
//
//    // ----------------------------------------------------------
//    // RESTOCK RECOMMENDATIONS (used by RecommendationsPage)
//    // ----------------------------------------------------------
//    public List<Map<String, Object>> getRestockRecommendations() {
//        List<Map<String, Object>> suggestions = new ArrayList<>();
//        try {
//            List<Product> products = productRepository.findAll();
//
//            for (Product product : products) {
//
//                // 1) Build history (7 days)
//                List<Map<String, Object>> historyList = new ArrayList<>();
//                for (int i = 7; i >= 1; i--) {
//                    historyList.add(Map.of(
//                            "date", LocalDate.now().minusDays(i).format(fmt),
//                            "quantity", Math.max(1, product.getCurrentStock() == null ? 1 : product.getCurrentStock())
//                    ));
//                }
//
//                // 2) Ask Gemini for forecast for this product
//                String prompt = buildForecastPrompt(product.getSku(), product.getName(), historyList, 7);
//                Map<String, Object> geminiResp = callGemini(prompt, 0.0, 1);
//
//                if (geminiResp == null) {
//                    // skip or add error entry
//                    continue;
//                }
//
//                String raw = extractGeminiContent(geminiResp);
//                Map<String, Object> parsed = tryParseJson(raw);
//                if (parsed == null || !parsed.containsKey("forecast")) {
//                    continue;
//                }
//
//                @SuppressWarnings("unchecked")
//                List<Map<String, Object>> forecastList = (List<Map<String, Object>>) parsed.get("forecast");
//                double next7DaysDemand = forecastList.stream()
//                        .mapToDouble(f -> Double.parseDouble(f.get("predictedDemand").toString()))
//                        .sum();
//
//                long suggestedQty = Math.max(0, Math.round(next7DaysDemand - (product.getCurrentStock() == null ? 0 : product.getCurrentStock())));
//
//                Map<String, Object> item = new LinkedHashMap<>();
//                item.put("productId", product.getId());
//                item.put("productName", product.getName());
//                item.put("sku", product.getSku());
//                item.put("vendor", product.getVendorName());
//                item.put("currentStock", product.getCurrentStock());
//                item.put("forecast", forecastList);
//                item.put("predicted7DayDemand", Math.round(next7DaysDemand));
//                item.put("suggestedReorderQty", suggestedQty);
//                item.put("urgency", product.getCurrentStock() == null || product.getCurrentStock() < 5 ? "HIGH" : "MEDIUM");
//                item.put("confidence", parsed.getOrDefault("confidence", 0.5));
//                suggestions.add(item);
//            }
//
//            return suggestions;
//
//        } catch (Exception e) {
//            log.error("AI recommendations failed: {}", e.getMessage(), e);
//            return List.of(Map.of("error", "AI service unavailable"));
//        }
//    }
//
//    // ----------------- Helpers -----------------
//
//    private ForecastingResponse buildFallbackForecast(Product product) {
//        ForecastingResponse fr = new ForecastingResponse();
//        fr.setProductSku(product.getSku());
//        fr.setProductName(product.getName());
//        Map<String, Double> preds = new LinkedHashMap<>();
//        double value = product.getCurrentStock() == null ? 1 : product.getCurrentStock();
//        for (int i = 1; i <= 7; i++) {
//            preds.put(LocalDate.now().plusDays(i).format(fmt), value);
//        }
//        fr.setPredictions(preds);
//        fr.setAccuracy(0.4);
//        return fr;
//    }
//
//    /**
//     * Call Gemini REST endpoint and return parsed response as Map.
//     * We expect a response structure similar to:
//     * {
//     *   "candidates": [
//     *     { "content": "...model text..." , ... }
//     *   ],
//     *   ...
//     * }
//     */
//    @SuppressWarnings("unchecked")
//    private Map<String, Object> callGemini(String prompt, double temperature, int candidateCount) {
//        try {
//            String url = geminiEndpoint;
//            if (apiKey != null && !apiKey.isBlank()) {
//                if (!url.contains("?")) url = url + "?key=" + apiKey;
//                else url = url + "&key=" + apiKey;
//            }
//
//            Map<String, Object> requestBody = new HashMap<>();
//            Map<String, String> promptMap = new HashMap<>();
//            promptMap.put("text", prompt);
//            requestBody.put("prompt", promptMap);
//            // recommended controls
//            requestBody.put("temperature", temperature);
//            requestBody.put("candidateCount", candidateCount);
//            requestBody.put("topP", 0.95);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//
//            if (!resp.getStatusCode().is2xxSuccessful()) {
//                log.error("Gemini returned non-2xx: {}", resp.getStatusCode());
//                return null;
//            }
//
//            String body = resp.getBody();
//            if (body == null) return null;
//
//            Map<String, Object> parsed = mapper.readValue(body, new TypeReference<>() {});
//            return parsed;
//
//        } catch (HttpStatusCodeException ex) {
//            log.error("Gemini API HTTP error: {} -> {}", ex.getStatusCode(), ex.getResponseBodyAsString());
//            return null;
//        } catch (Exception e) {
//            log.error("callGemini exception: {}", e.getMessage(), e);
//            return null;
//        }
//    }
//
//    /**
//     * Extract "content" text from Gemini response. Tries candidates[0].content, candidates[0].output[0].content, etc.
//     */
//    @SuppressWarnings("unchecked")
//    private String extractGeminiContent(Map<String, Object> geminiResp) {
//        try {
//            if (geminiResp.containsKey("candidates")) {
//                Object c = geminiResp.get("candidates");
//                if (c instanceof List && !((List<?>) c).isEmpty()) {
//                    Object first = ((List<?>) c).get(0);
//                    if (first instanceof Map) {
//                        Map<String, Object> m = (Map<String, Object>) first;
//                        if (m.containsKey("content")) return m.get("content").toString();
//                        // some versions use "output" -> list of content pieces
//                        if (m.containsKey("output")) {
//                            Object out = m.get("output");
//                            if (out instanceof List && !((List<?>) out).isEmpty()) {
//                                Object o0 = ((List<?>) out).get(0);
//                                if (o0 instanceof Map && ((Map<?,?>)o0).containsKey("content")) {
//                                    return ((Map<?,?>)o0).get("content").toString();
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            // fallback: if top-level 'content' exists
//            if (geminiResp.containsKey("content")) return geminiResp.get("content").toString();
//        } catch (Exception e) {
//            log.warn("extractGeminiContent failed: {}", e.getMessage());
//        }
//        return null;
//    }
//
//    private Map<String, Object> tryParseJson(String raw) {
//        if (raw == null) return null;
//        try {
//            // Trim and try to find JSON start
//            String candidate = raw.strip();
//            // Some LLM responses contain explanatory text + JSON. Try to locate first '{'
//            int idx = candidate.indexOf('{');
//            if (idx >= 0) candidate = candidate.substring(idx);
//            Map<String, Object> parsed = mapper.readValue(candidate, new TypeReference<>() {});
//            return parsed;
//        } catch (Exception e) {
//            log.warn("Failed to parse Gemini content as JSON: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    /**
//     * Build a prompt that asks the model to return a strict JSON containing forecast array and confidence.
//     * We keep the prompt deterministic and short.
//     */
//    private String buildForecastPrompt(String sku, String name, List<Map<String, Object>> history, int days) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("You are an inventory forecasting assistant. ");
//        sb.append("Given the historical daily quantities for a product, produce a JSON object with:\n");
//        sb.append("  { \"forecast\": [ {\"date\":\"YYYY-MM-DD\",\"predictedDemand\": <number>}, ... ], \"confidence\": <0-1 float> }\n");
//        sb.append("Output MUST be valid JSON only (no extra explanation). Use the next ").append(days).append(" calendar days for the forecast dates.\n");
//        sb.append("Product SKU: ").append(sku).append("\n");
//        sb.append("Product Name: ").append(name == null ? "" : name).append("\n");
//        sb.append("History (date -> quantity):\n");
//        for (Map<String, Object> h : history) {
//            sb.append(h.get("date")).append(" -> ").append(h.get("quantity")).append("\n");
//        }
//        sb.append("\nReturn the JSON now.");
//        return sb.toString();
//    }
//}
