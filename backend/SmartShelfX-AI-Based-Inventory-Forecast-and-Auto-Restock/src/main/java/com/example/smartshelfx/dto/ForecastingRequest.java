package com.example.smartshelfx.dto;

import java.time.LocalDate;

public class ForecastingRequest {
    private String productSku;
    private LocalDate startDate;
    private LocalDate endDate;
    private String period; // "daily", "weekly", "monthly"

    // Constructors, Getters, Setters
    public ForecastingRequest() {}

    public ForecastingRequest(String productSku, LocalDate startDate, LocalDate endDate, String period) {
        this.productSku = productSku;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
    }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}