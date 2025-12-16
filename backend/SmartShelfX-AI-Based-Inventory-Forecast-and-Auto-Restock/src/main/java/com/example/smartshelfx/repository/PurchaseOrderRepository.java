package com.example.smartshelfx.repository;

import com.example.smartshelfx.model.PurchaseOrder;
import com.example.smartshelfx.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    List<PurchaseOrder> findByVendorIdOrderByCreatedAtDesc(Long vendorId);

    List<PurchaseOrder> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(PurchaseOrder.OrderStatus status);

    List<PurchaseOrder> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.createdAt BETWEEN :startDate AND :endDate ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // ✅ FIXED: expectedDelivery is LocalDate, so use LocalDate parameters
    @Query("""
        SELECT po 
        FROM PurchaseOrder po 
        WHERE po.expectedDelivery >= :startDate 
          AND po.expectedDelivery < :endDate
        ORDER BY po.expectedDelivery ASC
    """)
    List<PurchaseOrder> findUpcomingDeliveries(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.status = :status")
    Long countByStatus(@Param("status") PurchaseOrder.OrderStatus status);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.status IN ('PENDING', 'APPROVED') ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findActivePurchaseOrders();

    @Query("""
        SELECT po.product.id, SUM(po.quantity) 
        FROM PurchaseOrder po 
        WHERE po.status IN ('APPROVED', 'ORDERED') 
        GROUP BY po.product.id
    """)
    List<Object[]> findPendingQuantitiesByProduct();

    @Query(value = "SELECT * FROM purchase_orders WHERE status = 'PENDING' AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) ORDER BY created_at DESC",
            nativeQuery = true)
    List<PurchaseOrder> findRecentPendingOrders();
    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.status IN ('PENDING','APPROVED','ORDERED')")
    int countActivePOs();

}
