package com.tunisia.financial.exception;

/**
 * Exception thrown when fraud detection process fails
 */
public class FraudDetectionException extends RuntimeException {
    
    public FraudDetectionException(String message) {
        super(message);
    }
    
    public FraudDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public FraudDetectionException(Long transactionId, Throwable cause) {
        super("Fraud detection failed for transaction: " + transactionId, cause);
    }
}
