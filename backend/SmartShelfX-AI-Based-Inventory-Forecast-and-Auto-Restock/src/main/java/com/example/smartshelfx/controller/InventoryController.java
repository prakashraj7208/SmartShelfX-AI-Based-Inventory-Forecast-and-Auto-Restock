package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.dto.StockInRequest;
import com.example.smartshelfx.dto.StockOutRequest;
import com.example.smartshelfx.model.StockTransaction;
import com.example.smartshelfx.model.User;
import com.example.smartshelfx.security.CustomUserDetails;
import com.example.smartshelfx.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/stock-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<StockTransaction>> addStock(
            @RequestBody StockInRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userDetails.getUser();
            StockTransaction transaction = inventoryService.addStock(
                    request.getProductId(),
                    request.getQuantity(),
                    request.getNotes(),
                    request.getReferenceNumber(),
                    currentUser
            );
            return ResponseEntity.ok(ApiResponse.success("Stock added successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    @PostMapping("/stock-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<StockTransaction>> removeStock(
            @RequestBody StockOutRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            User currentUser = userDetails.getUser();
            StockTransaction transaction = inventoryService.removeStock(
                    request.getProductId(),
                    request.getQuantity(),
                    request.getNotes(),
                    request.getReferenceNumber(),
                    currentUser
            );
            return ResponseEntity.ok(ApiResponse.success("Stock removed successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<StockTransaction>>> getAllTransactions() {
        try {
            List<StockTransaction> transactions = inventoryService.getAllTransactions();
            return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", transactions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch transactions", null));
        }
    }

    @GetMapping("/transactions/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<StockTransaction>>> getRecentTransactions() {
        try {
            List<StockTransaction> transactions = inventoryService.getRecentTransactions(20);
            return ResponseEntity.ok(ApiResponse.success("Recent transactions fetched", transactions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch recent transactions", null));
        }
    }

    @GetMapping("/products/{productId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<StockTransaction>>> getProductTransactions(@PathVariable Long productId) {
        try {
            List<StockTransaction> transactions = inventoryService.getTransactionsByProduct(productId);
            return ResponseEntity.ok(ApiResponse.success("Product transactions fetched", transactions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch product transactions", null));
        }
    }
}