package com.example.smartshelfx.service;

import com.example.smartshelfx.dto.PagedResponse;
import com.example.smartshelfx.model.Product;
import com.example.smartshelfx.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public PagedResponse<Product> getAllProducts(int size, int page, String sortBy, String sortDir, String category) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage;

        if (category != null && !category.isEmpty()) {
            productPage = productRepository.findByCategory(category, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        return new PagedResponse<>(
                productPage.getContent(),
                productPage.getNumber(),
                productPage.getTotalPages(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.isLast()
        );
    }
    // Add this method to ProductService - place it after the existing getAllProducts method
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional
    public Product createProduct(Product product, MultipartFile image) {
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new RuntimeException("SKU already exists: " + product.getSku());
        }

        try {
            if (image != null && !image.isEmpty()) {
                product.setImageName(image.getOriginalFilename());
                product.setImageType(image.getContentType());
                product.setImageData(image.getBytes());
                log.info("Image uploaded: {} ({} bytes)", image.getOriginalFilename(), image.getSize());
            }

            return productRepository.save(product);
        } catch (IOException e) {
            throw new RuntimeException("Error while saving product image", e);
        }
    }

    public Product updateProduct(Long id, Product product, MultipartFile image) {
        Product existing = getProductById(id);

        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setCategory(product.getCategory());
        existing.setPrice(product.getPrice());
        existing.setCostPrice(product.getCostPrice());
        existing.setReorderLevel(product.getReorderLevel());
        existing.setSafetyStock(product.getSafetyStock());
        existing.setLeadTimeDays(product.getLeadTimeDays());
        existing.setVendor(product.getVendor());
        existing.setActive(product.getActive());

        try {
            if (image != null && !image.isEmpty()) {
                existing.setImageData(image.getBytes());
                existing.setImageName(image.getOriginalFilename());
                existing.setImageType(image.getContentType());
            }
            return productRepository.save(existing);
        } catch (IOException e) {
            throw new RuntimeException("Error while updating product image", e);
        }
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    public PagedResponse<Product> searchProducts(String query, int size, int page, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage;

        if (query == null || query.isEmpty()) {
            productPage = productRepository.findAll(pageable);
        } else {
            productPage = productRepository.searchByKeyword(query, pageable);
        }

        return new PagedResponse<>(
                productPage.getContent(),
                productPage.getNumber(),
                productPage.getTotalPages(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.isLast()
        );
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    public List<Product> getCriticalStockProducts() {
        return productRepository.findCriticalStockProducts();
    }

    public List<Product> getOutOfStockProducts() {
        return productRepository.findOutOfStockProducts();
    }

    public List<Object[]> getProductCountByCategory() {
        return productRepository.findProductCountByCategory();
    }

    public Double getTotalInventoryValue() {
        return productRepository.calculateTotalInventoryValue();
    }

    public List<Product> getProductsByVendor(Long vendorId) {
        return productRepository.findByVendorId(vendorId);
    }

}