package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.model.Alert;
import com.example.smartshelfx.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Alert>>> getAllAlerts(@RequestParam(defaultValue = "false") Boolean unreadOnly) {
        try {
            List<Alert> alerts = unreadOnly ?
                    alertService.getUnreadAlerts() : alertService.getAllAlerts();
            return ResponseEntity.ok(ApiResponse.success("Alerts fetched successfully", alerts));
        } catch (Exception e) {
            log.error("Error fetching alerts: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch alerts", null));
        }
    }

    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Long>> getUnreadAlertCount() {
        try {
            long count = alertService.getUnreadAlertCount();
            return ResponseEntity.ok(ApiResponse.success("Unread alert count fetched", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch unread alert count", null));
        }
    }

    @PutMapping("/{alertId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Alert>> markAsRead(@PathVariable Long alertId) {
        try {
            Alert alert = alertService.markAsRead(alertId);
            if (alert == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success("Alert marked as read", alert));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to mark alert as read", null));
        }
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        try {
            alertService.markAllAsRead();
            return ResponseEntity.ok(ApiResponse.success("All alerts marked as read", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to mark all alerts as read", null));
        }
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Alert>>> getProductAlerts(@PathVariable Long productId) {
        try {
            List<Alert> alerts = alertService.getAlertsByProduct(productId);
            return ResponseEntity.ok(ApiResponse.success("Product alerts fetched", alerts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch product alerts", null));
        }
    }
}