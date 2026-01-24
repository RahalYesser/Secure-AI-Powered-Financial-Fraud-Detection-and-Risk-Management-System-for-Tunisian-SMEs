package com.tunisia.financial.exception;

/**
 * Exception thrown when a credit risk assessment operation fails
 */
public class CreditRiskAssessmentException extends RuntimeException {
    
    public CreditRiskAssessmentException(String message) {
        super(message);
    }
    
    public CreditRiskAssessmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
