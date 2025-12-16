//package com.example.smartshelfx.service;
//
//import com.example.smartshelfx.dto.RestockSuggestionDto;
//import com.example.smartshelfx.model.Product;
//import com.example.smartshelfx.repository.ProductRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class RestockService {
//
//    private final ProductRepository productRepository;
//    private final ForecastService forecastService;
//
//    public RestockService(ProductRepository productRepository, ForecastService forecastService) {
//        this.productRepository = productRepository;
//        this.forecastService = forecastService;
//    }
//
//    public List<RestockSuggestionDto> generateSuggestions(Integer horizonDays, Integer historyDays, Boolean onlyRisky) {
//        List<Product> products = productRepository.findAll();
//        List<RestockSuggestionDto> suggestions = new ArrayList<>();
//
//        for (Product p : products) {
//
//            if (!Boolean.TRUE.equals(p.getActive())) continue;
//
//            try {
//                // ✅ NEW FORECAST SERVICE CALL
//                var forecast = forecastService.generateForecast(p);
//
//                RestockSuggestionDto dto = new RestockSuggestionDto();
//                dto.setProductId(p.getId());
//                dto.setSku(p.getSku());
//                dto.setName(p.getName());
//                dto.setCurrentStock(p.getCurrentStock());
//                dto.setPredictedDemand((int) forecast.get("totalDemand"));
//                dto.setRiskOfStockout((Boolean) forecast.get("riskOfStockout"));
//                dto.setRecommendedOrder((Integer) forecast.get("recommendedOrder"));
//                dto.setReason((String) forecast.get("reasoning"));
//
//                if (onlyRisky == null || !onlyRisky || dto.getRiskOfStockout()) {
//                    suggestions.add(dto);
//                }
//
//            } catch (Exception e) {
//                System.out.println("⚠ Forecast failed for product " + p.getId());
//            }
//        }
//
//        return suggestions;
//    }
//}
