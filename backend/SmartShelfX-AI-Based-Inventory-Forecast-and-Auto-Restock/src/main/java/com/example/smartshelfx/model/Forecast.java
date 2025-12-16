package com.example.smartshelfx.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "forecasts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Forecast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDate forecastDate;

    @Column(nullable = false)
    private Integer predictedDemand;

    private Double confidenceScore;

    private String algorithmUsed = "MOVING_AVERAGE";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Historical comparison
    private Integer actualSales;
    private Double accuracy;

    @Column(name = "forecast_period_days")
    private Integer forecastPeriodDays = 7;
}