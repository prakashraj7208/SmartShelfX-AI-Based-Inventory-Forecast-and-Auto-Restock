package com.example.smartshelfx.dto;

import java.util.Map;

public class ForecastingResponse {
    private String productSku;
    private String productName;
    private Map<String, Double> predictions; // date -> predicted quantity
    private Double accuracy;
    private String message;

    // Constructors, Getters, Setters
    public ForecastingResponse() {}

    public ForecastingResponse(String productSku, String productName, Map<String, Double> predictions, Double accuracy) {
        this.productSku = productSku;
        this.productName = productName;
        this.predictions = predictions;
        this.accuracy = accuracy;
    }

    // Getters and Setters
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Map<String, Double> getPredictions() { return predictions; }
    public void setPredictions(Map<String, Double> predictions) { this.predictions = predictions; }

    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}