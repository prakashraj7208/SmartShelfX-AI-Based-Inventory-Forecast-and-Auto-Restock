package com.example.smartshelfx.controller;

import com.example.smartshelfx.dto.ApiResponse;
import com.example.smartshelfx.dto.PagedResponse;
import com.example.smartshelfx.model.Product;
import com.example.smartshelfx.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Product>>> getProducts(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String category) {
        try {
            PagedResponse<Product> products = productService.getAllProducts(size, page, sortBy, sortDir, category);
            return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
        } catch (Exception e) {
            log.error("Error fetching products: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch products", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        try {
            Product product = productService.getProductById(id);
            return ResponseEntity.ok(ApiResponse.success("Product fetched successfully", product));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }



    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @RequestParam("product") String productJson,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            // Convert JSON string to Product object
            ObjectMapper objectMapper = new ObjectMapper();
            Product product = objectMapper.readValue(productJson, Product.class);

            Product savedProduct = productService.createProduct(product, image);
            return ResponseEntity.ok(ApiResponse.success("Product created successfully", savedProduct));
        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create product: " + e.getMessage(), null));
        }
    }

    //for testing use json endpoints

    @PostMapping("/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Product>> createProductJson(@RequestBody Product product) {
        try {
            Product savedProduct = productService.createProduct(product, null);
            return ResponseEntity.ok(ApiResponse.success("Product created successfully", savedProduct));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id,
            @RequestPart Product product,
            @RequestPart(required = false) MultipartFile image) {
        try {
            Product updatedProduct = productService.updateProduct(id, product, image);
            return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete product", null));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<Product>>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            PagedResponse<Product> products = productService.searchProducts(query, size, page, sortBy, sortDir);
            return ResponseEntity.ok(ApiResponse.success("Products searched successfully", products));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to search products", null));
        }
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Product>>> getLowStockProducts() {
        try {
            List<Product> lowStockProducts = productService.getLowStockProducts();
            return ResponseEntity.ok(ApiResponse.success("Low stock products fetched", lowStockProducts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch low stock products", null));
        }
    }

    @GetMapping("/critical-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Product>>> getCriticalStockProducts() {
        try {
            List<Product> criticalStockProducts = productService.getCriticalStockProducts();
            return ResponseEntity.ok(ApiResponse.success("Critical stock products fetched", criticalStockProducts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch critical stock products", null));
        }
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Product>>> getOutOfStockProducts() {
        try {
            List<Product> outOfStockProducts = productService.getOutOfStockProducts();
            return ResponseEntity.ok(ApiResponse.success("Out of stock products fetched", outOfStockProducts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch out of stock products", null));
        }
    }
    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VENDOR')")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByVendor(@PathVariable Long vendorId) {
        try {
            List<Product> products = productService.getProductsByVendor(vendorId);
            return ResponseEntity.ok(ApiResponse.success("Vendor products fetched", products));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch vendor products", null));
        }
    }

}