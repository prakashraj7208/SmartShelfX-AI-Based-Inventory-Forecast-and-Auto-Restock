package com.example.smartshelfx.repository;

import com.example.smartshelfx.model.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, Long> {
    List<Forecast> findByProductIdOrderByForecastDateDesc(Long productId);

    @Query("SELECT f FROM Forecast f WHERE f.product.id = :productId AND f.forecastDate >= :startDate ORDER BY f.forecastDate")
    List<Forecast> findRecentForecasts(@Param("productId") Long productId, @Param("startDate") LocalDate startDate);

    @Query("SELECT f FROM Forecast f WHERE f.forecastDate = :date")
    List<Forecast> findByForecastDate(@Param("date") LocalDate date);

    // FIXED: Use proper date comparison
    @Query("SELECT f FROM Forecast f WHERE f.product.currentStock <= f.predictedDemand AND f.forecastDate <= :futureDate")
    List<Forecast> findStockoutRiskProducts(@Param("futureDate") LocalDate futureDate);

    @Query("SELECT AVG(f.accuracy) FROM Forecast f WHERE f.accuracy IS NOT NULL")
    Double findAverageAccuracy();
}