package com.example.smartshelfx.service;

import com.example.smartshelfx.model.StockTransaction;
import com.example.smartshelfx.model.Product;
import com.example.smartshelfx.model.User;
import com.example.smartshelfx.repository.StockTransactionRepository;
import com.example.smartshelfx.repository.ProductRepository;
import com.example.smartshelfx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockTransactionService {

    private final StockTransactionRepository stockTransactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;

    @Transactional
    public StockTransaction createStockTransaction(StockTransaction transaction, Long handledById) {
        // Validate and get product
        Product product = productRepository.findById(transaction.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + transaction.getProduct().getId()));

        // Validate and get user
        User handledBy = userRepository.findById(handledById)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + handledById));

        // Set the handledBy user
        transaction.setHandledBy(handledBy);

        // Ensure timestamp is set
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(LocalDateTime.now());
        }

        // Update product stock based on transaction type
        int currentStock = product.getCurrentStock();
        int newStock = currentStock;

        switch (transaction.getType()) {
            case IN:
                newStock = currentStock + transaction.getQuantity();
                break;
            case OUT:
                newStock = currentStock - transaction.getQuantity();
                // Validate stock doesn't go negative
                if (newStock < 0) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                            ". Current: " + currentStock + ", Requested: " + transaction.getQuantity());
                }
                break;
        }

        // Update product stock
        product.setCurrentStock(newStock);
        productRepository.save(product);

        // Save the transaction
        StockTransaction savedTransaction = stockTransactionRepository.save(transaction);

        // Check for low stock alerts after OUT transactions
        if (transaction.getType() == StockTransaction.TransactionType.OUT) {
            checkLowStockAlerts(product, newStock);
        }

        log.info("Stock transaction created: {} {} units for product '{}' (New stock: {})",
                transaction.getType(), transaction.getQuantity(), product.getName(), newStock);

        return savedTransaction;
    }

    private void checkLowStockAlerts(Product product, int currentStock) {
        if (currentStock <= product.getReorderLevel()) {
            // Check if similar alert already exists
            boolean alertExists = alertService.hasActiveLowStockAlert(product);

            if (!alertExists) {
                alertService.createLowStockAlert(product);
                log.info("Low stock alert created for product: {}", product.getName());
            }
        }
    }

    public List<StockTransaction> getTransactionsByProduct(Long productId) {
        return stockTransactionRepository.findByProductIdOrderByTimestampDesc(productId);
    }

    public List<StockTransaction> getRecentTransactionsByProduct(Long productId) {
        return stockTransactionRepository.findRecentTransactionsByProduct(productId);
    }

    public List<StockTransaction> getTransactionsByType(StockTransaction.TransactionType type) {
        return stockTransactionRepository.findByTypeOrderByTimestampDesc(type);
    }

    public List<StockTransaction> getRecentTransactions(int limit) {
        return stockTransactionRepository.findRecentTransactions(limit);
    }

    public List<StockTransaction> getAllTransactions() {
        return stockTransactionRepository.findAll();
    }

    public StockTransaction getTransactionById(Long id) {
        return stockTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock transaction not found with id: " + id));
    }

    // Analytics methods
    public Long getTodayStockInCount() {
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return stockTransactionRepository.countTodayStockIns(start, end);
    }

    public Long getTodayStockOutCount() {
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return stockTransactionRepository.countTodayStockOuts(start, end);
    }


    public List<Object[]> getSalesBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return stockTransactionRepository.findSalesBetweenDates(startDate, endDate);
    }

    // Bulk stock operations
    @Transactional
    public void processBulkStockIn(List<StockTransaction> transactions, Long handledById) {
        for (StockTransaction transaction : transactions) {
            transaction.setType(StockTransaction.TransactionType.IN);
            createStockTransaction(transaction, handledById);
        }
    }

    @Transactional
    public void processBulkStockOut(List<StockTransaction> transactions, Long handledById) {
        for (StockTransaction transaction : transactions) {
            transaction.setType(StockTransaction.TransactionType.OUT);
            createStockTransaction(transaction, handledById);
        }
    }

    // Stock adjustment (manual correction)
    @Transactional
    public StockTransaction adjustStock(Long productId, int newQuantity, Long handledById, String notes) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int currentStock = product.getCurrentStock();
        int adjustment = newQuantity - currentStock;

        StockTransaction transaction = new StockTransaction();
        transaction.setProduct(product);
        transaction.setQuantity(Math.abs(adjustment));
        transaction.setType(adjustment > 0 ? StockTransaction.TransactionType.IN : StockTransaction.TransactionType.OUT);
        transaction.setNotes("Stock adjustment: " + notes + " (From: " + currentStock + " → To: " + newQuantity + ")");
        transaction.setReferenceNumber("ADJ-" + System.currentTimeMillis());

        return createStockTransaction(transaction, handledById);
    }

    // Get stock movement summary for a product
    public StockMovementSummary getStockMovementSummary(Long productId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> salesData = stockTransactionRepository.findSalesBetweenDates(startDate, endDate);

        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        Long totalIn = stockTransactionRepository.countTodayStockIns(start, end);
        Long totalOut = stockTransactionRepository.countTodayStockOuts(start, end);


        return new StockMovementSummary(totalIn != null ? totalIn : 0, totalOut != null ? totalOut : 0);
    }

    // DTO for stock movement summary
    public static class StockMovementSummary {
        private final long totalIn;
        private final long totalOut;

        public StockMovementSummary(long totalIn, long totalOut) {
            this.totalIn = totalIn;
            this.totalOut = totalOut;
        }

        public long getTotalIn() { return totalIn; }
        public long getTotalOut() { return totalOut; }
        public long getNetMovement() { return totalIn - totalOut; }
    }
}