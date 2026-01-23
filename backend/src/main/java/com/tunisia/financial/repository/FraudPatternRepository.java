package com.tunisia.financial.repository;

import com.tunisia.financial.entity.FraudPattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for FraudPattern entity with custom query methods
 */
@Repository
public interface FraudPatternRepository extends JpaRepository<FraudPattern, Long> {
    
    /**
     * Find fraud patterns by transaction ID
     */
    List<FraudPattern> findByTransactionId(Long transactionId);
    
    /**
     * Find fraud patterns by pattern type
     */
    Page<FraudPattern> findByPatternType(String patternType, Pageable pageable);
    
    /**
     * Find fraud patterns by detector model
     */
    List<FraudPattern> findByDetectorModel(String detectorModel);
    
    /**
     * Find fraud patterns with confidence above threshold
     */
    @Query("SELECT fp FROM FraudPattern fp WHERE fp.confidence >= :threshold")
    List<FraudPattern> findByConfidenceGreaterThanEqual(@Param("threshold") Double threshold);
    
    /**
     * Find unreviewed fraud patterns
     */
    Page<FraudPattern> findByReviewedFalse(Pageable pageable);
    
    /**
     * Find fraud patterns detected within a time range
     */
    @Query("SELECT fp FROM FraudPattern fp WHERE fp.detectedAt BETWEEN :startDate AND :endDate")
    List<FraudPattern> findByDetectedAtBetween(
            @Param("startDate") Instant startDate, 
            @Param("endDate") Instant endDate
    );
    
    /**
     * Count fraud patterns by pattern type
     */
    @Query("SELECT fp.patternType, COUNT(fp) FROM FraudPattern fp GROUP BY fp.patternType")
    List<Object[]> countByPatternType();
    
    /**
     * Find high-confidence unreviewed patterns
     */
    @Query("SELECT fp FROM FraudPattern fp WHERE fp.reviewed = false AND fp.confidence >= :threshold ORDER BY fp.confidence DESC")
    List<FraudPattern> findHighConfidenceUnreviewed(@Param("threshold") Double threshold);
}
