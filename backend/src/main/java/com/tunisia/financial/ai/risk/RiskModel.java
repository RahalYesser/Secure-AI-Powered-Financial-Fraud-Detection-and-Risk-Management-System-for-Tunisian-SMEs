package com.tunisia.financial.ai.risk;

import com.tunisia.financial.dto.FinancialData;
import com.tunisia.financial.dto.response.RiskAssessment;

/**
 * Functional interface for credit risk model strategy
 * Allows different risk assessment algorithms and ML models to be plugged in
 */
@FunctionalInterface
public interface RiskModel {
    
    /**
     * Assess credit risk based on financial data
     * 
     * @param financialData the financial data to analyze
     * @return the risk assessment prediction with score and category
     */
    RiskAssessment.ModelRiskPrediction assess(FinancialData financialData);
}
