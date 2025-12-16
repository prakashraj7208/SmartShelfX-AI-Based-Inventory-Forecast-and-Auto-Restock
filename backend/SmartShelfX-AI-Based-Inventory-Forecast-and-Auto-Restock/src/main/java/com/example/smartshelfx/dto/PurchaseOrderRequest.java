package com.example.smartshelfx.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseOrderRequest {
    private Long productId;
    private Long vendorId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDate expectedDelivery;
    private String notes;
}