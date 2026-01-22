package com.tunisia.financial.dto.response;

import java.util.List;

/**
 * Record representing the result of fraud detection analysis
 */
public record FraudDetectionResult(
        boolean isFraud,
        double confidence,
        String primaryReason,
        List<ModelPrediction> modelPredictions,
        double fraudScore
) {
    /**
     * Nested record for individual model predictions
     */
    public record ModelPrediction(
            String modelName,
            double confidence,
            boolean isFraud,
            String reason
    ) {
    }
}
