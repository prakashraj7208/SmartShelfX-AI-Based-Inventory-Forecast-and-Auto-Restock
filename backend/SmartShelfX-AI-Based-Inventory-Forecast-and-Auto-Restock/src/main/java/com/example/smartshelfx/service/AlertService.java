package com.example.smartshelfx.service;

import com.example.smartshelfx.model.Alert;
import com.example.smartshelfx.model.Product;
import com.example.smartshelfx.model.Role;
import com.example.smartshelfx.model.User;
import com.example.smartshelfx.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final EmailService emailService;
    private final UserService userService;

    public boolean hasActiveLowStockAlert(Product product) {
        return alertRepository.existsByProductAndTypeAndIsReadFalse(
                product,
                Alert.AlertType.LOW_STOCK
        );
    }

    // Public method used by InventoryService
    @Transactional
    public void createLowStockAlert(Product product) {
        boolean exists = alertRepository.existsByProductAndTypeAndIsReadFalse(product, Alert.AlertType.LOW_STOCK);
        if (exists) return;

        Alert alert = Alert.createLowStockAlert(product, product.getCurrentStock(), product.getReorderLevel());
        alert.setCreatedAt(LocalDateTime.now());
        alertRepository.save(alert);

        try { sendLowStockAlertEmails(product); } catch (Exception e) { log.warn("Email failed: {}", e.getMessage()); }
    }

    // Public method used by InventoryService
    @Transactional
    public void resolveLowStockAlerts(Product product) {
        // Mark unread low-stock alerts for this product as read
        List<Alert> alerts = alertRepository.findByProductAndTypeOrderByCreatedAtDesc(product, Alert.AlertType.LOW_STOCK);
        for (Alert a : alerts) {
            if (!a.getIsRead()) {
                a.setIsRead(true);
                a.setResolvedAt(LocalDateTime.now());
                alertRepository.save(a);
            }
        }
    }

    // Public method used by ForecastService
    @Transactional
    public void createRestockSuggestionAlert(Product product, Integer suggestedQuantity) {
        boolean exists = alertRepository.existsByProductAndTypeAndIsReadFalse(product, Alert.AlertType.RESTOCK_SUGGESTION);
        if (exists) return;

        Alert alert = new Alert();
        alert.setType(Alert.AlertType.RESTOCK_SUGGESTION);
        alert.setTitle("🔁 Restock Suggestion: " + product.getName());
        alert.setDescription(String.format("Suggested to order %d units for %s (current stock: %d)",
                suggestedQuantity, product.getName(), product.getCurrentStock()));
        alert.setPriority(Alert.Priority.MEDIUM);
        alert.setProduct(product);
        alert.setSuggestedAction("Review suggestion and create PO");
        alert.setCreatedAt(LocalDateTime.now());
        alertRepository.save(alert);

        try {
            List<User> admins = userService.getUsersByRole(Role.ROLE_ADMIN);
            for (User u : admins) {
                emailService.sendRestockSuggestionEmail(u.getEmail(), product.getName(), suggestedQuantity);
            }
        } catch (Exception e) {
            log.warn("Failed to email restock suggestion: {}", e.getMessage());
        }
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getUnreadAlerts() {
        return alertRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }

    public List<Alert> getRecentAlerts() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return alertRepository.findRecentAlerts(since);
    }

    public List<Alert> getAlertsByProduct(Long productId) {
        return alertRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Transactional
    public Alert markAsRead(Long alertId) {
        return alertRepository.findById(alertId).map(a -> {
            a.setIsRead(true);
            a.setResolvedAt(LocalDateTime.now());
            return alertRepository.save(a);
        }).orElse(null);
    }

    @Transactional
    public void markAllAsRead() {
        List<Alert> unread = alertRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unread.forEach(a -> {
            a.setIsRead(true);
            a.setResolvedAt(LocalDateTime.now());
            alertRepository.save(a);
        });
    }

    public long getUnreadAlertCount() {
        return alertRepository.countByIsReadFalse();
    }

    // Email helpers (best-effort, swallow exceptions)
    private void sendLowStockAlertEmails(Product product) {
        try {
            List<User> managers = userService.getUsersByRole(Role.ROLE_MANAGER);
            List<User> admins = userService.getUsersByRole(Role.ROLE_ADMIN);
            Set<User> recipients = new HashSet<>();
            if (managers != null) recipients.addAll(managers);
            if (admins != null) recipients.addAll(admins);

            for (User r : recipients) {
                emailService.sendLowStockAlertEmail(r.getEmail(), product.getName(), product.getCurrentStock(), product.getReorderLevel());
            }
        } catch (Exception e) {
            log.warn("sendLowStockAlertEmails failed: {}", e.getMessage());
        }
    }

}
