package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.model.PurchaseOrder;
import com.example.smartshelfx.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> createPurchaseOrder(
            @RequestBody PurchaseOrder purchaseOrder,
            @RequestParam Long createdBy) {
        try {
            PurchaseOrder created = purchaseOrderService.createPurchaseOrder(purchaseOrder, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Purchase order created successfully", created));
        } catch (Exception e) {
            log.error("Error creating purchase order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create purchase order: " + e.getMessage(), null));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VENDOR')")
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getAllPurchaseOrders() {
        try {
            List<PurchaseOrder> orders = purchaseOrderService.getAllPurchaseOrders();
            return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved successfully", orders));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve purchase orders", null));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VENDOR')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> getPurchaseOrderById(@PathVariable Long id) {
        try {
            PurchaseOrder order = purchaseOrderService.getPurchaseOrderById(id);
            return ResponseEntity.ok(ApiResponse.success("Purchase order retrieved successfully", order));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> approvePurchaseOrder(
            @PathVariable Long id,
            @RequestParam Long approvedBy) {
        try {
            PurchaseOrder order = purchaseOrderService.approvePurchaseOrder(id, approvedBy);
            return ResponseEntity.ok(ApiResponse.success("Purchase order approved successfully", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to approve purchase order: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/mark-ordered")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> markAsOrdered(@PathVariable Long id) {
        try {
            PurchaseOrder order = purchaseOrderService.markAsOrdered(id);
            return ResponseEntity.ok(ApiResponse.success("Purchase order marked as ordered", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to mark as ordered: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/mark-delivered")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> markAsDelivered(
            @PathVariable Long id,
            @RequestParam Long receivedBy) {
        try {
            PurchaseOrder order = purchaseOrderService.markAsDelivered(id, receivedBy);
            return ResponseEntity.ok(ApiResponse.success("Purchase order marked as delivered", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to mark as delivered: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrder>> cancelPurchaseOrder(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            PurchaseOrder order = purchaseOrderService.cancelPurchaseOrder(id, reason);
            return ResponseEntity.ok(ApiResponse.success("Purchase order cancelled successfully", order));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to cancel purchase order: " + e.getMessage(), null));
        }
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VENDOR')")
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getVendorPurchaseOrders(@PathVariable Long vendorId) {
        try {
            List<PurchaseOrder> orders = purchaseOrderService.getPurchaseOrdersByVendor(vendorId);
            return ResponseEntity.ok(ApiResponse.success("Vendor purchase orders retrieved", orders));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve vendor purchase orders", null));
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getPurchaseOrdersByStatus(
            @PathVariable PurchaseOrder.OrderStatus status) {
        try {
            List<PurchaseOrder> orders = purchaseOrderService.getPurchaseOrdersByStatus(status);
            return ResponseEntity.ok(ApiResponse.success("Purchase orders retrieved by status", orders));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve purchase orders by status", null));
        }
    }

    @GetMapping("/analytics/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<PurchaseOrderService.POCountSummary>> getPOSummary() {
        try {
            PurchaseOrderService.POCountSummary summary = purchaseOrderService.getPOCountSummary();
            return ResponseEntity.ok(ApiResponse.success("Purchase order summary retrieved", summary));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve purchase order summary", null));
        }
    }

    @GetMapping("/upcoming-deliveries")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getUpcomingDeliveries() {
        try {
            List<PurchaseOrder> orders = purchaseOrderService.getUpcomingDeliveries();
            return ResponseEntity.ok(ApiResponse.success("Upcoming deliveries retrieved", orders));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve upcoming deliveries", null));
        }
    }

}