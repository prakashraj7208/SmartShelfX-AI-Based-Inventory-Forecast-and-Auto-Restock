//package com.example.smartshelfx.controller;
//
//import com.example.smartshelfx.ai.SmartShelfXAIService;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/forecast")
//public class ForecastController {
//
//    private final SmartShelfXAIService ai;
//
//    public ForecastController(SmartShelfXAIService ai) {
//        this.ai = ai;
//    }
//
//    @PostMapping
//    public String forecast(@RequestBody Map<String, Object> input) {
//        String prompt = "Generate forecast for product: " + input.get("product");
//        return ai.generateForecastJson(prompt);
//    }
//}
