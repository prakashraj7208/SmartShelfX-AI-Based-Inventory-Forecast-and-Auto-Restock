package com.example.smartshelfx.dto;

import lombok.Data;

@Data
public class StockInRequest {
    private Long productId;
    private Integer quantity;
    private String notes;
    private String referenceNumber;
}