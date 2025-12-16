package com.example.smartshelfx.controller;

import com.example.smartshelfx.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    // TEST EMAIL ENDPOINT
    @GetMapping("/test")
    public ResponseEntity<?> sendTestEmail(
            @RequestParam String email
    ) {
        emailService.sendSimpleEmail(
                email,
                "Test Email - SmartShelfX",
                "This is a test email from SmartShelfX System."
        );

        return ResponseEntity.ok("{\"status\":\"Email sent successfully!\"}");
    }


    // TEST PURCHASE ORDER EMAIL
    @GetMapping("/test-po")
    public ResponseEntity<?> sendPOEmail(
            @RequestParam String email,
            @RequestParam String vendorName,
            @RequestParam String poNumber,
            @RequestParam String productName,
            @RequestParam Integer quantity
    ) {

        emailService.sendPurchaseOrderEmail(
                email,
                vendorName,
                poNumber,
                productName,
                quantity
        );

        return ResponseEntity.ok("{\"status\":\"PO email sent successfully!\"}");
    }
}
