package com.example.smartshelfx.controller;

import com.example.smartshelfx.service.EmailService;
import com.example.smartshelfx.service.InventoryService;
import com.example.smartshelfx.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class NotificationController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/low-stock/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> sendLowStockNotification(
            @PathVariable Long productId,
            @RequestParam String vendorEmail) {
        try {
            // ✅ FIXED: Check if product exists directly
            var product = productService.getProductById(productId);
            if (product != null) {
                emailService.sendLowStockAlert(
                        vendorEmail,
                        product.getName(),
                        product.getSku(),
                        product.getCurrentStock()
                );

                return ResponseEntity.ok(Map.of(
                        "message", "Low stock alert sent to vendor",
                        "vendorEmail", vendorEmail,
                        "product", product.getName()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to send notification: " + e.getMessage()));
        }
    }

    @PostMapping("/test-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestParam String email) {
        try {
            emailService.sendSimpleEmail(
                    email,
                    "SmartShelfX - Test Email",
                    "This is a test email from SmartShelfX system. Your email configuration is working correctly."
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Test email sent successfully",
                    "to", email
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to send test email: " + e.getMessage()));
        }
    }

    @GetMapping("/low-stock-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getLowStockAlerts() {
        try {
            var lowStockProducts = productService.getLowStockProducts();
            Map<String, Object> response = new HashMap<>();
            response.put("count", lowStockProducts.size());
            response.put("products", lowStockProducts);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get low stock alerts: " + e.getMessage()));
        }
    }
}