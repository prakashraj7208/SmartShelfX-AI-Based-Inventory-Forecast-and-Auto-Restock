package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.model.StockTransaction;
import com.example.smartshelfx.service.StockTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-transactions")
@RequiredArgsConstructor
@Slf4j
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<StockTransaction>> createTransaction(
            @RequestBody StockTransaction transaction,
            @RequestParam Long handledBy) {
        try {
            StockTransaction created = stockTransactionService.createStockTransaction(transaction, handledBy);
            return ResponseEntity.ok(ApiResponse.success("Stock transaction created successfully", created));
        } catch (Exception e) {
            log.error("Error creating stock transaction: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create stock transaction: " + e.getMessage(), null));
        }
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VENDOR')")
    public ResponseEntity<ApiResponse<List<StockTransaction>>> getTransactionsByProduct(
            @PathVariable Long productId) {
        try {
            List<StockTransaction> transactions = stockTransactionService.getTransactionsByProduct(productId);
            return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", transactions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve transactions", null));
        }
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<StockTransaction>>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<StockTransaction> transactions = stockTransactionService.getRecentTransactions(limit);
            return ResponseEntity.ok(ApiResponse.success("Recent transactions retrieved", transactions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve recent transactions", null));
        }
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<StockTransaction>>> getTransactionsByType(
            @PathVariable StockTransaction.TransactionType type) {
        try {
            List<StockTransaction> transactions = stockTransactionService.getTransactionsByType(type);
            return ResponseEntity.ok(ApiResponse.success("Transactions retrieved by type", transactions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve transactions by type", null));
        }
    }

    @PostMapping("/adjust-stock/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<StockTransaction>> adjustStock(
            @PathVariable Long productId,
            @RequestParam int newQuantity,
            @RequestParam Long handledBy,
            @RequestParam String notes) {
        try {
            StockTransaction transaction = stockTransactionService.adjustStock(productId, newQuantity, handledBy, notes);
            return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to adjust stock: " + e.getMessage(), null));
        }
    }

    @GetMapping("/analytics/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getTodayAnalytics() {
        try {
            Long stockIns = stockTransactionService.getTodayStockInCount();
            Long stockOuts = stockTransactionService.getTodayStockOutCount();

            var analytics = new Object() {
                public final Long todayStockIns = stockIns;
                public final Long todayStockOuts = stockOuts;
                public final Long netMovement = stockIns - stockOuts;
            };

            return ResponseEntity.ok(ApiResponse.success("Today's analytics retrieved", analytics));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve analytics", null));
        }
    }
}