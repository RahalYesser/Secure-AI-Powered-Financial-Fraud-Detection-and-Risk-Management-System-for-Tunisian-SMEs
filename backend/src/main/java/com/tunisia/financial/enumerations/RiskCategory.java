package com.tunisia.financial.enumerations;

/**
 * Risk categories for credit risk assessment
 * Defines different levels of credit risk for SME businesses
 * 
 * LOW: Minimal risk, strong financial indicators
 * MEDIUM: Moderate risk, acceptable with monitoring
 * HIGH: Significant risk, requires careful evaluation
 * CRITICAL: Very high risk, immediate action needed
 */
public enum RiskCategory {
    /**
     * Low risk category - strong financial position
     * Typically indicates healthy cash flow, low debt ratio, and stable revenue
     */
    LOW,
    
    /**
     * Medium risk category - acceptable risk level
     * May have some concerns but overall manageable risk profile
     */
    MEDIUM,
    
    /**
     * High risk category - significant concerns
     * Indicates potential issues with financial stability or debt management
     */
    HIGH,
    
    /**
     * Critical risk category - severe risk indicators
     * Requires immediate attention and potential intervention
     */
    CRITICAL;
    
    /**
     * Get risk category based on risk score (0-100)
     * 
     * @param riskScore the calculated risk score
     * @return the corresponding risk category
     */
    public static RiskCategory fromScore(double riskScore) {
        if (riskScore < 25) {
            return LOW;
        } else if (riskScore < 50) {
            return MEDIUM;
        } else if (riskScore < 75) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }
}
