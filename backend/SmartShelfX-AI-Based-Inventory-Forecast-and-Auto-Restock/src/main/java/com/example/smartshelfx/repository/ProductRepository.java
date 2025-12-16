package com.example.smartshelfx.repository;

import com.example.smartshelfx.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    Page<Product> findByCategory(String category, Pageable pageable);
    Page<Product> findByVendorId(Long vendorId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.currentStock <= p.reorderLevel AND p.active = true")
    List<Product> findLowStockProducts();


    @Query("SELECT p FROM Product p WHERE p.currentStock <= p.safetyStock AND p.active = true")
    List<Product> findCriticalStockProducts();

    @Query("SELECT p FROM Product p WHERE p.currentStock = 0 AND p.active = true")
    List<Product> findOutOfStockProducts();

    @Query("SELECT p.category, COUNT(p) FROM Product p GROUP BY p.category")
    List<Object[]> findProductCountByCategory();

    @Query("SELECT SUM(p.currentStock * p.price) FROM Product p")
    Double calculateTotalInventoryValue();

    @Query("SELECT p FROM Product p WHERE p.vendor.id = :vendorId AND p.active = true")
    List<Product> findByVendorId(@Param("vendorId") Long vendorId);

}