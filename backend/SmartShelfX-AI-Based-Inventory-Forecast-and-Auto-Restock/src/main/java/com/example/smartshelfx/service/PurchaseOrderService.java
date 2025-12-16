package com.example.smartshelfx.service;

import com.example.smartshelfx.model.*;
import com.example.smartshelfx.repository.PurchaseOrderRepository;
import com.example.smartshelfx.repository.ProductRepository;
import com.example.smartshelfx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockTransactionService stockTransactionService;
    private final AlertService alertService;
    private final EmailService emailService;

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder, Long createdById) {
        // Validate product
        Product product = productRepository.findById(purchaseOrder.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Validate vendor
        User vendor = userRepository.findById(purchaseOrder.getVendor().getId())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // Validate createdBy user
        User createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate PO number if not provided
        if (purchaseOrder.getPoNumber() == null) {
            purchaseOrder.setPoNumber(generatePONumber());
        }

        // Set relationships
        purchaseOrder.setProduct(product);
        purchaseOrder.setVendor(vendor);
        purchaseOrder.setCreatedBy(createdBy);
        purchaseOrder.setStatus(PurchaseOrder.OrderStatus.PENDING);

        // Calculate total amount
        if (purchaseOrder.getUnitPrice() != null && purchaseOrder.getQuantity() != null) {
            purchaseOrder.setTotalAmount(purchaseOrder.getUnitPrice().multiply(
                    java.math.BigDecimal.valueOf(purchaseOrder.getQuantity())));
        }

        PurchaseOrder savedPO = purchaseOrderRepository.save(purchaseOrder);

        // Create alert for new purchase order
        createPurchaseOrderAlert(savedPO);

        // ✅ NEW: Send email notification to vendor
        sendPurchaseOrderEmail(savedPO);

        log.info("Purchase order created: {} for product '{}'", savedPO.getPoNumber(), product.getName());
        return savedPO;
    }

    // ✅ NEW: Email notification method
    private void sendPurchaseOrderEmail(PurchaseOrder purchaseOrder) {
        try {
            emailService.sendPurchaseOrderEmail(
                    purchaseOrder.getVendor().getEmail(),
                    purchaseOrder.getVendor().getFullName(),
                    purchaseOrder.getPoNumber(),
                    purchaseOrder.getProduct().getName(),
                    purchaseOrder.getQuantity()
            );
            log.info("Purchase order email sent to vendor: {}", purchaseOrder.getVendor().getEmail());
        } catch (Exception e) {
            log.error("Failed to send PO email to {}: {}",
                    purchaseOrder.getVendor().getEmail(), e.getMessage());
            // Don't throw exception - email failure shouldn't block PO creation
        }
    }

    @Transactional
    public PurchaseOrder createAIGeneratedPO(Product product, Integer quantity, Long vendorId, Long createdById) {
        PurchaseOrder po = new PurchaseOrder();
        po.setProduct(product);
        po.setVendor(userRepository.findById(vendorId).orElseThrow(() -> new RuntimeException("Vendor not found")));
        po.setQuantity(quantity);
        po.setUnitPrice(product.getPrice()); // Use product price as default
        po.setStatus(PurchaseOrder.OrderStatus.PENDING);
        po.setNotes("AI-generated purchase order based on demand forecast");

        return createPurchaseOrder(po, createdById);
    }

    @Transactional
    public PurchaseOrder approvePurchaseOrder(Long poId, Long approvedById) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (po.getStatus() != PurchaseOrder.OrderStatus.PENDING) {
            throw new RuntimeException("Only pending purchase orders can be approved");
        }

        po.setStatus(PurchaseOrder.OrderStatus.APPROVED);
        po.setUpdatedAt(LocalDateTime.now());

        // Create alert for approval
        Alert approvalAlert = Alert.builder()
                .product(po.getProduct())
                .type(Alert.AlertType.PURCHASE_ORDER_UPDATE)
                .title("✅ Purchase Order Approved: " + po.getPoNumber())
                .description("Purchase order for " + po.getQuantity() + " units of " + po.getProduct().getName() + " has been approved")
                .priority(Alert.Priority.MEDIUM)
                .suggestedAction("Proceed with ordering from vendor")
                .build();
        // Save alert through your alert service

        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder markAsOrdered(Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (po.getStatus() != PurchaseOrder.OrderStatus.APPROVED) {
            throw new RuntimeException("Only approved purchase orders can be marked as ordered");
        }

        po.setStatus(PurchaseOrder.OrderStatus.ORDERED);
        po.setUpdatedAt(LocalDateTime.now());

        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder markAsDelivered(Long poId, Long receivedById) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (po.getStatus() != PurchaseOrder.OrderStatus.ORDERED) {
            throw new RuntimeException("Only ordered purchase orders can be marked as delivered");
        }

        User receivedBy = userRepository.findById(receivedById)
                .orElseThrow(() -> new RuntimeException("User not found"));

        po.setStatus(PurchaseOrder.OrderStatus.DELIVERED);
        po.setUpdatedAt(LocalDateTime.now());

        // Create stock transaction for the received goods
        StockTransaction stockIn = new StockTransaction();
        stockIn.setProduct(po.getProduct());
        stockIn.setQuantity(po.getQuantity());
        stockIn.setType(StockTransaction.TransactionType.IN);
        stockIn.setNotes("Purchase order delivery: " + po.getPoNumber());
        stockIn.setReferenceNumber(po.getPoNumber());

        stockTransactionService.createStockTransaction(stockIn, receivedById);

        log.info("Purchase order {} marked as delivered. {} units added to stock.",
                po.getPoNumber(), po.getQuantity());

        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long poId, String reason) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (po.getStatus() == PurchaseOrder.OrderStatus.DELIVERED) {
            throw new RuntimeException("Delivered purchase orders cannot be cancelled");
        }

        po.setStatus(PurchaseOrder.OrderStatus.CANCELLED);
        po.setNotes((po.getNotes() != null ? po.getNotes() + " " : "") + "CANCELLED: " + reason);
        po.setUpdatedAt(LocalDateTime.now());

        return purchaseOrderRepository.save(po);
    }

    // Query methods
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
    }

    public PurchaseOrder getPurchaseOrderByNumber(String poNumber) {
        return purchaseOrderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new RuntimeException("Purchase order not found: " + poNumber));
    }

    public List<PurchaseOrder> getPurchaseOrdersByVendor(Long vendorId) {
        return purchaseOrderRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    public List<PurchaseOrder> getPurchaseOrdersByStatus(PurchaseOrder.OrderStatus status) {
        return purchaseOrderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<PurchaseOrder> getActivePurchaseOrders() {
        return purchaseOrderRepository.findActivePurchaseOrders();
    }
    // Add this method to your existing PurchaseOrderService class
    @Transactional
    public PurchaseOrder createPurchaseOrder(Long productId, Integer quantity, Long vendorId,
                                             BigDecimal unitPrice, LocalDate expectedDelivery,
                                             String notes, Long createdById) {

        // Validate product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Validate vendor
        User vendor = userRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        // Create purchase order object
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setProduct(product);
        purchaseOrder.setVendor(vendor);
        purchaseOrder.setQuantity(quantity);
        purchaseOrder.setUnitPrice(unitPrice != null ? unitPrice : product.getPrice());
        purchaseOrder.setExpectedDelivery(expectedDelivery);
        purchaseOrder.setNotes(notes);

        // Use the existing create method
        return createPurchaseOrder(purchaseOrder, createdById);
    }
    public List<PurchaseOrder> getUpcomingDeliveries() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(7); // Next 7 days
        return purchaseOrderRepository.findUpcomingDeliveries(start, end);
    }


    // Analytics methods
    public POCountSummary getPOCountSummary() {
        Long pending = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.PENDING);
        Long approved = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.APPROVED);
        Long ordered = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.ORDERED);
        Long delivered = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.DELIVERED);

        return new POCountSummary(
                pending != null ? pending : 0,
                approved != null ? approved : 0,
                ordered != null ? ordered : 0,
                delivered != null ? delivered : 0
        );
    }

    private String generatePONumber() {
        return "PO-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void createPurchaseOrderAlert(PurchaseOrder po) {
        Alert alert = Alert.builder()
                .product(po.getProduct())
                .type(Alert.AlertType.PURCHASE_ORDER_UPDATE)
                .title("📋 New Purchase Order: " + po.getPoNumber())
                .description("Purchase order created for " + po.getQuantity() + " units of " + po.getProduct().getName())
                .priority(Alert.Priority.MEDIUM)
                .suggestedAction("Review and approve purchase order")
                .build();
        // Save alert through your alert service
    }



    // DTO for PO count summary
    public static class POCountSummary {
        private final long pendingCount;
        private final long approvedCount;
        private final long orderedCount;
        private final long deliveredCount;
        private final long totalCount;

        public POCountSummary(long pendingCount, long approvedCount, long orderedCount, long deliveredCount) {
            this.pendingCount = pendingCount;
            this.approvedCount = approvedCount;
            this.orderedCount = orderedCount;
            this.deliveredCount = deliveredCount;
            this.totalCount = pendingCount + approvedCount + orderedCount + deliveredCount;
        }

        // Getters
        public long getPendingCount() { return pendingCount; }
        public long getApprovedCount() { return approvedCount; }
        public long getOrderedCount() { return orderedCount; }
        public long getDeliveredCount() { return deliveredCount; }
        public long getTotalCount() { return totalCount; }
    }
}