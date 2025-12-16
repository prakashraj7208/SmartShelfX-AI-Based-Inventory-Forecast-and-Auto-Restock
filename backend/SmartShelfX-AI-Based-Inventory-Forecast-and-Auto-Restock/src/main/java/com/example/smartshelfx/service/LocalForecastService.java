package com.example.smartshelfx.service;

import com.example.smartshelfx.model.StockTransaction;
import com.example.smartshelfx.repository.StockTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalForecastService {

    private final StockTransactionRepository stockRepo;

    public Map<String, Object> generateForecast(Long productId) {

        List<StockTransaction> transactions = stockRepo.findByProductIdOrderByTimestampDesc(productId);

        if (transactions.isEmpty()) {
            return Map.of(
                    "productId", productId,
                    "message", "No transaction history found",
                    "status", "NO_DATA"
            );
        }

        // Group daily sales (OUT transactions)
        Map<LocalDate, Integer> dailySales = new TreeMap<>();

        for (StockTransaction t : transactions) {
            if (t.getType() == StockTransaction.TransactionType.OUT) {
                LocalDate day = t.getTimestamp().toLocalDate();
                dailySales.put(day, dailySales.getOrDefault(day, 0) + t.getQuantity());
            }
        }

        if (dailySales.isEmpty()) {
            return Map.of(
                    "productId", productId,
                    "message", "No sales history",
                    "status", "NO_SALES"
            );
        }

        List<Integer> values = new ArrayList<>(dailySales.values());

        // Moving Averages
        double avg7 = movingAverage(values, 7);
        double avg14 = movingAverage(values, 14);

        // Simple forecast for the next 7 days
        double next7Days = avg7 * 7;

        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("7_day_MA", avg7);
        response.put("14_day_MA", avg14);
        response.put("next7DaysPrediction", next7Days);
        response.put("dailySales", dailySales);

        return response;
    }

    private double movingAverage(List<Integer> values, int period) {
        if (values.isEmpty()) return 0;

        int count = Math.min(values.size(), period);
        return values.subList(0, count).stream().mapToDouble(i -> i).average().orElse(0);
    }
}
