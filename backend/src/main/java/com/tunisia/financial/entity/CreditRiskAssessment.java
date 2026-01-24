package com.tunisia.financial.entity;

import com.tunisia.financial.enumerations.RiskCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a credit risk assessment in the system
 */
@Entity
@Table(name = "credit_risk_assessments", indexes = {
        @Index(name = "idx_risk_assessment_user", columnList = "sme_user_id"),
        @Index(name = "idx_risk_assessment_category", columnList = "risk_category"),
        @Index(name = "idx_risk_assessment_created", columnList = "assessed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditRiskAssessment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * SME user for whom this assessment was performed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sme_user_id", nullable = false)
    @NotNull(message = "SME user is required")
    private User smeUser;
    
    /**
     * Overall risk score (0-100)
     */
    @Column(name = "risk_score", nullable = false)
    @NotNull(message = "Risk score is required")
    @DecimalMin(value = "0.0", message = "Risk score must be between 0 and 100")
    private Double riskScore;
    
    /**
     * Risk category classification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false, length = 20)
    @NotNull(message = "Risk category is required")
    private RiskCategory riskCategory;
    
    /**
     * Summary of the assessment
     */
    @Column(name = "assessment_summary", columnDefinition = "TEXT")
    private String assessmentSummary;
    
    /**
     * Financial data used for assessment (stored as JSON)
     */
    @Column(name = "financial_data", columnDefinition = "TEXT")
    private String financialData;
    
    /**
     * Annual revenue at assessment time
     */
    @Column(name = "annual_revenue", precision = 15, scale = 2)
    private BigDecimal annualRevenue;
    
    /**
     * Total assets at assessment time
     */
    @Column(name = "total_assets", precision = 15, scale = 2)
    private BigDecimal totalAssets;
    
    /**
     * Total liabilities at assessment time
     */
    @Column(name = "total_liabilities", precision = 15, scale = 2)
    private BigDecimal totalLiabilities;
    
    /**
     * Debt ratio at assessment time
     */
    @Column(name = "debt_ratio", precision = 5, scale = 4)
    private BigDecimal debtRatio;
    
    /**
     * Industry sector
     */
    @Column(name = "industry_sector", length = 100)
    private String industrySector;
    
    /**
     * Years in business
     */
    @Column(name = "years_in_business")
    private Integer yearsInBusiness;
    
    /**
     * Credit history score (0-100)
     */
    @Column(name = "credit_history_score")
    private Double creditHistoryScore;
    
    /**
     * Model predictions as JSON
     */
    @Column(name = "model_predictions", columnDefinition = "TEXT")
    private String modelPredictions;
    
    /**
     * Ensemble method used
     */
    @Column(name = "ensemble_method", length = 50)
    private String ensembleMethod;
    
    /**
     * Market conditions at assessment time
     */
    @Column(name = "market_conditions", columnDefinition = "TEXT")
    private String marketConditions;
    
    /**
     * Timestamp when the assessment was performed
     */
    @Column(name = "assessed_at", nullable = false, updatable = false)
    private Instant assessedAt;
    
    /**
     * Whether this assessment has been reviewed
     */
    @Column(name = "reviewed")
    private Boolean reviewed = false;
    
    /**
     * Reviewer's notes if reviewed
     */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    /**
     * Timestamp when the assessment was last updated
     */
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        assessedAt = Instant.now();
        updatedAt = Instant.now();
        if (reviewed == null) {
            reviewed = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
