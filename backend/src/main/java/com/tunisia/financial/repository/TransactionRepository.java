package com.tunisia.financial.repository;

import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Transaction entity with custom query methods
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Find all transactions for a specific user
     */
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Find transactions by status
     */
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    
    /**
     * Find transactions by type
     */
    Page<Transaction> findByType(TransactionType type, Pageable pageable);
    
    /**
     * Find transactions by user and status
     */
    Page<Transaction> findByUserIdAndStatus(UUID userId, TransactionStatus status, Pageable pageable);
    
    /**
     * Find transactions by user and type
     */
    Page<Transaction> findByUserIdAndType(UUID userId, TransactionType type, Pageable pageable);
    
    /**
     * Find transaction by reference number
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    
    /**
     * Find transactions within a date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByDateRange(@Param("startDate") Instant startDate, 
                                      @Param("endDate") Instant endDate, 
                                      Pageable pageable);
    
    /**
     * Find transactions for a user within a date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                                @Param("startDate") Instant startDate,
                                                @Param("endDate") Instant endDate,
                                                Pageable pageable);
    
    /**
     * Find transactions with fraud score above threshold
     */
    @Query("SELECT t FROM Transaction t WHERE t.fraudScore >= :threshold")
    Page<Transaction> findByFraudScoreGreaterThanEqual(@Param("threshold") Double threshold, Pageable pageable);
    
    /**
     * Find transactions by user with amount greater than specified value
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.amount >= :minAmount")
    Page<Transaction> findByUserIdAndAmountGreaterThanEqual(@Param("userId") UUID userId,
                                                             @Param("minAmount") BigDecimal minAmount,
                                                             Pageable pageable);
    
    /**
     * Count transactions by status
     */
    long countByStatus(TransactionStatus status);
    
    /**
     * Count transactions by type
     */
    long countByType(TransactionType type);
    
    /**
     * Count transactions for a specific user
     */
    long countByUserId(UUID userId);
    
    /**
     * Get total transaction amount by user
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user.id = :userId AND t.status = :status")
    BigDecimal sumAmountByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") TransactionStatus status);
    
    /**
     * Get average transaction amount
     */
    @Query("SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t WHERE t.status = 'COMPLETED'")
    BigDecimal getAverageCompletedAmount();
    
    /**
     * Get total amount by status
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") TransactionStatus status);
}
