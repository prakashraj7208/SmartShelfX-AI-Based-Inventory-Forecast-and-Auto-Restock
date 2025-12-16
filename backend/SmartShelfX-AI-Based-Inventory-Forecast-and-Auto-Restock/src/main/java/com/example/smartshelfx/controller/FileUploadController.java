package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.model.Product;
import com.example.smartshelfx.service.CSVImportService;
import com.example.smartshelfx.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final CSVImportService csvImportService;

    @PostMapping("/products/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importProductsFromCSV(
            @RequestParam("file") MultipartFile file) { // Removed vendorId parameter

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please select a CSV file to upload", null));
            }

            if (!file.getContentType().equals("text/csv") &&
                    !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please upload a CSV file", null));
            }

            List<Product> importedProducts = csvImportService.importProductsFromCSV(file);

            Map<String, Object> result = Map.of(
                    "successCount", importedProducts.size(),
                    "errorCount", 0,
                    "importedProducts", importedProducts,
                    "message", "Products imported successfully"
            );

            return ResponseEntity.ok(ApiResponse.success("CSV import completed successfully", result));

        } catch (Exception e) {
            log.error("Error importing products from CSV: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to import products: " + e.getMessage(), null));
        }
    }

    @PostMapping("/products/image")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> uploadProductImage(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please select an image file", null));
            }

            // Validate image type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please upload a valid image file", null));
            }

            String filename = fileStorageService.storeProductImage(file);
            return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", filename));

        } catch (IOException e) {
            log.error("Error uploading product image: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload image: " + e.getMessage(), null));
        }
    }

    @GetMapping("/files/{filename}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VENDOR')")
    public ResponseEntity<byte[]> getFile(
            @PathVariable String filename,
            @RequestParam String type) { // type: 'images', 'csv', 'reports'

        try {
            String directory = getDirectoryByType(type);
            byte[] fileContent = fileStorageService.loadFile(filename, directory);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(fileContent);

        } catch (IOException e) {
            log.error("Error retrieving file {}: {}", filename, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private String getDirectoryByType(String type) {
        switch (type.toLowerCase()) {
            case "images": return "uploads/images";
            case "csv": return "uploads/csv";
            case "reports": return "uploads/reports";
            default: throw new IllegalArgumentException("Invalid file type: " + type);
        }
    }
}