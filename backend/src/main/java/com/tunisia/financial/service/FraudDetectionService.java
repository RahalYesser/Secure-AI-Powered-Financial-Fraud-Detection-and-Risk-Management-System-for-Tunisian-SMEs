package com.tunisia.financial.service;

import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.dto.response.FraudPatternResponse;
import com.tunisia.financial.entity.FraudPattern;
import com.tunisia.financial.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for AI-powered fraud detection
 */
public interface FraudDetectionService {
    
    /**
     * Detect fraud in a transaction using ensemble of AI models
     * 
     * @param transaction the transaction to analyze
     * @return fraud detection result with confidence scores from multiple models
     */
    FraudDetectionResult detectFraud(Transaction transaction);
    
    /**
     * Get all detected fraud patterns
     * 
     * @return list of all fraud patterns
     */
    List<FraudPattern> getFraudPatterns();
    
    /**
     * Get fraud patterns with pagination
     * 
     * @param pageable pagination parameters
     * @return page of fraud pattern responses
     */
    Page<FraudPatternResponse> getFraudPatterns(Pageable pageable);
    
    /**
     * Get fraud patterns for a specific transaction
     * 
     * @param transactionId the transaction ID
     * @return list of fraud patterns detected for the transaction
     */
    List<FraudPatternResponse> getFraudPatternsByTransactionId(Long transactionId);
    
    /**
     * Get unreviewed fraud patterns
     * 
     * @param pageable pagination parameters
     * @return page of unreviewed fraud patterns
     */
    Page<FraudPatternResponse> getUnreviewedFraudPatterns(Pageable pageable);
    
    /**
     * Get high confidence fraud patterns
     * 
     * @param threshold minimum confidence threshold
     * @return list of high confidence fraud patterns
     */
    List<FraudPatternResponse> getHighConfidenceFraudPatterns(Double threshold);
    
    /**
     * Update/reload a specific fraud detection model
     * 
     * @param modelType the type of model to update (DJL, ONNX, TensorFlow)
     */
    void updateModel(String modelType);
    
    /**
     * Mark a fraud pattern as reviewed
     * 
     * @param patternId the fraud pattern ID
     * @param reviewNotes optional review notes
     */
    void markPatternAsReviewed(Long patternId, String reviewNotes);
    
    /**
     * Get fraud patterns detected within a date range
     * 
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @return list of fraud patterns detected in the range
     */
    List<FraudPatternResponse> getFraudPatternsByDateRange(Instant startDate, Instant endDate);
}
