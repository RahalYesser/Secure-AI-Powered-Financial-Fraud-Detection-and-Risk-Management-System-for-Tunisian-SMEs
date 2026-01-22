package com.tunisia.financial.ai.fraud;

import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.entity.Transaction;

/**
 * Functional interface for fraud detection strategy
 * Allows different detection algorithms to be plugged in
 */
@FunctionalInterface
public interface FraudDetectionStrategy {
    
    /**
     * Detect fraud in a transaction
     * 
     * @param transaction the transaction to analyze
     * @return the fraud detection result with confidence score
     */
    FraudDetectionResult.ModelPrediction detect(Transaction transaction);
}
