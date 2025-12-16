package com.example.smartshelfx.service;

import com.example.smartshelfx.model.*;
import com.example.smartshelfx.repository.ProductRepository;
import com.example.smartshelfx.repository.StockTransactionRepository;
import com.example.smartshelfx.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;

    @Transactional
    public StockTransaction addStock(Long productId, Integer quantity, String notes, String referenceNumber, User handledBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setCurrentStock(product.getCurrentStock() + quantity);
        productRepository.save(product);

        StockTransaction transaction = new StockTransaction();
        transaction.setProduct(product);
        transaction.setQuantity(quantity);
        transaction.setType(StockTransaction.TransactionType.IN);
        transaction.setNotes(notes);
        transaction.setReferenceNumber(referenceNumber != null ? referenceNumber : generateReferenceNumber());
        transaction.setHandledBy(handledBy);
        transaction.setTimestamp(LocalDateTime.now());

        StockTransaction saved = stockTransactionRepository.save(transaction);

        checkAndUpdateAlerts(product);
        return saved;
    }

    @Transactional
    public StockTransaction removeStock(Long productId, Integer quantity, String notes, String referenceNumber, User handledBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getCurrentStock() < quantity)
            throw new RuntimeException("Insufficient stock");

        product.setCurrentStock(product.getCurrentStock() - quantity);
        productRepository.save(product);

        StockTransaction transaction = new StockTransaction();
        transaction.setProduct(product);
        transaction.setQuantity(quantity);
        transaction.setType(StockTransaction.TransactionType.OUT);
        transaction.setNotes(notes);
        transaction.setReferenceNumber(referenceNumber != null ? referenceNumber : generateReferenceNumber());
        transaction.setHandledBy(handledBy);
        transaction.setTimestamp(LocalDateTime.now());

        StockTransaction saved = stockTransactionRepository.save(transaction);

        checkAndCreateLowStockAlert(product);
        return saved;
    }

    private void checkAndCreateLowStockAlert(Product product) {
        if (product.getCurrentStock() <= product.getReorderLevel()) {
            alertService.createLowStockAlert(product);
        }
    }

    private void checkAndUpdateAlerts(Product product) {
        if (product.getCurrentStock() > product.getReorderLevel()) {
            alertService.resolveLowStockAlerts(product);
        }
    }

    private String generateReferenceNumber() {
        return "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<StockTransaction> getTransactionsByProduct(Long productId) {
        return stockTransactionRepository.findByProductIdOrderByTimestampDesc(productId);
    }

    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAll();
    }

    public List<StockTransaction> getRecentTransactions(int limit) {
        return stockTransactionRepository.findRecentTransactions(limit);
    }

    // FINAL — FIXED
    public Long getTodayStockIns() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return stockTransactionRepository.countTodayStockIns(start, end);
    }

    public Long getTodayStockOuts() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return stockTransactionRepository.countTodayStockOuts(start, end);
    }
}