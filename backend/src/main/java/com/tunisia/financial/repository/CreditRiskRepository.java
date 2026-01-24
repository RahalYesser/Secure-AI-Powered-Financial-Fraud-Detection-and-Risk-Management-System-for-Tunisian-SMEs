package com.tunisia.financial.repository;

import com.tunisia.financial.entity.CreditRiskAssessment;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.RiskCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CreditRiskAssessment entity with custom query methods
 */
@Repository
public interface CreditRiskRepository extends JpaRepository<CreditRiskAssessment, Long> {
    
    /**
     * Find assessments by SME user
     */
    List<CreditRiskAssessment> findBySmeUser(User user);
    
    /**
     * Find assessments by SME user ordered by assessment date descending
     */
    List<CreditRiskAssessment> findBySmeUserOrderByAssessedAtDesc(User user);
    
    /**
     * Find assessments by SME user ID
     */
    List<CreditRiskAssessment> findBySmeUserId(UUID userId);
    
    /**
     * Find assessments by SME user ID with pagination
     */
    Page<CreditRiskAssessment> findBySmeUserId(UUID userId, Pageable pageable);
    
    /**
     * Find assessments by risk category
     */
    Page<CreditRiskAssessment> findByRiskCategory(RiskCategory category, Pageable pageable);
    
    /**
     * Find assessments by risk category list
     */
    Page<CreditRiskAssessment> findByRiskCategoryIn(List<RiskCategory> categories, Pageable pageable);
    
    /**
     * Find high-risk assessments (HIGH or CRITICAL)
     */
    @Query("SELECT a FROM CreditRiskAssessment a WHERE a.riskCategory IN ('HIGH', 'CRITICAL') ORDER BY a.assessedAt DESC")
    Page<CreditRiskAssessment> findHighRiskAssessments(Pageable pageable);
    
    /**
     * Find assessments above a risk score threshold
     */
    @Query("SELECT a FROM CreditRiskAssessment a WHERE a.riskScore >= :threshold ORDER BY a.riskScore DESC")
    List<CreditRiskAssessment> findByRiskScoreGreaterThanEqual(@Param("threshold") Double threshold);
    
    /**
     * Find assessments within date range
     */
    @Query("SELECT a FROM CreditRiskAssessment a WHERE a.assessedAt BETWEEN :startDate AND :endDate ORDER BY a.assessedAt DESC")
    List<CreditRiskAssessment> findByAssessedAtBetween(
            @Param("startDate") Instant startDate, 
            @Param("endDate") Instant endDate
    );
    
    /**
     * Find assessments by user and date range
     */
    @Query("SELECT a FROM CreditRiskAssessment a WHERE a.smeUser.id = :userId AND a.assessedAt BETWEEN :startDate AND :endDate ORDER BY a.assessedAt DESC")
    List<CreditRiskAssessment> findByUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
    
    /**
     * Find most recent assessment for a user
     */
    Optional<CreditRiskAssessment> findFirstBySmeUserOrderByAssessedAtDesc(User user);
    
    /**
     * Find most recent assessment for a user by ID
     */
    Optional<CreditRiskAssessment> findFirstBySmeUserIdOrderByAssessedAtDesc(UUID userId);
    
    /**
     * Find unreviewed assessments
     */
    Page<CreditRiskAssessment> findByReviewedFalse(Pageable pageable);
    
    /**
     * Find unreviewed high-risk assessments
     */
    @Query("SELECT a FROM CreditRiskAssessment a WHERE a.reviewed = false AND a.riskCategory IN ('HIGH', 'CRITICAL') ORDER BY a.riskScore DESC")
    Page<CreditRiskAssessment> findUnreviewedHighRiskAssessments(Pageable pageable);
    
    /**
     * Count assessments by risk category
     */
    @Query("SELECT a.riskCategory, COUNT(a) FROM CreditRiskAssessment a GROUP BY a.riskCategory")
    List<Object[]> countByRiskCategory();
    
    /**
     * Count assessments for a user
     */
    long countBySmeUserId(UUID userId);
    
    /**
     * Find assessments by industry sector
     */
    Page<CreditRiskAssessment> findByIndustrySector(String sector, Pageable pageable);
    
    /**
     * Calculate average risk score for a user
     */
    @Query("SELECT AVG(a.riskScore) FROM CreditRiskAssessment a WHERE a.smeUser.id = :userId")
    Optional<Double> calculateAverageRiskScoreByUserId(@Param("userId") UUID userId);
    
    /**
     * Calculate average risk score by industry sector
     */
    @Query("SELECT AVG(a.riskScore) FROM CreditRiskAssessment a WHERE a.industrySector = :sector")
    Optional<Double> calculateAverageRiskScoreBySector(@Param("sector") String sector);
    
    /**
     * Find assessments assessed after a certain date
     */
    List<CreditRiskAssessment> findByAssessedAtAfter(Instant date);
    
    /**
     * Count assessments in date range
     */
    @Query("SELECT COUNT(a) FROM CreditRiskAssessment a WHERE a.assessedAt BETWEEN :startDate AND :endDate")
    long countByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
