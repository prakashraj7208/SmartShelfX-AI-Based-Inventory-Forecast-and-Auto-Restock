package com.example.smartshelfx.repository;

import com.example.smartshelfx.model.Alert;
import com.example.smartshelfx.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // ------------------------------------------------------------
    // BASIC OPERATIONS
    // ------------------------------------------------------------
    boolean existsByProductAndTypeAndIsReadFalse(Product product, Alert.AlertType type);

    List<Alert> findByIsReadFalseOrderByCreatedAtDesc();

    List<Alert> findByProductIdOrderByCreatedAtDesc(Long productId);

    long countByIsReadFalse();


    // ------------------------------------------------------------
    // DATE-BASED QUERIES — FIXED to use LocalDateTime
    // ------------------------------------------------------------
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT * FROM alerts WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) ORDER BY created_at DESC", nativeQuery = true)
    List<Alert> findRecentAlertsNative();


    // ------------------------------------------------------------
    // STATUS FILTERING
    // ------------------------------------------------------------
    @Query("SELECT a FROM Alert a WHERE a.isRead = :isRead ORDER BY a.createdAt DESC")
    List<Alert> findByReadStatus(@Param("isRead") Boolean isRead);

    List<Alert> findByTypeAndIsReadFalseOrderByCreatedAtDesc(Alert.AlertType type);

    long countByTypeAndIsReadFalse(Alert.AlertType type);


    // ------------------------------------------------------------
    // BULK OPERATIONS
    // ------------------------------------------------------------
    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.id IN :alertIds")
    void markAlertsAsRead(@Param("alertIds") List<Long> alertIds);


    // ------------------------------------------------------------
    // PRIORITY FILTERS
    // ------------------------------------------------------------
    List<Alert> findByPriorityAndIsReadFalseOrderByCreatedAtDesc(Alert.Priority priority);

    long countByPriorityAndIsReadFalse(Alert.Priority priority);


    // ------------------------------------------------------------
    // ADVANCED FINDERS
    // ------------------------------------------------------------
    List<Alert> findByProductAndTypeOrderByCreatedAtDesc(Product product, Alert.AlertType type);

    @Query("SELECT a FROM Alert a WHERE a.priority IN ('HIGH','CRITICAL','URGENT') AND a.isRead = false ORDER BY a.createdAt DESC")
    List<Alert> findCriticalAlerts();


    // ------------------------------------------------------------
    // LIMIT QUERIES
    // ------------------------------------------------------------
    @Query(value = "SELECT * FROM alerts WHERE is_read = false ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Alert> findRecentUnreadAlerts(@Param("limit") int limit);


    // ------------------------------------------------------------
    // ANALYTICS — FIXED to use LocalDateTime
    // ------------------------------------------------------------
    @Query("SELECT a.type, COUNT(a) FROM Alert a WHERE a.isRead = false GROUP BY a.type")
    List<Object[]> countUnreadAlertsByType();

    @Query("SELECT a.priority, COUNT(a) FROM Alert a WHERE a.isRead = false GROUP BY a.priority")
    List<Object[]> countUnreadAlertsByPriority();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.isRead = false AND a.createdAt >= :startDate")
    Long countRecentUnreadAlerts(@Param("startDate") LocalDateTime startDate);

    @Query("""
           SELECT a 
           FROM Alert a 
           WHERE a.createdAt BETWEEN :start AND :end 
           ORDER BY a.createdAt DESC
           """)
    List<Alert> findAlertsByDateRange(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);


    // ------------------------------------------------------------
    // SYSTEM METRICS
    // ------------------------------------------------------------
    @Query("SELECT COUNT(a), SUM(CASE WHEN a.isRead = true THEN 1 ELSE 0 END) FROM Alert a")
    Object[] getAlertStatistics();


    // ------------------------------------------------------------
    // TYPE + PRIORITY ANALYTICS
    // ------------------------------------------------------------
    @Query("""
           SELECT a.type, a.priority, COUNT(a)
           FROM Alert a
           WHERE a.isRead = false
           GROUP BY a.type, a.priority
           """)
    List<Object[]> countUnreadAlertsByTypeAndPriority();


    // ------------------------------------------------------------
    // DAILY ALERT TREND — FIXED LocalDateTime → SQL DATE()
    // ------------------------------------------------------------
    @Query("""
           SELECT DATE(a.createdAt), COUNT(a)
           FROM Alert a
           WHERE a.createdAt >= :startDate
           GROUP BY DATE(a.createdAt)
           ORDER BY DATE(a.createdAt)
           """)
    List<Object[]> countAlertsByDate(@Param("startDate") LocalDateTime startDate);
}
