package com.tunisia.financial.service;

import com.tunisia.financial.dto.FinancialData;
import com.tunisia.financial.dto.response.RiskAssessment;
import com.tunisia.financial.dto.response.RiskReport;
import com.tunisia.financial.entity.CreditRiskAssessment;
import com.tunisia.financial.enumerations.RiskCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for AI-powered credit risk assessment
 */
public interface CreditRiskService {
    
    /**
     * Perform credit risk assessment using ensemble of AI models
     * 
     * @param financialData the financial data to analyze
     * @return risk assessment result with scores from multiple models
     */
    RiskAssessment assessCreditRisk(FinancialData financialData);
    
    /**
     * Generate comprehensive risk report with recommendations
     * 
     * @param assessmentId the assessment ID
     * @return detailed risk report
     */
    RiskReport generateRiskReport(Long assessmentId);
    
    /**
     * Get assessment by ID
     * 
     * @param assessmentId the assessment ID
     * @return the credit risk assessment
     */
    CreditRiskAssessment getAssessmentById(Long assessmentId);
    
    /**
     * Get all assessments for a specific user
     * 
     * @param userId the SME user ID
     * @return list of assessments
     */
    List<CreditRiskAssessment> getAssessmentsByUserId(UUID userId);
    
    /**
     * Get assessments with pagination
     * 
     * @param pageable pagination parameters
     * @return page of assessments
     */
    Page<CreditRiskAssessment> getAllAssessments(Pageable pageable);
    
    /**
     * Get assessments by risk category
     * 
     * @param category the risk category
     * @param pageable pagination parameters
     * @return page of assessments in specified category
     */
    Page<CreditRiskAssessment> getAssessmentsByCategory(RiskCategory category, Pageable pageable);
    
    /**
     * Get high-risk assessments (HIGH or CRITICAL)
     * 
     * @param pageable pagination parameters
     * @return page of high-risk assessments
     */
    Page<CreditRiskAssessment> getHighRiskAssessments(Pageable pageable);
    
    /**
     * Get assessments above a risk score threshold
     * 
     * @param threshold minimum risk score (0-100)
     * @return list of assessments above threshold
     */
    List<CreditRiskAssessment> getAssessmentsAboveThreshold(Double threshold);
    
    /**
     * Get most recent assessment for a user
     * 
     * @param userId the SME user ID
     * @return the most recent assessment or null if none exists
     */
    CreditRiskAssessment getMostRecentAssessment(UUID userId);
    
    /**
     * Get assessments within date range
     * 
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of assessments in date range
     */
    List<CreditRiskAssessment> getAssessmentsByDateRange(Instant startDate, Instant endDate);
    
    /**
     * Get unreviewed assessments
     * 
     * @param pageable pagination parameters
     * @return page of unreviewed assessments
     */
    Page<CreditRiskAssessment> getUnreviewedAssessments(Pageable pageable);
    
    /**
     * Get unreviewed high-risk assessments
     * 
     * @param pageable pagination parameters
     * @return page of unreviewed high-risk assessments
     */
    Page<CreditRiskAssessment> getUnreviewedHighRiskAssessments(Pageable pageable);
    
    /**
     * Mark an assessment as reviewed
     * 
     * @param assessmentId the assessment ID
     * @param reviewNotes optional review notes
     */
    void markAssessmentAsReviewed(Long assessmentId, String reviewNotes);
    
    /**
     * Update/reload a specific risk model
     * 
     * @param modelType the type of model to update (DJL, ONNX, TensorFlow)
     */
    void updateModel(String modelType);
    
    /**
     * Get risk statistics by category
     * 
     * @return map of category to count
     */
    java.util.Map<RiskCategory, Long> getRiskStatistics();
    
    /**
     * Calculate average risk score for a user
     * 
     * @param userId the user ID
     * @return average risk score
     */
    Double calculateAverageRiskScore(UUID userId);
    
    /**
     * Get assessments by industry sector
     * 
     * @param sector the industry sector
     * @param pageable pagination parameters
     * @return page of assessments in specified sector
     */
    Page<CreditRiskAssessment> getAssessmentsBySector(String sector, Pageable pageable);
    
    /**
     * Calculate average risk score by industry sector
     * 
     * @param sector the industry sector
     * @return average risk score for the sector
     */
    Double calculateAverageRiskScoreBySector(String sector);
}
