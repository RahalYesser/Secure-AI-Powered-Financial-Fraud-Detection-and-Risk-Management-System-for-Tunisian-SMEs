package com.tunisia.financial.dto.response;

import com.tunisia.financial.enumerations.RiskCategory;

import java.util.List;
import java.util.UUID;

/**
 * Record representing a credit risk assessment result
 */
public record RiskAssessment(
        Long assessmentId,
        UUID smeUserId,
        double riskScore,
        RiskCategory riskCategory,
        String assessmentSummary,
        List<ModelRiskPrediction> modelPredictions,
        java.time.Instant assessedAt
) {
    /**
     * Nested record for individual model risk predictions
     */
    public record ModelRiskPrediction(
            String modelName,
            double riskScore,
            RiskCategory predictedCategory,
            String rationale
    ) {
    }
    
    /**
     * Check if the risk level is acceptable (LOW or MEDIUM)
     */
    public boolean isAcceptableRisk() {
        return riskCategory == RiskCategory.LOW || riskCategory == RiskCategory.MEDIUM;
    }
    
    /**
     * Check if immediate action is required (CRITICAL)
     */
    public boolean requiresImmediateAction() {
        return riskCategory == RiskCategory.CRITICAL;
    }
}
