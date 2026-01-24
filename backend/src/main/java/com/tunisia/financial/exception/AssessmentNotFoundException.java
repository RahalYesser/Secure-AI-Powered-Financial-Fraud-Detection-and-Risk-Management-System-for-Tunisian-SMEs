package com.tunisia.financial.exception;

/**
 * Exception thrown when a credit risk assessment is not found
 */
public class AssessmentNotFoundException extends RuntimeException {
    
    public AssessmentNotFoundException(String message) {
        super(message);
    }
    
    public AssessmentNotFoundException(Long assessmentId) {
        super("Credit risk assessment not found with ID: " + assessmentId);
    }
}
