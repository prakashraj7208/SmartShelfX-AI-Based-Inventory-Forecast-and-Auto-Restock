package com.example.smartshelfx.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendLowStockAlert(String toEmail, String productName, String sku, Integer currentStock) {
        String subject = "Low Stock Alert - " + productName;

        String body = String.format(
                "Product: %s (SKU: %s)\nCurrent Stock: %d\nPlease consider restocking soon.\n\nSmartShelfX System",
                productName, sku, currentStock
        );

        sendSimpleEmail(toEmail, subject, body);
    }

    // ✅ RESTOCK SUGGESTION EMAIL
    public void sendRestockSuggestionEmail(String recipientEmail, String productName, Integer suggestedQuantity) {
        try {
            String subject = "Restock Suggestion - " + productName;

            String body = String.format("""
                    Hello,

                    A restock suggestion has been generated.

                    Product: %s
                    Suggested Quantity: %d

                    Please review and create a purchase order if needed.

                    SmartShelfX System
                    """, productName, suggestedQuantity);

            sendSimpleEmail(recipientEmail, subject, body);
            log.info("Restock suggestion email sent to {}", recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send restock suggestion email: {}", e.getMessage());
        }
    }

    // ✅ PURCHASE ORDER EMAIL
    public void sendPurchaseOrderEmail(String vendorEmail, String vendorName, String poNumber,
                                       String productName, Integer quantity) {

        String subject = "📋 New Purchase Order - " + poNumber;

        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <body>
                    <h2>New Purchase Order</h2>
                    <p>Dear %s,</p>
                    <p>You have received a new purchase order:</p>
                    <p><b>PO Number:</b> %s</p>
                    <p><b>Product:</b> %s</p>
                    <p><b>Quantity:</b> %d units</p>
                    <p>Please review and confirm availability.</p>
                </body>
                </html>
                """, vendorName, poNumber, productName, quantity);

        try {
           // sendHtmlEmail(vendorEmail, subject, htmlBody);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(vendorEmail);
            message.setSubject(subject);
            message.setText(htmlBody);

            mailSender.send(message);
        } catch (Exception e) {
           throw new RuntimeException(e);
        }
    }

    // ✅ LOW STOCK ALERT EMAIL
    public void sendLowStockAlertEmail(String toEmail, String productName, Integer currentStock, Integer reorderLevel) {
        String subject = "🚨 Low Stock Alert - " + productName;

        String htmlBody = String.format("""
                <!DOCTYPE html>
                <html>
                <body>
                    <h2>Low Stock Alert</h2>
                    <p><b>Product:</b> %s</p>
                    <p><b>Current Stock:</b> %d</p>
                    <p><b>Reorder Level:</b> %d</p>
                </body>
                </html>
                """, productName, currentStock, reorderLevel);

        try {
           // sendHtmlEmail(toEmail, subject, htmlBody);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(htmlBody);

            mailSender.send(message);
        } catch (Exception e) {
            sendSimpleEmail(toEmail, subject,
                    "Product: " + productName +
                            "\nCurrent Stock: " + currentStock +
                            "\nReorder Level: " + reorderLevel);
        }
    }

    // ✅ AI PREDICTION EMAIL
    public void sendAIPredictionEmail(String toEmail, String productName, double predictedDemand, String recommendation) {

        String subject = "🤖 AI Prediction Alert - " + productName;

        String htmlBody = String.format("""
                <html>
                <body>
                    <h2>AI Demand Prediction</h2>
                    <p><b>Product:</b> %s</p>
                    <p><b>Predicted Demand:</b> %.1f</p>
                    <p><b>Recommendation:</b> %s</p>
                </body>
                </html>
                """, productName, predictedDemand, recommendation);

        try {
            sendHtmlEmail(toEmail, subject, htmlBody);
        } catch (MessagingException e) {
            sendSimpleEmail(toEmail, subject,
                    "Product: " + productName +
                            "\nPredicted Demand: " + predictedDemand +
                            "\nRecommendation: " + recommendation);
        }
    }

    // SIMPLE EMAIL
    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // HTML EMAIL
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
