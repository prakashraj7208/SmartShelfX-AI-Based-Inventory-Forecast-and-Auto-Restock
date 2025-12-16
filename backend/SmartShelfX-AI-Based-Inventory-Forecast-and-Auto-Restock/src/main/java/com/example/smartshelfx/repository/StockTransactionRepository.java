package com.example.smartshelfx.repository;

import com.example.smartshelfx.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByProductIdOrderByTimestampDesc(Long productId);

    @Query("SELECT st FROM StockTransaction st WHERE st.product.id = :productId ORDER BY st.timestamp DESC")
    List<StockTransaction> findRecentTransactionsByProduct(@Param("productId") Long productId);

    List<StockTransaction> findByTypeOrderByTimestampDesc(StockTransaction.TransactionType type);

    // FINAL — start/end DateTime required
    @Query("""
        SELECT COUNT(st)
        FROM StockTransaction st
        WHERE st.type = 'IN'
          AND st.timestamp >= :start
          AND st.timestamp < :end
    """)
    Long countTodayStockIns(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(st)
        FROM StockTransaction st
        WHERE st.type = 'OUT'
          AND st.timestamp >= :start
          AND st.timestamp < :end
    """)
    Long countTodayStockOuts(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @Query("""
        SELECT st.product.id, SUM(st.quantity)
        FROM StockTransaction st
        WHERE st.type = 'OUT'
          AND st.timestamp BETWEEN :startDate AND :endDate
        GROUP BY st.product.id
    """)
    List<Object[]> findSalesBetweenDates(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT st FROM StockTransaction st ORDER BY st.timestamp DESC")
    List<StockTransaction> findTopNByOrderByTimestampDesc();

    @Query(value = "SELECT * FROM stock_transactions ORDER BY timestamp DESC LIMIT :limit",
            nativeQuery = true)
    List<StockTransaction> findRecentTransactions(@Param("limit") int limit);

    @Query("SELECT COUNT(t) FROM StockTransaction t")
    int countTransactions();

    @Query("SELECT COUNT(t) FROM StockTransaction t WHERE t.type = :type")
    int countType(@Param("type") StockTransaction.TransactionType type);

}